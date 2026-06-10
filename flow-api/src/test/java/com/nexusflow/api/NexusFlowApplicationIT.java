package com.nexusflow.api;

import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.PaymentOrder;
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

    @Test
    void contextLoads() {
        assertThat(orderRepository).isNotNull();
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
}
