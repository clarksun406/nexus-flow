package com.nexusflow.api.controller;

import com.nexusflow.common.ApiResponse;
import com.nexusflow.common.ErrorCode;
import com.nexusflow.common.IdempotencyViolationException;
import com.nexusflow.common.InvalidStateTransitionException;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.common.OrphanTransactionNotFoundException;
import com.nexusflow.common.PaymentNotFoundException;
import com.nexusflow.common.WebhookDeadLetterNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler that maps domain exceptions to API responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(PaymentNotFoundException e) {
        return ApiResponse.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(OrphanTransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleOrphanNotFound(OrphanTransactionNotFoundException e) {
        return ApiResponse.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(WebhookDeadLetterNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleWebhookDeadLetterNotFound(WebhookDeadLetterNotFoundException e) {
        return ApiResponse.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(IdempotencyViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleIdempotency(IdempotencyViolationException e) {
        return ApiResponse.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse<Void> handleInvalidState(InvalidStateTransitionException e) {
        return ApiResponse.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(NexusFlowException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleNexusFlow(NexusFlowException e) {
        log.error("NexusFlow error: code={}, message={}", e.getErrorCode(), e.getMessage(), e);
        return ApiResponse.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ApiResponse.fail(ErrorCode.INVALID_REQUEST, msg);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ApiResponse.fail(ErrorCode.INVALID_REQUEST, "Invalid parameter: " + e.getName());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return ApiResponse.fail(ErrorCode.INTERNAL_ERROR, "Internal server error");
    }
}
