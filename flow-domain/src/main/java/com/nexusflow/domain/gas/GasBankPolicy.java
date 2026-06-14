package com.nexusflow.domain.gas;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class GasBankPolicy {
    Chain chain;
    String network;
    String nativeCurrency;
    BigDecimal minimumBalance;
    BigDecimal targetBalance;
    BigDecimal lowGasPriceThreshold;
    BigDecimal highGasPriceThreshold;

    public GasBankRecommendation recommend(GasBankBalance balance, GasEstimate estimate) {
        BigDecimal available = amountOrZero(balance != null ? balance.getAvailableBalance() : null);
        BigDecimal target = amountOrZero(targetBalance);
        BigDecimal topUpAmount = target.subtract(available).max(BigDecimal.ZERO);
        GasPriceBand band = classifyGasPrice(estimate);

        if (isBelowEstimatedFee(available, estimate)) {
            return recommendation(GasBankAction.TOP_UP_NOW, band, topUpAmount,
                    "balance is below estimated transaction fee");
        }
        if (band == GasPriceBand.LOW && topUpAmount.compareTo(BigDecimal.ZERO) > 0) {
            return recommendation(GasBankAction.BATCH_TOP_UP_WHEN_LOW_GAS, band, topUpAmount,
                    "gas price is low and wallet is below target balance");
        }
        if (available.compareTo(amountOrZero(minimumBalance)) < 0) {
            GasBankAction action = band == GasPriceBand.HIGH
                    ? GasBankAction.DEFER_TOP_UP
                    : GasBankAction.TOP_UP_NOW;
            return recommendation(action, band, topUpAmount,
                    "wallet is below minimum gas balance");
        }
        return recommendation(GasBankAction.NONE, band, BigDecimal.ZERO,
                "wallet gas balance is sufficient");
    }

    private GasPriceBand classifyGasPrice(GasEstimate estimate) {
        if (estimate == null || estimate.getGasPrice() == null) {
            return GasPriceBand.UNKNOWN;
        }
        BigDecimal gasPrice = estimate.getGasPrice();
        if (lowGasPriceThreshold != null && gasPrice.compareTo(lowGasPriceThreshold) <= 0) {
            return GasPriceBand.LOW;
        }
        if (highGasPriceThreshold != null && gasPrice.compareTo(highGasPriceThreshold) >= 0) {
            return GasPriceBand.HIGH;
        }
        return GasPriceBand.NORMAL;
    }

    private boolean isBelowEstimatedFee(BigDecimal available, GasEstimate estimate) {
        return estimate != null
                && estimate.getEstimatedFee() != null
                && available.compareTo(estimate.getEstimatedFee()) < 0;
    }

    private GasBankRecommendation recommendation(GasBankAction action, GasPriceBand band,
                                                 BigDecimal topUpAmount, String reason) {
        return GasBankRecommendation.builder()
                .action(action)
                .gasPriceBand(band)
                .topUpAmount(topUpAmount)
                .nativeCurrency(nativeCurrency)
                .reason(reason)
                .build();
    }

    private BigDecimal amountOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
