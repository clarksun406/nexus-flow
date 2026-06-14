package com.nexusflow.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void okWrapsDataWithSuccessEnvelope() {
        long before = System.currentTimeMillis();

        ApiResponse<Map<String, String>> response = ApiResponse.ok(Map.of("paymentId", "pay-1"));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("0");
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).containsEntry("paymentId", "pay-1");
        assertThat(response.getTimestamp()).isGreaterThanOrEqualTo(before);
    }

    @Test
    void failFallsBackToErrorCodeMessage() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.PAYMENT_NOT_FOUND, null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("NF-1001");
        assertThat(response.getMessage()).isEqualTo("Payment not found");
        assertThat(response.getData()).isNull();
    }

    @Test
    void responseSerializesAsStableEnvelope() throws Exception {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .code("NF-0002")
                .message("bad request")
                .data("details")
                .timestamp(123L)
                .build();

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"code\":\"NF-0002\"");
        assertThat(json).contains("\"message\":\"bad request\"");
        assertThat(json).contains("\"data\":\"details\"");
        assertThat(json).contains("\"timestamp\":123");
    }
}
