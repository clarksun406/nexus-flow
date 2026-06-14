package com.nexusflow.application.dto;

import com.nexusflow.domain.gas.GasBankBalance;
import com.nexusflow.domain.gas.GasBankRecommendation;
import com.nexusflow.domain.gas.GasEstimate;
import com.nexusflow.domain.gas.GasTopUpResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GasBankFundingResult {
    GasBankBalance balance;
    GasEstimate estimate;
    GasBankRecommendation recommendation;
    GasTopUpResult topUpResult;

    public boolean topUpRequested() {
        return topUpResult != null;
    }
}
