package com.nexusflow.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.application.FiatRampApplicationService;
import com.nexusflow.application.dto.FiatRampCreateOrderRequestDto;
import com.nexusflow.application.dto.FiatRampOrderResponseDto;
import com.nexusflow.application.dto.FiatRampQuoteRequestDto;
import com.nexusflow.application.dto.FiatRampQuoteResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FiatRampControllerTest {

    private FiatRampApplicationService service;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service = mock(FiatRampApplicationService.class);
        objectMapper = new ObjectMapper();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new FiatRampController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void quoteReturnsWrappedResponse() throws Exception {
        when(service.quote(any())).thenReturn(FiatRampQuoteResponseDto.builder()
                .quoteId("quote-1")
                .providerId("MOONPAY")
                .direction("ON_RAMP")
                .fiatAmount("100")
                .fiatCurrency("USD")
                .cryptoAmount("99.5")
                .token("USDT")
                .network("TRC20")
                .exchangeRate("0.995")
                .build());

        mockMvc.perform(post("/fiat/ramp/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(quoteRequest("100"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quoteId").value("quote-1"))
                .andExpect(jsonPath("$.data.providerId").value("MOONPAY"));

        ArgumentCaptor<FiatRampQuoteRequestDto> captor = ArgumentCaptor.forClass(FiatRampQuoteRequestDto.class);
        verify(service).quote(captor.capture());
        assertThat(captor.getValue().getPreferredGateway()).isEqualTo("MOONPAY");
    }

    @Test
    void createOrderReturnsWrappedResponse() throws Exception {
        when(service.createOrder(any())).thenReturn(orderResponse("PENDING_PAYMENT"));

        mockMvc.perform(post("/fiat/ramp/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createOrderRequest("100"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rampOrderId").value("ramp-order-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
    }

    @Test
    void getOrderReturnsWrappedResponse() throws Exception {
        when(service.getOrder("ramp-order-1")).thenReturn(orderResponse("COMPLETED"));

        mockMvc.perform(get("/fiat/ramp/orders/ramp-order-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rampOrderId").value("ramp-order-1"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(service).getOrder("ramp-order-1");
    }

    @Test
    void createOrderRejectsInvalidFiatAmountBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/fiat/ramp/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createOrderRequest("0"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NF-0002"))
                .andExpect(jsonPath("$.message").value("fiatAmount: fiatAmount must be a positive decimal"));

        verify(service, never()).createOrder(any());
    }

    private FiatRampQuoteRequestDto quoteRequest(String fiatAmount) {
        return FiatRampQuoteRequestDto.builder()
                .merchantId("merchant-1")
                .direction("ON_RAMP")
                .fiatAmount(fiatAmount)
                .fiatCurrency("USD")
                .token("USDT")
                .network("TRC20")
                .preferredGateway("MOONPAY")
                .build();
    }

    private FiatRampCreateOrderRequestDto createOrderRequest(String fiatAmount) {
        return FiatRampCreateOrderRequestDto.builder()
                .merchantId("merchant-1")
                .merchantOrderNo("order-1")
                .direction("ON_RAMP")
                .quoteId("quote-1")
                .fiatAmount(fiatAmount)
                .fiatCurrency("USD")
                .token("USDT")
                .network("TRC20")
                .walletAddress("TDEST")
                .preferredGateway("MOONPAY")
                .build();
    }

    private FiatRampOrderResponseDto orderResponse(String status) {
        return FiatRampOrderResponseDto.builder()
                .rampOrderId("ramp-order-1")
                .merchantId("merchant-1")
                .merchantOrderNo("order-1")
                .direction("ON_RAMP")
                .providerId("MOONPAY")
                .providerOrderId("provider-order-1")
                .fiatAmount("100")
                .fiatCurrency("USD")
                .cryptoAmount("99.5")
                .token("USDT")
                .network("TRC20")
                .checkoutUrl("https://ramp.example/checkout/1")
                .status(status)
                .build();
    }
}
