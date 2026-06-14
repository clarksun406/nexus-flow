package com.nexusflow.common;

public class OrphanTransactionNotFoundException extends NexusFlowException {

    public OrphanTransactionNotFoundException(String chain, String txHash) {
        super(ErrorCode.ORPHAN_TRANSACTION_NOT_FOUND,
                "Orphan transaction not found: chain=" + chain + ", txHash=" + txHash);
    }
}
