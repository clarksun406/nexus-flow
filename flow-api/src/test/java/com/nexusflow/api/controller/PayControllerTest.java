package com.nexusflow.api.controller;

import com.nexusflow.application.PaymentOrchestrator;
import com.nexusflow.application.dto.OrderResponse;
import com.nexusflow.api.security.MerchantAuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayControllerTest {

    private PaymentOrchestrator orchestrator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orchestrator = mock(PaymentOrchestrator.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PayController(orchestrator))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getOrderReturnsWrappedResponse() throws Exception {
        when(orchestrator.getOrder("pay-1")).thenReturn(OrderResponse.builder()
                .paymentId("pay-1")
                .merchantId("merchant-1")
                .status("CONFIRMED")
                .build());

        mockMvc.perform(get("/pay/order/pay-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").value("pay-1"))
                .andExpect(jsonPath("$.data.merchantId").value("merchant-1"));
    }

    @Test
    void getOrderRejectsCrossMerchantAccess() throws Exception {
        when(orchestrator.getOrder("pay-1")).thenReturn(OrderResponse.builder()
                .paymentId("pay-1")
                .merchantId("merchant-1")
                .status("CONFIRMED")
                .build());

        mockMvc.perform(get("/pay/order/pay-1")
                        .requestAttr(MerchantAuthContext.MERCHANT_ID_ATTRIBUTE, "merchant-2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NF-0004"));
    }

    @Test
    void getOrderAllowsGlobalKeyAccess() throws Exception {
        when(orchestrator.getOrder("pay-1")).thenReturn(OrderResponse.builder()
                .paymentId("pay-1")
                .merchantId("merchant-1")
                .status("CONFIRMED")
                .build());

        mockMvc.perform(get("/pay/order/pay-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").value("pay-1"));
    }
}
