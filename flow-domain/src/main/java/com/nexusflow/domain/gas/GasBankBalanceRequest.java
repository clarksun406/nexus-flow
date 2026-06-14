package com.nexusflow.domain.gas;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GasBankBalanceRequest {
    Chain chain;
    String network;
    String walletAddress;
    String nativeCurrency;
}
