package com.nexusflow.infra.router;

import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRouter;
import com.nexusflow.domain.channel.CurrencyConfig;
import com.nexusflow.domain.channel.CurrencyRateCache;
import com.nexusflow.domain.channel.ExchangeRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultChannelRouterTest {

    @Test
    void filtersOutChannelsThatDoNotSupportRequestedAsset() {
        ChannelAdapter usdtChannel = channel("USDT_CHANNEL", true,
                currency("USDT", "TRC20"));
        ChannelAdapter usdcChannel = channel("USDC_CHANNEL", true,
                currency("USDC", "ERC20"));
        CurrencyRateCache rateCache = mock(CurrencyRateCache.class);
        when(rateCache.getExchangeRate(usdtChannel, "USDT", "TRC20", "USD"))
                .thenReturn(rate("USDT", "TRC20", "USD", "1.00"));

        DefaultChannelRouter router = new DefaultChannelRouter(List.of(usdtChannel, usdcChannel), rateCache);

        List<ChannelAdapter> routed = router.route(ChannelRouter.RouteRequest.builder()
                .token("USDT")
                .network("TRC20")
                .currencyFiat("USD")
                .build());

        assertEquals(1, routed.size());
        assertEquals("USDT_CHANNEL", routed.get(0).channelId());
    }

    @Test
    void preferredChannelIsNotReturnedWhenItDoesNotSupportRequestedAsset() {
        ChannelAdapter usdcChannel = channel("COINBASE_COMMERCE", true,
                currency("USDC", "ERC20"));
        CurrencyRateCache rateCache = mock(CurrencyRateCache.class);
        DefaultChannelRouter router = new DefaultChannelRouter(List.of(usdcChannel), rateCache);

        List<ChannelAdapter> routed = router.route(ChannelRouter.RouteRequest.builder()
                .token("USDT")
                .network("TRC20")
                .currencyFiat("USD")
                .preferredChannelId("COINBASE_COMMERCE")
                .build());

        assertTrue(routed.isEmpty());
    }

    private ChannelAdapter channel(String channelId, boolean healthy, CurrencyConfig... currencies) {
        ChannelAdapter adapter = mock(ChannelAdapter.class);
        when(adapter.channelId()).thenReturn(channelId);
        when(adapter.isHealthy()).thenReturn(healthy);
        when(adapter.getSupportedCurrencies()).thenReturn(List.of(currencies));
        return adapter;
    }

    private CurrencyConfig currency(String token, String network) {
        return CurrencyConfig.builder()
                .token(token)
                .network(network)
                .enabled(true)
                .build();
    }

    private ExchangeRate rate(String token, String network, String quote, String price) {
        return ExchangeRate.builder()
                .token(token)
                .network(network)
                .quoteCurrency(quote)
                .price(new BigDecimal(price))
                .timestamp(Instant.now())
                .build();
    }
}
