package com.nexusflow.infra.cache;

import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.CurrencyConfig;
import com.nexusflow.domain.channel.CurrencyRateCache;
import com.nexusflow.domain.channel.ExchangeRate;

import java.util.List;

/**
 * No-op cache — always delegates to the underlying adapter.
 * Used as the default when Redis caching is not enabled.
 */
public class NoOpCurrencyRateCache implements CurrencyRateCache {

    @Override
    public ExchangeRate getExchangeRate(ChannelAdapter adapter, String token, String network, String quoteCurrency) {
        return adapter.getExchangeRate(token, network, quoteCurrency);
    }

    @Override
    public List<CurrencyConfig> getSupportedCurrencies(ChannelAdapter adapter) {
        return adapter.getSupportedCurrencies();
    }
}
