package com.nexusflow.domain.channel;

import java.util.List;

/**
 * Cache port for currency configurations and exchange rates.
 * Implementations may use in-memory, Redis, or other backends.
 */
public interface CurrencyRateCache {

    /**
     * Returns the exchange rate, potentially cached.
     * Implementations should fall back to the adapter on cache miss or failure.
     */
    ExchangeRate getExchangeRate(ChannelAdapter adapter, String token, String network, String quoteCurrency);

    /**
     * Returns supported currencies, potentially cached.
     * Implementations should fall back to the adapter on cache miss or failure.
     */
    List<CurrencyConfig> getSupportedCurrencies(ChannelAdapter adapter);
}
