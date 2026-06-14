package com.nexusflow.application.dto;

import com.nexusflow.domain.gas.GasBankPolicy;
import com.nexusflow.domain.gas.GasOperation;
import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class GasBankFundingCommand {
    String requestId;
    Chain chain;
    String network;
    String walletAddress;
    String token;
    String nativeCurrency;
    GasOperation operation;
    BigDecimal amount;
    String toAddress;
    GasBankPolicy policy;
}
