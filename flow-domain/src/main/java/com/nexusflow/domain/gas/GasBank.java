package com.nexusflow.domain.gas;

/**
 * Port for managing native-token balances used to pay outbound transaction fees.
 */
public interface GasBank {
    GasBankBalance getBalance(GasBankBalanceRequest request);
    GasTopUpResult requestTopUp(GasTopUpRequest request);
}
