package com.nexusflow.domain.wallet;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MpcSignature {
    String requestId;
    String mpcWalletId;
    String signedTransaction;
    String providerSignatureId;
    Instant signedAt;
}
