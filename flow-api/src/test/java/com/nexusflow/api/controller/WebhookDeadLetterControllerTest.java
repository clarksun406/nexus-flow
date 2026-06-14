package com.nexusflow.api.controller;

import com.nexusflow.application.WebhookDeadLetterApplicationService;
import com.nexusflow.application.WebhookDeadLetterStatus;
import com.nexusflow.application.dto.WebhookDeadLetterResponse;
import com.nexusflow.common.WebhookDeadLetterNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebhookDeadLetterControllerTest {

    private WebhookDeadLetterApplicationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(WebhookDeadLetterApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WebhookDeadLetterController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listUsesStatusAndLimit() throws Exception {
        when(service.list(WebhookDeadLetterStatus.PENDING, 25))
                .thenReturn(List.of(response("dlq-1", "PENDING")));

        mockMvc.perform(get("/ops/webhook-dead-letters")
                        .param("status", "PENDING")
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("dlq-1"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        verify(service).list(WebhookDeadLetterStatus.PENDING, 25);
    }

    @Test
    void replayReturnsUpdatedDeadLetter() throws Exception {
        when(service.replay("dlq-1")).thenReturn(response("dlq-1", "REPLAYED"));

        mockMvc.perform(post("/ops/webhook-dead-letters/dlq-1/replay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("dlq-1"))
                .andExpect(jsonPath("$.data.status").value("REPLAYED"));

        verify(service).replay("dlq-1");
    }

    @Test
    void ignoreReturnsUpdatedDeadLetter() throws Exception {
        when(service.ignore("dlq-1")).thenReturn(response("dlq-1", "IGNORED"));

        mockMvc.perform(post("/ops/webhook-dead-letters/dlq-1/ignore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("dlq-1"))
                .andExpect(jsonPath("$.data.status").value("IGNORED"));

        verify(service).ignore("dlq-1");
    }

    @Test
    void replayMissingDeadLetterReturnsNotFound() throws Exception {
        when(service.replay("missing")).thenThrow(new WebhookDeadLetterNotFoundException("missing"));

        mockMvc.perform(post("/ops/webhook-dead-letters/missing/replay"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NF-3005"));
    }

    private WebhookDeadLetterResponse response(String id, String status) {
        return WebhookDeadLetterResponse.builder()
                .id(id)
                .deliveryType("CRYPTO_PAYMENT")
                .targetUrl("https://merchant.example/callback")
                .payload("{}")
                .failureReason("timeout")
                .attempts(4)
                .status(status)
                .createdAt(1L)
                .build();
    }
}
