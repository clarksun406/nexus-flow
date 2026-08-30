package com.nexusflow.application;

import com.nexusflow.application.dto.OpsFiatRampPageResponse;
import com.nexusflow.application.dto.OpsOrderPageResponse;
import com.nexusflow.application.dto.OpsPaymentPageResponse;
import com.nexusflow.common.PageResult;
import com.nexusflow.domain.fiat.FiatRampDirection;
import com.nexusflow.domain.fiat.FiatRampOrder;
import com.nexusflow.domain.fiat.FiatRampRepository;
import com.nexusflow.domain.fiat.FiatRampStatus;
import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.OrderStatus;
import com.nexusflow.domain.order.PaymentOrder;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsQueryApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private FiatRampRepository fiatRampRepository;
    private OpsQueryApplicationService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        fiatRampRepository = mock(FiatRampRepository.class);
        service = new OpsQueryApplicationService(orderRepository, paymentRepository, fiatRampRepository);
    }

    @Test
    void searchOrdersMapsFieldsAndPassesFilters() {
        PaymentOrder order = PaymentOrder.reconstitute()
                .paymentId("pay-1").merchantId("m-1").merchantOrderNo("ord-1")
                .amountFiat(new BigDecimal("10.00")).currencyFiat("USD")
                .amountCrypto(new BigDecimal("10.00")).currencyCrypto("USDT")
                .network("TRC20").channelId("BITMART")
                .paidAmountFiat(new BigDecimal("5.00")).paidAmountCrypto(new BigDecimal("5.00"))
                .txHash("tx-1").status(OrderStatus.PARTIALLY_PAID)
                .expireTime(NOW.plusSeconds(600)).payTime(NOW)
                .createTime(NOW.minusSeconds(60)).updateTime(NOW)
                .build();
        when(orderRepository.search(OrderStatus.PARTIALLY_PAID, "m-1", 0, 20))
                .thenReturn(PageResult.of(List.of(order), 0, 20, 1));

        OpsOrderPageResponse response = service.searchOrders(OrderStatus.PARTIALLY_PAID, "m-1", 0, 20);

        verify(orderRepository).search(OrderStatus.PARTIALLY_PAID, "m-1", 0, 20);
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        OpsOrderPageResponse.OrderItem item = response.getItems().get(0);
        assertThat(item.getPaymentId()).isEqualTo("pay-1");
        assertThat(item.getStatus()).isEqualTo("PARTIALLY_PAID");
        assertThat(item.getAmountFiat()).isEqualTo("10.00");
        assertThat(item.getPaidAmountCrypto()).isEqualTo("5.00");
        assertThat(item.getCreateTime()).isEqualTo(NOW.minusSeconds(60).toEpochMilli());
    }

    @Test
    void searchOrdersPassesNullFiltersThrough() {
        when(orderRepository.search(null, null, 2, 50)).thenReturn(PageResult.of(List.of(), 2, 50, 0));

        OpsOrderPageResponse response = service.searchOrders(null, null, 2, 50);

        verify(orderRepository).search(null, null, 2, 50);
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotal()).isZero();
    }

    @Test
    void searchPaymentsMapsFieldsAndPassesFilters() {
        CryptoPayment payment = CryptoPayment.reconstitute()
                .id("exec-1").orderId("pay-1")
                .expected(Money.of("USDT_TRC20", new BigDecimal("10.00")))
                .received(Money.of("USDT_TRC20", new BigDecimal("4.00")))
                .status(PaymentStatus.DETECTED)
                .receivingAddress("TADDR").txHash("tx-9")
                .confirmations(2).requiredConfirmations(3)
                .createdAt(NOW.minusSeconds(30)).detectedAt(NOW)
                .build();
        when(paymentRepository.search(PaymentStatus.DETECTED, 0, 20))
                .thenReturn(PageResult.of(List.of(payment), 0, 20, 1));

        OpsPaymentPageResponse response = service.searchPayments(PaymentStatus.DETECTED, 0, 20);

        verify(paymentRepository).search(PaymentStatus.DETECTED, 0, 20);
        OpsPaymentPageResponse.PaymentItem item = response.getItems().get(0);
        assertThat(item.getId()).isEqualTo("exec-1");
        assertThat(item.getCurrency()).isEqualTo("USDT_TRC20");
        assertThat(item.getExpectedAmount()).isEqualTo("10.00");
        assertThat(item.getReceivedAmount()).isEqualTo("4.00");
        assertThat(item.getConfirmations()).isEqualTo(2);
        assertThat(item.getDetectedAt()).isEqualTo(NOW.toEpochMilli());
    }

    @Test
    void searchFiatRampOrdersMapsFieldsAndPassesFilters() {
        FiatRampOrder order = FiatRampOrder.reconstitute()
                .rampOrderId("ramp-1").merchantId("m-1").merchantOrderNo("rord-1")
                .paymentId("pay-1").direction(FiatRampDirection.ON_RAMP)
                .providerId("MOONPAY").status(FiatRampStatus.PENDING_PAYMENT)
                .fiatAmount(new BigDecimal("100.00")).fiatCurrency("USD")
                .cryptoAmount(new BigDecimal("99.00")).token("USDT").network("TRC20")
                .exchangeRate(new BigDecimal("1.01"))
                .createTime(NOW.minusSeconds(30)).updateTime(NOW)
                .build();
        when(fiatRampRepository.search(FiatRampStatus.PENDING_PAYMENT, "m-1", 0, 20))
                .thenReturn(PageResult.of(List.of(order), 0, 20, 1));

        OpsFiatRampPageResponse response = service.searchFiatRampOrders(FiatRampStatus.PENDING_PAYMENT, "m-1", 0, 20);

        verify(fiatRampRepository).search(FiatRampStatus.PENDING_PAYMENT, "m-1", 0, 20);
        OpsFiatRampPageResponse.FiatRampItem item = response.getItems().get(0);
        assertThat(item.getRampOrderId()).isEqualTo("ramp-1");
        assertThat(item.getDirection()).isEqualTo("ON_RAMP");
        assertThat(item.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(item.getFiatAmount()).isEqualTo("100.00");
        assertThat(item.getExchangeRate()).isEqualTo("1.01");
        assertThat(item.getUpdateTime()).isEqualTo(NOW.toEpochMilli());
    }
}
