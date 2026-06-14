package com.nexusflow.infra.adapter.coinbase;

import com.nexusflow.domain.channel.ChannelAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoinbaseCommerceAdapterTest {

    private CoinbaseCommerceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CoinbaseCommerceAdapter();
    }

    @Test
    void createDepositAddressReturnsChargeReference() {
        ChannelAdapter.CreateDepositRequest request = ChannelAdapter.CreateDepositRequest.builder()
                .orderId("order-1234567890")
                .token("USDC")
                .network("ERC20")
                .cryptoAmount(new BigDecimal("10"))
                .build();

        var deposit = adapter.createDepositAddress(request);

        assertEquals("0xCOINBASE_STUB_order-12", deposit.getAddress());
        assertEquals("CB_CHARGE_order-1234567890", deposit.getChannelOrderId());
        assertEquals(12, deposit.getRequiredConfirmations());
    }

    @Test
    void supportedCurrenciesIncludeUsdcErc20AndBtc() {
        var currencies = adapter.getSupportedCurrencies();

        assertTrue(currencies.stream().anyMatch(c ->
                "USDC".equals(c.getToken()) && "ERC20".equals(c.getNetwork())));
        assertTrue(currencies.stream().anyMatch(c ->
                "BTC".equals(c.getToken()) && "BTC".equals(c.getNetwork())));
    }

    @Test
    void getExchangeRateReturnsStubQuote() {
        var rate = adapter.getExchangeRate("USDC", "ERC20", "USD");

        assertEquals("USDC", rate.getToken());
        assertEquals("ERC20", rate.getNetwork());
        assertEquals("USD", rate.getQuoteCurrency());
        assertEquals(0, BigDecimal.ONE.compareTo(rate.getPrice()));
    }
}
