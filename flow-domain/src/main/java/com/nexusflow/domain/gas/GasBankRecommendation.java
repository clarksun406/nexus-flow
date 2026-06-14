package com.nexusflow.domain.gas;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class GasBankRecommendation {
    GasBankAction action;
    GasPriceBand gasPriceBand;
    BigDecimal topUpAmount;
    String nativeCurrency;
    String reason;

    public boolean requiresFunding() {
        return action != null && action != GasBankAction.NONE;
    }
}
