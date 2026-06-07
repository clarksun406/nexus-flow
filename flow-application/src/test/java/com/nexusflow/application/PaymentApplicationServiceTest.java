package com.nexusflow.application;

import com.nexusflow.domain.event.DomainEventPublisher;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Money;
import com.nexusflow.domain.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentApplicationServiceTest {

    private PaymentRepository paymentRepository;
    private WalletRepository walletRepository;
    private DomainEventPublisher eventPublisher;

    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        walletRepository = mock(WalletRepository.class);
        eventPublisher = mock(DomainEventPublisher.class);
        service = new PaymentApplicationService(paymentRepository, walletRepository, eventPublisher);
    }

    private CryptoPayment pendingPaymentAt(String address) {
        CryptoPayment p = CryptoPayment.builder()
                .id("pay-1").orderId("order-1")
                .expected(Money.of("USDT_TRC20", new BigDecimal("100")))
                .receivingAddress(address)
                .requiredConfirmations(3)
                .build();
        p.markPending();
        p.collectEvents(); // drop the PENDING event so assertions focus on detection
        return p;
    }

    @Test
    void detectsMatchingPaymentAndTransitionsToDetected() {
        CryptoPayment payment = pendingPaymentAt("TADDR");
        when(paymentRepository.findByTxHash("tx-1")).thenReturn(Optional.empty());
        when(paymentRepository.findPendingByReceivingAddress("TADDR")).thenReturn(Optional.of(payment));

        service.onPaymentDetected("tx-1", "TADDR", "100", "USDT_TRC20");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DETECTED);
        assertThat(payment.getTxHash()).isEqualTo("tx-1");
        assertThat(payment.getReceived().getAmount()).isEqualByComparingTo("100");
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publish(any());
    }

    @Test
    void skipsWhenTransactionAlreadyProcessed() {
        when(paymentRepository.findByTxHash("tx-1"))
                .thenReturn(Optional.of(pendingPaymentAt("TADDR")));

        service.onPaymentDetected("tx-1", "TADDR", "100", "USDT_TRC20");

        verify(paymentRepository, never()).findPendingByReceivingAddress(anyString());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void skipsWhenNoPendingPaymentAtAddress() {
        when(paymentRepository.findByTxHash("tx-1")).thenReturn(Optional.empty());
        when(paymentRepository.findPendingByReceivingAddress("TADDR")).thenReturn(Optional.empty());

        service.onPaymentDetected("tx-1", "TADDR", "100", "USDT_TRC20");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void skipsOnCurrencyMismatch() {
        CryptoPayment payment = pendingPaymentAt("TADDR"); // expects USDT_TRC20
        when(paymentRepository.findByTxHash("tx-1")).thenReturn(Optional.empty());
        when(paymentRepository.findPendingByReceivingAddress("TADDR")).thenReturn(Optional.of(payment));

        service.onPaymentDetected("tx-1", "TADDR", "100", "ETH");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).save(any());
    }
}
