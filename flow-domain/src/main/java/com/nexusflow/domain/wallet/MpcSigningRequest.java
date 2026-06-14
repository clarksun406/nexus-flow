package com.nexusflow.domain.wallet;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MpcSigningRequest {
    String requestId;
    String walletId;
    String mpcWalletId;
    Chain chain;
    String unsignedTransaction;
    String metadata;
}
