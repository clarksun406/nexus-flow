package com.nexusflow.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.application.PaymentApplicationService;
import com.nexusflow.application.dto.CreatePaymentCommand;
import com.nexusflow.application.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest {

    private PaymentApplicationService paymentService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentApplicationService.class);
        objectMapper = new ObjectMapper();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createPaymentUsesPrimaryIdempotencyHeader() throws Exception {
        when(paymentService.createPayment(any())).thenReturn(paymentResponse("pay-1", "order-1", "PENDING"));

        mockMvc.perform(post("/crypto/payments")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createCommand("order-1", "100.25"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value("pay-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        ArgumentCaptor<CreatePaymentCommand> captor = ArgumentCaptor.forClass(CreatePaymentCommand.class);
        verify(paymentService).createPayment(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("idem-1");
    }

    @Test
    void createPaymentFallsBackToXIdempotencyHeader() throws Exception {
        when(paymentService.createPayment(any())).thenReturn(paymentResponse("pay-2", "order-2", "PENDING"));

        mockMvc.perform(post("/crypto/payments")
                        .header("Idempotency-Key", " ")
                        .header("X-Idempotency-Key", "idem-x")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createCommand("order-2", "42"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").value("pay-2"));

        ArgumentCaptor<CreatePaymentCommand> captor = ArgumentCaptor.forClass(CreatePaymentCommand.class);
        verify(paymentService).createPayment(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("idem-x");
    }

    @Test
    void createPaymentRejectsInvalidAmountBeforeServiceCall() throws Exception {
        assertInvalidAmount("abc");
        assertInvalidAmount("0");
    }

    private void assertInvalidAmount(String amount) throws Exception {
        mockMvc.perform(post("/crypto/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createCommand("order-bad", amount))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NF-0002"))
                .andExpect(jsonPath("$.message").value("amount: amount must be a positive decimal"));
    }

    @Test
    void getPaymentReturnsWrappedResponse() throws Exception {
        when(paymentService.getPayment("pay-3")).thenReturn(paymentResponse("pay-3", "order-3", "CONFIRMED"));

        mockMvc.perform(get("/crypto/payments/pay-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("order-3"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void confirmPaymentUsesDefaultConfirmationsAndReturnsLatestPayment() throws Exception {
        when(paymentService.getPayment("pay-4")).thenReturn(paymentResponse("pay-4", "order-4", "CONFIRMED"));

        mockMvc.perform(post("/crypto/payments/pay-4/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        verify(paymentService).confirmPayment("pay-4", 12);
    }

    @Test
    void failPaymentPassesReasonAndReturnsLatestPayment() throws Exception {
        when(paymentService.getPayment("pay-5")).thenReturn(paymentResponse("pay-5", "order-5", "FAILED"));

        mockMvc.perform(post("/crypto/payments/pay-5/fail")
                        .param("reason", "expired on chain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        verify(paymentService).failPayment("pay-5", "expired on chain");
    }

    private CreatePaymentCommand createCommand(String orderId, String amount) {
        return CreatePaymentCommand.builder()
                .orderId(orderId)
                .currency("USDT_TRC20")
                .amount(amount)
                .callbackUrl("https://merchant.example/callback")
                .build();
    }

    private PaymentResponse paymentResponse(String paymentId, String orderId, String status) {
        return PaymentResponse.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .currency("USDT_TRC20")
                .expectedAmount("100.25")
                .receivingAddress("TADDR")
                .status(status)
                .confirmations(0)
                .createdAt(1L)
                .build();
    }
}
