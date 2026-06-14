package com.nexusflow.domain.gas;

import com.nexusflow.domain.shared.Chain;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GasBankPolicyTest {

    @Test
    void recommendsImmediateTopUpWhenBalanceCannotCoverEstimatedFee() {
        GasBankRecommendation recommendation = policy().recommend(
                balance("0.0005"),
                estimate("30", "0.00195"));

        assertThat(recommendation.getAction()).isEqualTo(GasBankAction.TOP_UP_NOW);
        assertThat(recommendation.getGasPriceBand()).isEqualTo(GasPriceBand.NORMAL);
        assertThat(recommendation.getTopUpAmount()).isEqualByComparingTo("0.0995");
        assertThat(recommendation.requiresFunding()).isTrue();
    }

    @Test
    void defersTopUpWhenBalanceIsBelowMinimumAndGasIsHigh() {
        GasBankRecommendation recommendation = policy().recommend(
                balance("0.02"),
                estimate("90", "0.00585"));

        assertThat(recommendation.getAction()).isEqualTo(GasBankAction.DEFER_TOP_UP);
        assertThat(recommendation.getGasPriceBand()).isEqualTo(GasPriceBand.HIGH);
        assertThat(recommendation.getTopUpAmount()).isEqualByComparingTo("0.08");
    }

    @Test
    void batchesTopUpWhenGasIsLowAndBalanceIsBelowTarget() {
        GasBankRecommendation recommendation = policy().recommend(
                balance("0.06"),
                estimate("10", "0.00065"));

        assertThat(recommendation.getAction()).isEqualTo(GasBankAction.BATCH_TOP_UP_WHEN_LOW_GAS);
        assertThat(recommendation.getGasPriceBand()).isEqualTo(GasPriceBand.LOW);
        assertThat(recommendation.getTopUpAmount()).isEqualByComparingTo("0.04");
    }

    @Test
    void noopsWhenBalanceIsSufficientAndGasIsNormal() {
        GasBankRecommendation recommendation = policy().recommend(
                balance("0.12"),
                estimate("30", "0.00195"));

        assertThat(recommendation.getAction()).isEqualTo(GasBankAction.NONE);
        assertThat(recommendation.getGasPriceBand()).isEqualTo(GasPriceBand.NORMAL);
        assertThat(recommendation.getTopUpAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(recommendation.requiresFunding()).isFalse();
    }

    private GasBankPolicy policy() {
        return GasBankPolicy.builder()
                .chain(Chain.ETH)
                .network("ERC20")
                .nativeCurrency("ETH")
                .minimumBalance(new BigDecimal("0.05"))
                .targetBalance(new BigDecimal("0.10"))
                .lowGasPriceThreshold(new BigDecimal("15"))
                .highGasPriceThreshold(new BigDecimal("60"))
                .build();
    }

    private GasBankBalance balance(String amount) {
        return GasBankBalance.builder()
                .chain(Chain.ETH)
                .network("ERC20")
                .walletAddress("0xwallet")
                .nativeCurrency("ETH")
                .availableBalance(new BigDecimal(amount))
                .checkedAt(Instant.parse("2026-06-14T00:00:00Z"))
                .build();
    }

    private GasEstimate estimate(String gasPrice, String fee) {
        return GasEstimate.builder()
                .chain(Chain.ETH)
                .network("ERC20")
                .operation(GasOperation.REFUND)
                .gasLimit(65_000L)
                .gasPrice(new BigDecimal(gasPrice))
                .estimatedFee(new BigDecimal(fee))
                .nativeCurrency("ETH")
                .estimatedAt(Instant.parse("2026-06-14T00:00:00Z"))
                .build();
    }
}
