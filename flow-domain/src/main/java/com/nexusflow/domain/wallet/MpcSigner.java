package com.nexusflow.domain.wallet;

/**
 * Port for delegating transaction signing to an external MPC provider.
 */
public interface MpcSigner {
    MpcSignature sign(MpcSigningRequest request);
}
