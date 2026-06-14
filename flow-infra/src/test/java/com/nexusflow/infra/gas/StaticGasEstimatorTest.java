package com.nexusflow.infra.gas;

import com.nexusflow.domain.gas.GasEstimateRequest;
import com.nexusflow.domain.gas.GasOperation;
import com.nexusflow.domain.shared.Chain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticGasEstimatorTest {

    private StaticGasEstimator estimator;

    @BeforeEach
    void setUp() {
        estimator = new StaticGasEstimator(
                new BigDecimal("30"),
                65_000L,
                new BigDecimal("15"),
                180L,
                new BigDecimal("20"));
    }

    @Test
    void estimatesErc20TransferGasInEth() {
        var estimate = estimator.estimate(GasEstimateRequest.builder()
                .chain(Chain.ETH)
                .network("ERC20")
                .operation(GasOperation.REFUND)
                .token("USDT")
                .amount(new BigDecimal("100"))
                .toAddress("0xabc")
                .build());

        assertEquals(Chain.ETH, estimate.getChain());
        assertEquals("ETH", estimate.getNativeCurrency());
        assertEquals(65_000L, estimate.getGasLimit());
        assertEquals(0, new BigDecimal("30").compareTo(estimate.getGasPrice()));
        assertEquals(0, new BigDecimal("0.001950000000000000").compareTo(estimate.getEstimatedFee()));
        assertTrue(estimate.getEstimatedAt() != null);
    }

    @Test
    void estimatesTrc20TransferGasInTrx() {
        var estimate = estimator.estimate(GasEstimateRequest.builder()
                .chain(Chain.TRON)
                .network("TRC20")
                .operation(GasOperation.REFUND)
                .token("USDT")
                .amount(new BigDecimal("100"))
                .toAddress("Tabc")
                .build());

        assertEquals("TRX", estimate.getNativeCurrency());
        assertEquals(1L, estimate.getGasLimit());
        assertEquals(0, new BigDecimal("15").compareTo(estimate.getEstimatedFee()));
    }

    @Test
    void estimatesBitcoinMinerFeeInBtc() {
        var estimate = estimator.estimate(GasEstimateRequest.builder()
                .chain(Chain.BTC)
                .network("BTC")
                .operation(GasOperation.NATIVE_TRANSFER)
                .token("BTC")
                .amount(new BigDecimal("0.01"))
                .toAddress("bc1q")
                .build());

        assertEquals("BTC", estimate.getNativeCurrency());
        assertEquals(180L, estimate.getGasLimit());
        assertEquals(0, new BigDecimal("20").compareTo(estimate.getGasPrice()));
        assertEquals(0, new BigDecimal("0.00003600").compareTo(estimate.getEstimatedFee()));
    }

    @Test
    void rejectsUnsupportedSolanaGasEstimation() {
        assertThrows(IllegalArgumentException.class, () -> estimator.estimate(GasEstimateRequest.builder()
                .chain(Chain.SOLANA)
                .network("SOLANA")
                .operation(GasOperation.NATIVE_TRANSFER)
                .build()));
    }
}
