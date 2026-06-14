package com.nexusflow.application;

import com.nexusflow.application.dto.FiatRampCallbackRequestDto;
import com.nexusflow.application.dto.FiatRampCreateOrderRequestDto;
import com.nexusflow.application.dto.FiatRampQuoteRequestDto;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.domain.fiat.FiatGateway;
import com.nexusflow.domain.fiat.FiatRampDirection;
import com.nexusflow.domain.fiat.FiatRampOrder;
import com.nexusflow.domain.fiat.FiatRampOrderRequest;
import com.nexusflow.domain.fiat.FiatRampQuote;
import com.nexusflow.domain.fiat.FiatRampQuoteRequest;
import com.nexusflow.domain.fiat.FiatRampRepository;
import com.nexusflow.domain.fiat.FiatRampStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FiatRampApplicationServiceTest {

    private FiatGateway moonPay;
    private FiatGateway banxa;
    private FiatRampRepository repository;
    private FiatRampApplicationService service;

    @BeforeEach
    void setUp() {
        moonPay = mock(FiatGateway.class);
        banxa = mock(FiatGateway.class);
        repository = mock(FiatRampRepository.class);
        service = new FiatRampApplicationService(List.of(moonPay, banxa), repository);

        when(moonPay.gatewayId()).thenReturn("MOONPAY");
        when(moonPay.isHealthy()).thenReturn(true);
        when(banxa.gatewayId()).thenReturn("BANXA");
        when(banxa.isHealthy()).thenReturn(true);
    }

    @Test
    void quoteUsesPreferredHealthyGatewayAndMapsRequest() {
        when(banxa.quote(any())).thenReturn(FiatRampQuote.builder()
                .quoteId("quote-1")
                .providerId("BANXA")
                .direction(FiatRampDirection.ON_RAMP)
                .fiatAmount(new BigDecimal("100.00"))
                .fiatCurrency("USD")
                .cryptoAmount(new BigDecimal("99.50"))
                .token("USDT")
                .network("TRC20")
                .exchangeRate(new BigDecimal("0.995"))
                .feeAmountFiat(new BigDecimal("1.00"))
                .expiresAt(Instant.parse("2026-06-14T00:10:00Z"))
                .build());

        var response = service.quote(FiatRampQuoteRequestDto.builder()
                .merchantId("merchant-1")
                .direction("on_ramp")
                .fiatAmount("100.00")
                .fiatCurrency("usd")
                .token("usdt")
                .network("trc20")
                .country("us")
                .paymentMethod("card")
                .preferredGateway("banxa")
                .build());

        assertThat(response.getQuoteId()).isEqualTo("quote-1");
        assertThat(response.getProviderId()).isEqualTo("BANXA");
        assertThat(response.getCryptoAmount()).isEqualTo("99.50");
        ArgumentCaptor<FiatRampQuoteRequest> captor = ArgumentCaptor.forClass(FiatRampQuoteRequest.class);
        verify(banxa).quote(captor.capture());
        assertThat(captor.getValue().getDirection()).isEqualTo(FiatRampDirection.ON_RAMP);
        assertThat(captor.getValue().getFiatCurrency()).isEqualTo("USD");
        assertThat(captor.getValue().getToken()).isEqualTo("USDT");
        assertThat(captor.getValue().getNetwork()).isEqualTo("TRC20");
        assertThat(captor.getValue().getCountry()).isEqualTo("US");
    }

    @Test
    void createOrderSavesGatewayOrder() {
        when(repository.findByMerchantOrderNo("merchant-1", "order-1")).thenReturn(Optional.empty());
        when(moonPay.createOrder(any())).thenReturn(pendingOrder());

        var response = service.createOrder(FiatRampCreateOrderRequestDto.builder()
                .merchantId("merchant-1")
                .merchantOrderNo("order-1")
                .paymentId("pay-1")
                .direction("ON_RAMP")
                .quoteId("quote-1")
                .fiatAmount("100")
                .fiatCurrency("USD")
                .token("USDT")
                .network("TRC20")
                .walletAddress("TDEST")
                .preferredGateway("MOONPAY")
                .build());

        assertThat(response.getRampOrderId()).isEqualTo("ramp-order-1");
        assertThat(response.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(response.getCheckoutUrl()).isEqualTo("https://ramp.example/checkout/1");
        ArgumentCaptor<FiatRampOrderRequest> requestCaptor = ArgumentCaptor.forClass(FiatRampOrderRequest.class);
        verify(moonPay).createOrder(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getMerchantOrderNo()).isEqualTo("order-1");
        assertThat(requestCaptor.getValue().getToken()).isEqualTo("USDT");
        verify(repository).save(any(FiatRampOrder.class));
    }

    @Test
    void createOrderRejectsDuplicateMerchantOrder() {
        when(repository.findByMerchantOrderNo("merchant-1", "order-1")).thenReturn(Optional.of(pendingOrder()));

        assertThatThrownBy(() -> service.createOrder(FiatRampCreateOrderRequestDto.builder()
                .merchantId("merchant-1")
                .merchantOrderNo("order-1")
                .direction("ON_RAMP")
                .fiatAmount("100")
                .fiatCurrency("USD")
                .token("USDT")
                .network("TRC20")
                .build()))
                .isInstanceOf(NexusFlowException.class)
                .hasMessageContaining("Duplicate fiat ramp order");

        verify(moonPay, never()).createOrder(any());
    }

    @Test
    void callbackCompletesProviderOrderAndPersistsSettlementReferences() {
        FiatRampOrder order = pendingOrder();
        when(repository.findByProviderOrderId("MOONPAY", "provider-order-1")).thenReturn(Optional.of(order));

        var response = service.handleProviderCallback("moonpay", FiatRampCallbackRequestDto.builder()
                .providerOrderId("provider-order-1")
                .status("completed")
                .fiatTransferId("fiat-transfer-1")
                .cryptoTxHash("0xtx")
                .build());

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getFiatTransferId()).isEqualTo("fiat-transfer-1");
        assertThat(response.getCryptoTxHash()).isEqualTo("0xtx");
        assertThat(order.getStatus()).isEqualTo(FiatRampStatus.COMPLETED);
        verify(repository).save(order);
    }

    @Test
    void rejectsQuoteWhenNoHealthyGatewayIsAvailable() {
        when(moonPay.isHealthy()).thenReturn(false);
        when(banxa.isHealthy()).thenReturn(false);

        assertThatThrownBy(() -> service.quote(FiatRampQuoteRequestDto.builder()
                .merchantId("merchant-1")
                .direction("ON_RAMP")
                .fiatAmount("100")
                .fiatCurrency("USD")
                .token("USDT")
                .network("TRC20")
                .build()))
                .isInstanceOf(NexusFlowException.class)
                .hasMessageContaining("No healthy fiat ramp gateway");
    }

    private FiatRampOrder pendingOrder() {
        FiatRampOrder order = FiatRampOrder.builder()
                .rampOrderId("ramp-order-1")
                .merchantId("merchant-1")
                .merchantOrderNo("order-1")
                .paymentId("pay-1")
                .direction(FiatRampDirection.ON_RAMP)
                .providerId("MOONPAY")
                .quoteId("quote-1")
                .fiatAmount(new BigDecimal("100"))
                .fiatCurrency("USD")
                .cryptoAmount(new BigDecimal("99.5"))
                .token("USDT")
                .network("TRC20")
                .exchangeRate(new BigDecimal("0.995"))
                .feeAmountFiat(new BigDecimal("1"))
                .walletAddress("TDEST")
                .expireTime(Instant.parse("2026-06-14T00:10:00Z"))
                .build();
        order.bindProviderOrder("provider-order-1", "https://ramp.example/checkout/1");
        return order;
    }
}
