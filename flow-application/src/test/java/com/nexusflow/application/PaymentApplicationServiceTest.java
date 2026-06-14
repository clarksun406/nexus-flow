package com.nexusflow.application;

import com.nexusflow.application.dto.CreatePaymentCommand;
import com.nexusflow.application.dto.PaymentResponse;
import com.nexusflow.common.IdempotencyViolationException;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.domain.blockchain.OrphanTransaction;
import com.nexusflow.domain.blockchain.OrphanTransactionRepository;
import com.nexusflow.domain.event.DomainEventPublisher;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.shared.Money;
import com.nexusflow.domain.wallet.AddressPoolEntry;
import com.nexusflow.domain.wallet.AddressPoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentApplicationServiceTest {

    private PaymentRepository paymentRepository;
    private AddressPoolRepository addressPoolRepository;
    private DomainEventPublisher eventPublisher;
    private WebhookService webhookService;
    private PaymentIdempotencyStore idempotencyStore;
    private OrphanTransactionRepository orphanTransactionRepository;

    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        addressPoolRepository = mock(AddressPoolRepository.class);
        eventPublisher = mock(DomainEventPublisher.class);
        webhookService = mock(WebhookService.class);
        idempotencyStore = mock(PaymentIdempotencyStore.class);
        orphanTransactionRepository = mock(OrphanTransactionRepository.class);
        service = new PaymentApplicationService(
                paymentRepository, addressPoolRepository, eventPublisher, webhookService,
                idempotencyStore, orphanTransactionRepository);
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

    // ── createPayment ──

    @Test
    void createPaymentHappyPath() {
        when(paymentRepository.existsByOrderId("order-1")).thenReturn(false);
        AddressPoolEntry address = AddressPoolEntry.builder()
                .id("addr-1").chain(Chain.TRON).address("TADDR")
                .encryptedPrivateKey("enc").derivationPath("m/44'/195'/0'/0/0")
                .derivationIndex(0).build();
        when(addressPoolRepository.findFirstAvailableByChain(Chain.TRON)).thenReturn(Optional.of(address));

        CreatePaymentCommand cmd = CreatePaymentCommand.builder()
                .orderId("order-1").currency("USDT_TRC20").amount("100").build();

        PaymentResponse resp = service.createPayment(cmd);

        assertThat(resp.getPaymentId()).isNotBlank();
        assertThat(resp.getOrderId()).isEqualTo("order-1");
        assertThat(resp.getReceivingAddress()).isEqualTo("TADDR");
        assertThat(resp.getStatus()).isEqualTo("PENDING");
        verify(paymentRepository).save(any(CryptoPayment.class));
        verify(addressPoolRepository).save(address);
        assertThat(address.getAssignedPaymentId()).isEqualTo(resp.getPaymentId());
        verify(eventPublisher).publish(any());
        verify(webhookService).notifyCryptoPayment(any(CryptoPayment.class), anyList());
    }

    @Test
    void createPaymentRejectsDuplicateOrderId() {
        when(paymentRepository.existsByOrderId("order-1")).thenReturn(true);

        CreatePaymentCommand cmd = CreatePaymentCommand.builder()
                .orderId("order-1").currency("USDT_TRC20").amount("100").build();

        assertThatThrownBy(() -> service.createPayment(cmd))
                .isInstanceOf(IdempotencyViolationException.class);
    }

    @Test
    void createPaymentReservesAndCompletesIdempotencyKey() {
        when(idempotencyStore.find("key-1")).thenReturn(Optional.empty());
        when(idempotencyStore.reserve(eq("key-1"), anyString(), any())).thenReturn(true);
        when(paymentRepository.existsByOrderId("order-1")).thenReturn(false);
        AddressPoolEntry address = AddressPoolEntry.builder()
                .id("addr-1").chain(Chain.TRON).address("TADDR")
                .encryptedPrivateKey("enc").derivationPath("m/44'/195'/0'/0/0")
                .derivationIndex(0).build();
        when(addressPoolRepository.findFirstAvailableByChain(Chain.TRON)).thenReturn(Optional.of(address));
        CreatePaymentCommand cmd = CreatePaymentCommand.builder()
                .orderId("order-1").currency("USDT_TRC20").amount("100")
                .idempotencyKey("key-1").build();

        PaymentResponse response = service.createPayment(cmd);

        verify(idempotencyStore).reserve(eq("key-1"), eq(PaymentApplicationService.requestHashFor(cmd)), any());
        verify(idempotencyStore).complete("key-1", response);
    }

    @Test
    void createPaymentReplaysCachedIdempotentResponse() {
        CreatePaymentCommand cmd = CreatePaymentCommand.builder()
                .orderId("order-1").currency("USDT_TRC20").amount("100")
                .idempotencyKey("key-1").build();
        PaymentResponse cached = PaymentResponse.builder()
                .paymentId("pay-cached").orderId("order-1").status("PENDING").build();
        when(idempotencyStore.find("key-1")).thenReturn(Optional.of(
                new PaymentIdempotencyStore.StoredPaymentResponse(
                        PaymentApplicationService.requestHashFor(cmd), cached)));

        PaymentResponse response = service.createPayment(cmd);

        assertThat(response).isSameAs(cached);
        verify(paymentRepository, never()).save(any());
        verify(addressPoolRepository, never()).save(any());
    }

    @Test
    void createPaymentRejectsIdempotencyKeyWithDifferentRequest() {
        CreatePaymentCommand cmd = CreatePaymentCommand.builder()
                .orderId("order-1").currency("USDT_TRC20").amount("100")
                .idempotencyKey("key-1").build();
        when(idempotencyStore.find("key-1")).thenReturn(Optional.of(
                new PaymentIdempotencyStore.StoredPaymentResponse("different-hash", null)));

        assertThatThrownBy(() -> service.createPayment(cmd))
                .isInstanceOf(IdempotencyViolationException.class);
    }

    @Test
    void createPaymentFailsWhenNoAddressForChain() {
        when(paymentRepository.existsByOrderId("order-1")).thenReturn(false);
        when(addressPoolRepository.findFirstAvailableByChain(Chain.TRON)).thenReturn(Optional.empty());

        CreatePaymentCommand cmd = CreatePaymentCommand.builder()
                .orderId("order-1").currency("USDT_TRC20").amount("100").build();

        assertThatThrownBy(() -> service.createPayment(cmd))
                .isInstanceOf(NexusFlowException.class);
    }

    // ── onPaymentDetected ──

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
        verify(webhookService).notifyCryptoPayment(eq(payment), anyList());
    }

    @Test
    void skipsWhenTransactionAlreadyProcessed() {
        when(paymentRepository.findByTxHash("tx-1"))
                .thenReturn(Optional.of(pendingPaymentAt("TADDR")));

        service.onPaymentDetected("tx-1", "TADDR", "100", "USDT_TRC20");

        verify(paymentRepository, never()).findPendingByReceivingAddress(anyString());
        verify(paymentRepository, never()).save(any());
        verify(webhookService, never()).notifyCryptoPayment(any(), anyList());
    }

    @Test
    void skipsWhenNoPendingPaymentAtAddress() {
        when(paymentRepository.findByTxHash("tx-1")).thenReturn(Optional.empty());
        when(paymentRepository.findPendingByReceivingAddress("TADDR")).thenReturn(Optional.empty());
        when(orphanTransactionRepository.findByChainAndTxHash(Chain.TRON, "tx-1")).thenReturn(Optional.empty());

        service.onPaymentDetected("tx-1", "TADDR", "100", "USDT_TRC20", 123L);

        verify(paymentRepository, never()).save(any());
        ArgumentCaptor<OrphanTransaction> captor = ArgumentCaptor.forClass(OrphanTransaction.class);
        verify(orphanTransactionRepository).save(captor.capture());
        OrphanTransaction orphan = captor.getValue();
        assertThat(orphan.getChain()).isEqualTo(Chain.TRON);
        assertThat(orphan.getTxHash()).isEqualTo("tx-1");
        assertThat(orphan.getToAddress()).isEqualTo("TADDR");
        assertThat(orphan.getAmount()).isEqualTo("100");
        assertThat(orphan.getCurrency()).isEqualTo("USDT_TRC20");
        assertThat(orphan.getBlockNumber()).isEqualTo(123L);
    }

    @Test
    void expirePaymentTransitionsPendingToExpired() {
        CryptoPayment payment = pendingPaymentAt("TADDR");
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));

        service.expirePayment("pay-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(payment.getExpiredAt()).isNotNull();
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publish(any());
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

    @Test
    void rejectsDustPaymentBelow10Percent() {
        CryptoPayment payment = pendingPaymentAt("TADDR"); // expects 100
        when(paymentRepository.findByTxHash("tx-1")).thenReturn(Optional.empty());
        when(paymentRepository.findPendingByReceivingAddress("TADDR")).thenReturn(Optional.of(payment));

        // Send 5 (5% of 100) — below 10% threshold
        service.onPaymentDetected("tx-1", "TADDR", "5", "USDT_TRC20");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING); // rejected
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void acceptsUnderpaymentAbove10Percent() {
        CryptoPayment payment = pendingPaymentAt("TADDR"); // expects 100
        when(paymentRepository.findByTxHash("tx-1")).thenReturn(Optional.empty());
        when(paymentRepository.findPendingByReceivingAddress("TADDR")).thenReturn(Optional.of(payment));

        // Send 50 (50% of 100) — above 10% threshold, accepted as underpayment
        service.onPaymentDetected("tx-1", "TADDR", "50", "USDT_TRC20");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DETECTED);
        verify(paymentRepository).save(payment);
    }
}
