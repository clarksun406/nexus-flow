package com.nexusflow.infra.blockchain;

import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Ethereum blockchain adapter (ETH / ERC20).
 */
@Slf4j
public class EthereumAdapter implements BlockchainAdapter {

    private static final Event ERC20_TRANSFER = new Event("Transfer", Arrays.asList(
            TypeReference.create(Address.class, true),
            TypeReference.create(Address.class, true),
            TypeReference.create(Uint256.class)
    ));

    private final Web3j web3j;
    private final String usdtContractAddress;

    public EthereumAdapter(String rpcUrl, String usdtContractAddress) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.usdtContractAddress = normalizeAddress(usdtContractAddress);
    }

    EthereumAdapter(Web3j web3j, String usdtContractAddress) {
        this.web3j = web3j;
        this.usdtContractAddress = normalizeAddress(usdtContractAddress);
    }

    @Override
    public List<ScannedTransaction> scanNewBlocks(long lastScannedBlock) {
        long current = getCurrentBlockHeight();
        long from = Math.max(0, lastScannedBlock + 1);
        if (current <= 0 || from > current) {
            return List.of();
        }

        try {
            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(from)),
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(current)),
                    usdtContractAddress);
            filter.addSingleTopic(EventEncoder.encode(ERC20_TRANSFER));

            List<ScannedTransaction> transactions = new ArrayList<>();
            for (EthLog.LogResult<?> result : web3j.ethGetLogs(filter).send().getLogs()) {
                Object raw = result.get();
                if (raw instanceof EthLog.LogObject logObject) {
                    parseTransferLog(logObject, current).ifPresent(transactions::add);
                }
            }
            return transactions;
        } catch (Exception e) {
            log.error("Failed to scan ETH ERC20 logs from {} to {}: {}", from, current, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public long getCurrentBlockHeight() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber().longValue();
        } catch (Exception e) {
            log.error("Failed to get ETH block height", e);
            return 0;
        }
    }

    @Override
    public int getConfirmations(String txHash) {
        try {
            EthGetTransactionReceipt response = web3j.ethGetTransactionReceipt(txHash).send();
            Optional<TransactionReceipt> receipt = response.getTransactionReceipt();
            if (receipt.isEmpty() || receipt.get().getBlockNumber() == null) {
                return 0;
            }
            long current = getCurrentBlockHeight();
            long txBlock = receipt.get().getBlockNumber().longValue();
            return current >= txBlock ? Math.toIntExact(current - txBlock + 1) : 0;
        } catch (Exception e) {
            log.error("Failed to get ETH confirmations for {}: {}", txHash, e.getMessage());
            return 0;
        }
    }

    @Override
    public String getBlockHash(long blockNumber) {
        try {
            EthBlock block = web3j.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)), false).send();
            return block.getBlock() != null ? block.getBlock().getHash() : null;
        } catch (Exception e) {
            log.error("Failed to get ETH block hash for {}: {}", blockNumber, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            web3j.ethBlockNumber().send();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Chain supportedChain() {
        return Chain.ETH;
    }

    private Optional<ScannedTransaction> parseTransferLog(EthLog.LogObject logObject, long currentHeight) {
        if (logObject.getTopics() == null || logObject.getTopics().size() < 3) {
            return Optional.empty();
        }
        BigInteger blockNumber = logObject.getBlockNumber();
        if (blockNumber == null) {
            return Optional.empty();
        }
        String data = logObject.getData();
        if (data == null || data.length() < 3) {
            return Optional.empty();
        }
        BigInteger amount = Numeric.toBigInt(data);
        long height = blockNumber.longValue();
        return Optional.of(ScannedTransaction.builder()
                .txHash(logObject.getTransactionHash())
                .fromAddress(topicToAddress(logObject.getTopics().get(1)))
                .toAddress(topicToAddress(logObject.getTopics().get(2)))
                .amount(amount.toString())
                .contractAddress(normalizeAddress(logObject.getAddress()))
                .blockNumber(height)
                .confirmations(currentHeight >= height ? Math.toIntExact(currentHeight - height + 1) : 0)
                .timestamp(fetchBlockTimestampMillis(blockNumber))
                .build());
    }

    private long fetchBlockTimestampMillis(BigInteger blockNumber) {
        try {
            EthBlock block = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), false).send();
            if (block.getBlock() == null || block.getBlock().getTimestamp() == null) {
                return 0L;
            }
            return block.getBlock().getTimestamp().longValue() * 1000L;
        } catch (Exception e) {
            log.debug("Failed to read ETH block timestamp for {}: {}", blockNumber, e.getMessage());
            return 0L;
        }
    }

    private static String topicToAddress(String topic) {
        String clean = Numeric.cleanHexPrefix(topic);
        if (clean.length() < 40) {
            return "0x" + clean;
        }
        return "0x" + clean.substring(clean.length() - 40).toLowerCase(Locale.ROOT);
    }

    private static String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            return address;
        }
        return Numeric.prependHexPrefix(Numeric.cleanHexPrefix(address).toLowerCase(Locale.ROOT));
    }
}
