package com.nexusflow.application;

import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentReconciliationJobTest {

    private PaymentRepository paymentRepository;
    private PaymentApplicationService paymentService;
    private BlockchainAdapter tronAdapter;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentService = mock(PaymentApplicationService.class);
        tronAdapter = mock(BlockchainAdapter.class);
    }

    @Test
    void reconcileQueriesChainAndDrivesConfirmation() {
        when(tronAdapter.supportedChain()).thenReturn(Chain.TRON); // read in constructor
        PaymentReconciliationJob job = new PaymentReconciliationJob(
                paymentRepository, paymentService, List.of(tronAdapter), 30);

        CryptoPayment p = mock(CryptoPayment.class);
        when(p.getId()).thenReturn("pay-1");
        when(p.getTxHash()).thenReturn("tx-1");
        when(p.getExpected()).thenReturn(Money.of("USDT_TRC20", BigDecimal.TEN));
        when(paymentRepository.findByStatusIn(anyCollection())).thenReturn(List.of(p));
        when(tronAdapter.getConfirmations("tx-1")).thenReturn(12);

        job.reconcileConfirmations();

        verify(tronAdapter).getConfirmations("tx-1");
        verify(paymentService).confirmPayment("pay-1", 12);
    }

    @Test
    void reconcileSkipsWhenNoAdapterForChain() {
        // no adapters registered → nothing can be reconciled
        PaymentReconciliationJob job = new PaymentReconciliationJob(
                paymentRepository, paymentService, List.of(), 30);

        CryptoPayment p = mock(CryptoPayment.class);
        when(p.getId()).thenReturn("pay-1");
        when(p.getTxHash()).thenReturn("tx-1");
        when(p.getExpected()).thenReturn(Money.of("USDT_TRC20", BigDecimal.TEN));
        when(paymentRepository.findByStatusIn(anyCollection())).thenReturn(List.of(p));

        job.reconcileConfirmations();

        verify(paymentService, never()).confirmPayment(anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void expireOnlyAffectsOverduePendingPayments() {
        PaymentReconciliationJob job = new PaymentReconciliationJob(
                paymentRepository, paymentService, List.of(), 30); // 30-min TTL

        CryptoPayment overdue = mock(CryptoPayment.class);
        when(overdue.getId()).thenReturn("old-1");
        when(overdue.getCreatedAt()).thenReturn(Instant.now().minus(Duration.ofHours(1)));

        CryptoPayment fresh = mock(CryptoPayment.class);
        when(fresh.getCreatedAt()).thenReturn(Instant.now());

        when(paymentRepository.findByStatusIn(anyCollection())).thenReturn(List.of(overdue, fresh));

        job.expireOverduePayments();

        verify(paymentService).expirePayment("old-1");
        verify(paymentService, times(1)).expirePayment(anyString());
    }

    @Test
    void oneFailingPaymentDoesNotAbortTheBatch() {
        when(tronAdapter.supportedChain()).thenReturn(Chain.TRON);
        PaymentReconciliationJob job = new PaymentReconciliationJob(
                paymentRepository, paymentService, List.of(tronAdapter), 30);

        CryptoPayment bad = mock(CryptoPayment.class);
        when(bad.getId()).thenReturn("bad");
        when(bad.getTxHash()).thenReturn("tx-bad");
        when(bad.getExpected()).thenReturn(Money.of("USDT_TRC20", BigDecimal.TEN));

        CryptoPayment good = mock(CryptoPayment.class);
        when(good.getId()).thenReturn("good");
        when(good.getTxHash()).thenReturn("tx-good");
        when(good.getExpected()).thenReturn(Money.of("USDT_TRC20", BigDecimal.TEN));

        when(paymentRepository.findByStatusIn(anyCollection())).thenReturn(List.of(bad, good));
        when(tronAdapter.getConfirmations("tx-bad")).thenThrow(new RuntimeException("node down"));
        when(tronAdapter.getConfirmations("tx-good")).thenReturn(20);

        job.reconcileConfirmations();

        // the good payment is still processed despite the bad one throwing
        verify(paymentService).confirmPayment("good", 20);
    }
}
