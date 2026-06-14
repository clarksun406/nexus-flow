package com.nexusflow.api;

import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.PaymentOrder;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.shared.Money;
import com.nexusflow.domain.wallet.Wallet;
import com.nexusflow.domain.wallet.WalletRepository;
import com.nexusflow.domain.wallet.WalletType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: spins up a real PostgreSQL via Testcontainers,
 * loads the full Spring Boot context, and verifies JPA repository wiring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class NexusFlowApplicationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nexusflow")
            .withUsername("nexusflow")
            .withPassword("nexusflow");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void contextLoads() {
        assertThat(orderRepository).isNotNull();
        assertThat(paymentRepository).isNotNull();
        assertThat(walletRepository).isNotNull();
    }

    @Test
    void canSaveAndRetrieveOrder() {
        PaymentOrder order = PaymentOrder.builder()
                .paymentId("pay-it-1")
                .merchantId("merchant-1")
                .merchantOrderNo("ord-it-1")
                .amountFiat(new BigDecimal("100.00"))
                .currencyFiat("USD")
                .amountCrypto(new BigDecimal("100.00"))
                .currencyCrypto("USDT")
                .network("TRC20")
                .exchangeRate(BigDecimal.ONE)
                .channelId("STUB")
                .channelUserId("user-1")
                .expireTime(Instant.now().plusSeconds(600))
                .build();

        orderRepository.save(order);

        Optional<PaymentOrder> found = orderRepository.findByPaymentId("pay-it-1");
        assertThat(found).isPresent();
        assertThat(found.get().getMerchantOrderNo()).isEqualTo("ord-it-1");
        assertThat(found.get().getCurrencyFiat()).isEqualTo("USD");
        assertThat(found.get().getStatus().name()).isEqualTo("WAITING_PAYMENT");
    }

    @Test
    void canSaveAndRetrieveExecutionPayment() {
        CryptoPayment payment = CryptoPayment.builder()
                .id("crypto-it-1")
                .orderId("crypto-order-it-1")
                .expected(Money.of("USDT_TRC20", new BigDecimal("10.00")))
                .receivingAddress("TITADDR")
                .requiredConfirmations(3)
                .callbackUrl("https://merchant.example/callback")
                .build();
        payment.markPending();
        payment.markDetected("tx-it-1", Money.of("USDT_TRC20", new BigDecimal("10.00")));

        paymentRepository.save(payment);

        Optional<CryptoPayment> found = paymentRepository.findById("crypto-it-1");
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo("crypto-order-it-1");
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.DETECTED);
        assertThat(found.get().getTxHash()).isEqualTo("tx-it-1");
        assertThat(found.get().getExpected().getCurrency()).isEqualTo("USDT_TRC20");
        assertThat(found.get().getExpected().getAmount()).isEqualByComparingTo("10.00");
        assertThat(found.get().getReceived().getCurrency()).isEqualTo("USDT_TRC20");
        assertThat(found.get().getReceived().getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void canSaveAndRetrieveWallet() {
        Wallet wallet = Wallet.builder()
                .id("wallet-it-1")
                .name("TRON hot")
                .chain(Chain.TRON)
                .type(WalletType.HOT)
                .address("TWALLETIT")
                .encryptedPrivateKey("ciphertext")
                .kmsKeyId("kms-key-1")
                .build();

        walletRepository.save(wallet);

        Optional<Wallet> found = walletRepository.findById("wallet-it-1");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("TRON hot");
        assertThat(found.get().getChain()).isEqualTo(Chain.TRON);
        assertThat(found.get().getType()).isEqualTo(WalletType.HOT);
        assertThat(found.get().getAddress()).isEqualTo("TWALLETIT");
        assertThat(found.get().isActive()).isTrue();
    }
}
