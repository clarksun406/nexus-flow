package com.nexusflow.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.application.dto.CreatePaymentCommand;
import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.PaymentOrder;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.shared.Money;
import com.nexusflow.domain.wallet.AddressPoolEntry;
import com.nexusflow.domain.wallet.AddressPoolRepository;
import com.nexusflow.domain.wallet.AddressPoolStatus;
import com.nexusflow.domain.wallet.Wallet;
import com.nexusflow.domain.wallet.WalletRepository;
import com.nexusflow.domain.wallet.WalletType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: spins up a real PostgreSQL via Testcontainers,
 * loads the full Spring Boot context, and verifies JPA repository wiring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class NexusFlowApplicationIT {

    private static final String API_KEY = "it-api-key";

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
        registry.add("nexusflow.encryption.key", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("nexusflow.api.key", () -> API_KEY);
        registry.add("nexusflow.api.rate-limit.per-minute", () -> "1000");
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AddressPoolRepository addressPoolRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
        assertThat(orderRepository).isNotNull();
        assertThat(paymentRepository).isNotNull();
        assertThat(walletRepository).isNotNull();
        assertThat(addressPoolRepository).isNotNull();
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

    @Test
    void createPaymentHttpPersistsAndReplaysIdempotentResponse() throws Exception {
        seedAddressPoolEntry("addr-it-http-idem", "THTTPIDEM", 9001);

        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .orderId("order-it-http-idem")
                .currency("USDT_TRC20")
                .amount("10.50")
                .build();

        ResponseEntity<String> first = postPayment(command, "idem-it-http-1");
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode firstBody = objectMapper.readTree(first.getBody());
        assertThat(firstBody.path("success").asBoolean()).isTrue();
        assertThat(firstBody.path("data").path("status").asText()).isEqualTo("PENDING");
        assertThat(firstBody.path("data").path("receivingAddress").asText()).isEqualTo("THTTPIDEM");

        String paymentId = firstBody.path("data").path("paymentId").asText();
        assertThat(paymentId).isNotBlank();
        assertThat(paymentRepository.findById(paymentId)).isPresent();

        AddressPoolEntry assigned = addressPoolRepository.findByAddress("THTTPIDEM").orElseThrow();
        assertThat(assigned.getStatus()).isEqualTo(AddressPoolStatus.ASSIGNED);
        assertThat(assigned.getAssignedPaymentId()).isEqualTo(paymentId);

        ResponseEntity<String> replay = postPayment(command, "idem-it-http-1");
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode replayBody = objectMapper.readTree(replay.getBody());
        assertThat(replayBody.path("data").path("paymentId").asText()).isEqualTo(paymentId);
        assertThat(replayBody.path("data").path("receivingAddress").asText()).isEqualTo("THTTPIDEM");
    }

    @Test
    void concurrentCreatePaymentRequestsAssignDistinctAddresses() throws Exception {
        int requestCount = 4;
        Set<String> seededAddresses = new HashSet<>();
        for (int i = 0; i < requestCount; i++) {
            String address = "TCONCURRENT" + i;
            seededAddresses.add(address);
            seedAddressPoolEntry("addr-it-concurrent-" + i, address, 9100 + i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Callable<JsonNode>> tasks = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                int index = i;
                tasks.add(() -> {
                    start.await();
                    CreatePaymentCommand command = CreatePaymentCommand.builder()
                            .orderId("order-it-concurrent-" + index)
                            .currency("USDT_TRC20")
                            .amount("1.00")
                            .build();
                    ResponseEntity<String> response = postPayment(command, "idem-it-concurrent-" + index);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    return objectMapper.readTree(response.getBody()).path("data");
                });
            }

            List<Future<JsonNode>> futures = tasks.stream().map(executor::submit).toList();
            start.countDown();

            Set<String> assignedAddresses = new HashSet<>();
            Set<String> paymentIds = new HashSet<>();
            for (Future<JsonNode> future : futures) {
                JsonNode data = future.get(30, TimeUnit.SECONDS);
                assertThat(data.path("status").asText()).isEqualTo("PENDING");
                assignedAddresses.add(data.path("receivingAddress").asText());
                paymentIds.add(data.path("paymentId").asText());
            }

            assertThat(assignedAddresses).hasSize(requestCount);
            assertThat(assignedAddresses).isSubsetOf(seededAddresses);
            assertThat(paymentIds).hasSize(requestCount);

            for (String address : assignedAddresses) {
                AddressPoolEntry entry = addressPoolRepository.findByAddress(address).orElseThrow();
                assertThat(entry.getStatus()).isEqualTo(AddressPoolStatus.ASSIGNED);
                assertThat(entry.getAssignedPaymentId()).isIn(paymentIds);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private ResponseEntity<String> postPayment(CreatePaymentCommand command, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", API_KEY);
        headers.set("Idempotency-Key", idempotencyKey);
        return restTemplate.postForEntity("/crypto/payments", new HttpEntity<>(command, headers), String.class);
    }

    private void seedAddressPoolEntry(String id, String address, int derivationIndex) {
        addressPoolRepository.save(AddressPoolEntry.builder()
                .id(id)
                .chain(Chain.TRON)
                .address(address)
                .encryptedPrivateKey("ciphertext-" + id)
                .derivationPath("m/44'/195'/0'/0/" + derivationIndex)
                .derivationIndex(derivationIndex)
                .build());
    }
}
