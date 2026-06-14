package com.nexusflow.domain.gas;

/**
 * Port for estimating native-chain gas or miner fees before outbound transfers.
 */
public interface GasEstimator {
    GasEstimate estimate(GasEstimateRequest request);
}
