package com.nexusflow.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.application.FiatRampApplicationService;
import com.nexusflow.application.PaymentOrchestrator;
import com.nexusflow.application.dto.FiatRampCallbackRequestDto;
import com.nexusflow.application.dto.FiatRampOrderResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CallbackControllerTest {

    private FiatRampApplicationService fiatRampService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PaymentOrchestrator orchestrator = mock(PaymentOrchestrator.class);
        fiatRampService = mock(FiatRampApplicationService.class);
        objectMapper = new ObjectMapper();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CallbackController(orchestrator, fiatRampService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void fiatRampCallbackDelegatesToApplicationService() throws Exception {
        when(fiatRampService.handleProviderCallback(eq("MOONPAY"), any())).thenReturn(
                FiatRampOrderResponseDto.builder()
                        .rampOrderId("ramp-order-1")
                        .providerId("MOONPAY")
                        .providerOrderId("provider-order-1")
                        .status("COMPLETED")
                        .fiatTransferId("fiat-transfer-1")
                        .cryptoTxHash("0xtx")
                        .build());

        mockMvc.perform(post("/callback/MOONPAY/fiat-ramp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(FiatRampCallbackRequestDto.builder()
                                .providerOrderId("provider-order-1")
                                .status("COMPLETED")
                                .fiatTransferId("fiat-transfer-1")
                                .cryptoTxHash("0xtx")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rampOrderId").value("ramp-order-1"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        ArgumentCaptor<FiatRampCallbackRequestDto> captor =
                ArgumentCaptor.forClass(FiatRampCallbackRequestDto.class);
        verify(fiatRampService).handleProviderCallback(eq("MOONPAY"), captor.capture());
        assertThat(captor.getValue().getProviderOrderId()).isEqualTo("provider-order-1");
        assertThat(captor.getValue().getCryptoTxHash()).isEqualTo("0xtx");
    }
}
