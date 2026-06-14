package com.nexusflow.infra.persistence;

import com.nexusflow.domain.fiat.FiatRampDirection;
import com.nexusflow.domain.fiat.FiatRampOrder;
import com.nexusflow.domain.fiat.FiatRampStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaFiatRampRepositoryTest {

    private SpringDataFiatRampOrderRepository springDataRepository;
    private JpaFiatRampRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataFiatRampOrderRepository.class);
        repository = new JpaFiatRampRepository(springDataRepository);
    }

    @Test
    void saveMapsDomainFieldsToEntity() {
        FiatRampOrder order = newCompletedOrder();

        repository.save(order);

        ArgumentCaptor<FiatRampOrderEntity> captor = ArgumentCaptor.forClass(FiatRampOrderEntity.class);
        verify(springDataRepository).save(captor.capture());
        FiatRampOrderEntity entity = captor.getValue();
        assertEquals("ramp-order-1", entity.getRampOrderId());
        assertEquals("merchant-1", entity.getMerchantId());
        assertEquals("merchant-order-1", entity.getMerchantOrderNo());
        assertEquals("pay-1", entity.getPaymentId());
        assertEquals("ON_RAMP", entity.getDirection());
        assertEquals("MOONPAY", entity.getProviderId());
        assertEquals("provider-order-1", entity.getProviderOrderId());
        assertEquals("quote-1", entity.getQuoteId());
        assertEquals(0, new BigDecimal("100.00").compareTo(entity.getFiatAmount()));
        assertEquals("USD", entity.getFiatCurrency());
        assertEquals(0, new BigDecimal("100.000000").compareTo(entity.getCryptoAmount()));
        assertEquals("USDT", entity.getToken());
        assertEquals("TRC20", entity.getNetwork());
        assertEquals(0, new BigDecimal("1.00").compareTo(entity.getExchangeRate()));
        assertEquals(0, new BigDecimal("2.50").compareTo(entity.getFeeAmountFiat()));
        assertEquals("TDEST", entity.getWalletAddress());
        assertEquals("https://ramp.example/checkout/1", entity.getCheckoutUrl());
        assertEquals("fiat-transfer-1", entity.getFiatTransferId());
        assertEquals("0xtx", entity.getCryptoTxHash());
        assertEquals("COMPLETED", entity.getStatus());
        assertNotNull(entity.getCreateTime());
        assertNotNull(entity.getUpdateTime());
    }

    @Test
    void savePreservesExistingVersionForUpdates() {
        FiatRampOrderEntity existing = new FiatRampOrderEntity();
        existing.setRampOrderId("ramp-order-1");
        existing.setVersion(3L);
        when(springDataRepository.findById("ramp-order-1")).thenReturn(Optional.of(existing));

        repository.save(newCompletedOrder());

        ArgumentCaptor<FiatRampOrderEntity> captor = ArgumentCaptor.forClass(FiatRampOrderEntity.class);
        verify(springDataRepository).save(captor.capture());
        assertEquals(3L, captor.getValue().getVersion());
    }

    @Test
    void findByProviderOrderIdReconstitutesPersistentFields() {
        Instant created = Instant.parse("2026-06-14T00:00:00Z");
        FiatRampOrderEntity entity = new FiatRampOrderEntity();
        entity.setRampOrderId("ramp-order-1");
        entity.setMerchantId("merchant-1");
        entity.setMerchantOrderNo("merchant-order-1");
        entity.setPaymentId("pay-1");
        entity.setDirection("OFF_RAMP");
        entity.setProviderId("BANXA");
        entity.setProviderOrderId("provider-order-1");
        entity.setQuoteId("quote-1");
        entity.setFiatAmount(new BigDecimal("99.00"));
        entity.setFiatCurrency("EUR");
        entity.setCryptoAmount(new BigDecimal("100.000000"));
        entity.setToken("USDT");
        entity.setNetwork("ERC20");
        entity.setExchangeRate(new BigDecimal("0.99"));
        entity.setFeeAmountFiat(new BigDecimal("1.00"));
        entity.setWalletAddress("0xsource");
        entity.setCheckoutUrl("https://ramp.example/offramp/1");
        entity.setFiatTransferId("fiat-settlement-1");
        entity.setCryptoTxHash("0xtx");
        entity.setNotifyUrl("https://merchant.example/ramp/webhook");
        entity.setReturnUrl("https://merchant.example/return");
        entity.setStatus("COMPLETED");
        entity.setCompleteTime(created);
        entity.setCreateTime(created);
        entity.setUpdateTime(created);
        when(springDataRepository.findByProviderIdAndProviderOrderId("BANXA", "provider-order-1"))
                .thenReturn(Optional.of(entity));

        Optional<FiatRampOrder> found = repository.findByProviderOrderId("BANXA", "provider-order-1");

        assertTrue(found.isPresent());
        FiatRampOrder order = found.get();
        assertEquals("ramp-order-1", order.getRampOrderId());
        assertEquals(FiatRampDirection.OFF_RAMP, order.getDirection());
        assertEquals("BANXA", order.getProviderId());
        assertEquals("provider-order-1", order.getProviderOrderId());
        assertEquals(0, new BigDecimal("99.00").compareTo(order.getFiatAmount()));
        assertEquals("EUR", order.getFiatCurrency());
        assertEquals("ERC20", order.getNetwork());
        assertEquals("fiat-settlement-1", order.getFiatTransferId());
        assertEquals("0xtx", order.getCryptoTxHash());
        assertEquals(FiatRampStatus.COMPLETED, order.getStatus());
        assertEquals(created, order.getCompleteTime());
    }

    @Test
    void findersDelegateToSpringDataRepository() {
        repository.findByRampOrderId("ramp-order-1");
        repository.findByMerchantOrderNo("merchant-1", "merchant-order-1");
        repository.findByPaymentId("pay-1");

        verify(springDataRepository).findById("ramp-order-1");
        verify(springDataRepository).findByMerchantIdAndMerchantOrderNo("merchant-1", "merchant-order-1");
        verify(springDataRepository).findByPaymentId("pay-1");
    }

    private FiatRampOrder newCompletedOrder() {
        FiatRampOrder order = FiatRampOrder.builder()
                .rampOrderId("ramp-order-1")
                .merchantId("merchant-1")
                .merchantOrderNo("merchant-order-1")
                .paymentId("pay-1")
                .direction(FiatRampDirection.ON_RAMP)
                .providerId("MOONPAY")
                .quoteId("quote-1")
                .fiatAmount(new BigDecimal("100.00"))
                .fiatCurrency("USD")
                .cryptoAmount(new BigDecimal("100.000000"))
                .token("USDT")
                .network("TRC20")
                .exchangeRate(new BigDecimal("1.00"))
                .feeAmountFiat(new BigDecimal("2.50"))
                .walletAddress("TDEST")
                .notifyUrl("https://merchant.example/ramp/webhook")
                .returnUrl("https://merchant.example/return")
                .expireTime(Instant.parse("2026-06-14T01:00:00Z"))
                .build();
        order.bindProviderOrder("provider-order-1", "https://ramp.example/checkout/1");
        order.markProcessing("fiat-transfer-1", null);
        order.markCompleted(null, "0xtx");
        return order;
    }
}
