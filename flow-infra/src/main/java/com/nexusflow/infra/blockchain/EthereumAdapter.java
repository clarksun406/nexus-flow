package com.nexusflow.infra.blockchain;

import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.Collections;
import java.util.List;

/**
 * Ethereum blockchain adapter (ETH / ERC20).
 */
@Slf4j
public class EthereumAdapter implements BlockchainAdapter {

    private final Web3j web3j;
    private final String usdtContractAddress;

    public EthereumAdapter(String rpcUrl, String usdtContractAddress) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.usdtContractAddress = usdtContractAddress;
    }

    @Override
    public List<ScannedTransaction> scanNewBlocks(long lastScannedBlock) {
        // TODO Phase 2: Implement block scanning with ERC20 Transfer event filtering
        log.debug("Scanning ETH blocks from {}", lastScannedBlock);
        return Collections.emptyList();
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
        // TODO: currentBlock - txBlock
        return 0;
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
}