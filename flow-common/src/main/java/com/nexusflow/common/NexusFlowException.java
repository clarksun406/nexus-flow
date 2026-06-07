package com.nexusflow.common;

import lombok.Getter;

/**
 * Base exception for all NexusFlow errors.
 */
@Getter
public class NexusFlowException extends RuntimeException {

    private final ErrorCode errorCode;

    public NexusFlowException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public NexusFlowException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public NexusFlowException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public NexusFlowException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
    }
}