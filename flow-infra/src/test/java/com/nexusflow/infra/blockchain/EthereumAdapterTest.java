package com.nexusflow.infra.blockchain;

import com.nexusflow.domain.blockchain.ScannedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EthereumAdapterTest {

    private Web3j web3j;
    private EthereumAdapter adapter;

    @BeforeEach
    void setUp() {
        web3j = mock(Web3j.class);
        adapter = new EthereumAdapter(web3j, "0x000000000000000000000000000000000000dEaD");
    }

    @Test
    void scansTransferLogsIntoTransactions() throws Exception {
        Request blockNumberRequest = request(blockNumber(101L));
        Request logsRequest = request(ethLog());
        Request blockRequest = request(block("0xblock64", 1_700_000_000L));
        when(web3j.ethBlockNumber()).thenReturn(blockNumberRequest);
        when(web3j.ethGetLogs(any(EthFilter.class))).thenReturn(logsRequest);
        when(web3j.ethGetBlockByNumber(any(DefaultBlockParameter.class), eq(false)))
                .thenReturn(blockRequest);

        List<ScannedTransaction> transactions = adapter.scanNewBlocks(99L);

        assertEquals(1, transactions.size());
        ScannedTransaction tx = transactions.get(0);
        assertEquals("0xtxhash", tx.getTxHash());
        assertEquals("0x1111111111111111111111111111111111111111", tx.getFromAddress());
        assertEquals("0x2222222222222222222222222222222222222222", tx.getToAddress());
        assertEquals("1000", tx.getAmount());
        assertEquals("0x000000000000000000000000000000000000dead", tx.getContractAddress());
        assertEquals(100L, tx.getBlockNumber());
        assertEquals(2, tx.getConfirmations());
        assertEquals(1_700_000_000_000L, tx.getTimestamp());
    }

    @Test
    void computesConfirmationsFromReceiptBlock() throws Exception {
        EthGetTransactionReceipt receiptResponse = mock(EthGetTransactionReceipt.class);
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getBlockNumber()).thenReturn(BigInteger.valueOf(100L));
        when(receiptResponse.getTransactionReceipt()).thenReturn(Optional.of(receipt));
        Request receiptRequest = request(receiptResponse);
        Request blockNumberRequest = request(blockNumber(105L));
        when(web3j.ethGetTransactionReceipt("0xtxhash")).thenReturn(receiptRequest);
        when(web3j.ethBlockNumber()).thenReturn(blockNumberRequest);

        assertEquals(6, adapter.getConfirmations("0xtxhash"));
    }

    @Test
    void readsBlockHash() throws Exception {
        Request blockRequest = request(block("0xhash", 1_700_000_000L));
        when(web3j.ethGetBlockByNumber(any(DefaultBlockParameter.class), eq(false)))
                .thenReturn(blockRequest);

        assertEquals("0xhash", adapter.getBlockHash(100L));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Request request(Response<?> response) throws Exception {
        Request request = mock(Request.class);
        when(request.send()).thenReturn(response);
        return request;
    }

    private EthBlockNumber blockNumber(long number) {
        EthBlockNumber response = mock(EthBlockNumber.class);
        when(response.getBlockNumber()).thenReturn(BigInteger.valueOf(number));
        return response;
    }

    private EthLog ethLog() {
        EthLog response = mock(EthLog.class);
        EthLog.LogResult<?> result = mock(EthLog.LogResult.class);
        EthLog.LogObject logObject = mock(EthLog.LogObject.class);
        when(result.get()).thenReturn(logObject);
        when(logObject.getTopics()).thenReturn(List.of(
                "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                addressTopic("0x1111111111111111111111111111111111111111"),
                addressTopic("0x2222222222222222222222222222222222222222")
        ));
        when(logObject.getBlockNumber()).thenReturn(BigInteger.valueOf(100L));
        when(logObject.getData()).thenReturn(Numeric.toHexStringWithPrefixZeroPadded(BigInteger.valueOf(1000L), 64));
        when(logObject.getTransactionHash()).thenReturn("0xtxhash");
        when(logObject.getAddress()).thenReturn("0x000000000000000000000000000000000000dEaD");
        when(response.getLogs()).thenReturn(List.of(result));
        return response;
    }

    private String addressTopic(String address) {
        return Numeric.toHexStringWithPrefixZeroPadded(Numeric.toBigInt(address), 64);
    }

    private EthBlock block(String hash, long timestampSeconds) {
        EthBlock response = mock(EthBlock.class);
        EthBlock.Block block = mock(EthBlock.Block.class);
        when(block.getHash()).thenReturn(hash);
        when(block.getTimestamp()).thenReturn(BigInteger.valueOf(timestampSeconds));
        when(response.getBlock()).thenReturn(block);
        return response;
    }
}
