package com.nexusflow.common;

/**
 * Thrown when an invalid state transition is attempted.
 */
public class InvalidStateTransitionException extends NexusFlowException {

    public InvalidStateTransitionException(String from, String to) {
        super(ErrorCode.INVALID_STATE_TRANSITION,
                "Cannot transition from " + from + " to " + to);
    }
}