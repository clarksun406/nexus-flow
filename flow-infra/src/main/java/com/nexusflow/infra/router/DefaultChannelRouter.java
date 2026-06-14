package com.nexusflow.infra.router;

import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRouter;
import com.nexusflow.domain.channel.CurrencyConfig;
import com.nexusflow.domain.channel.CurrencyRateCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default router: filters healthy channels, optionally filters by preferredChannelId,
 * then sorts by exchange rate (best rate for the buyer first).
 *
 * If exchange rate lookup fails for a channel, it is placed at the end of the list.
 * Uses {@link CurrencyRateCache} to avoid repeated upstream calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultChannelRouter implements ChannelRouter {

    private final List<ChannelAdapter> allAdapters;
    private final CurrencyRateCache currencyRateCache;

    @Override
    public List<ChannelAdapter> route(RouteRequest request) {
        String token = request.getToken() != null ? request.getToken() : "USDT";
        String network = request.getNetwork() != null ? request.getNetwork() : "TRC20";
        return allAdapters.stream()
                .filter(ChannelAdapter::isHealthy)
                .filter(a -> request.getPreferredChannelId() == null
                        || a.channelId().equalsIgnoreCase(request.getPreferredChannelId()))
                .filter(a -> supports(a, token, network))
                .sorted(rateComparator(request))
                .collect(Collectors.toList());
    }

    private boolean supports(ChannelAdapter adapter, String token, String network) {
        try {
            List<CurrencyConfig> currencies = adapter.getSupportedCurrencies();
            if (currencies == null || currencies.isEmpty()) {
                return false;
            }
            return currencies.stream().anyMatch(currency ->
                    currency.isEnabled()
                            && token.equalsIgnoreCase(currency.getToken())
                            && network.equalsIgnoreCase(currency.getNetwork()));
        } catch (Exception e) {
            log.warn("Failed to read supported currencies from {}: {}", adapter.channelId(), e.getMessage());
            return false;
        }
    }

    private Comparator<ChannelAdapter> rateComparator(RouteRequest request) {
        String token = request.getToken() != null ? request.getToken() : "USDT";
        String network = request.getNetwork() != null ? request.getNetwork() : "TRC20";
        String quote = request.getCurrencyFiat() != null ? request.getCurrencyFiat() : "USD";

        return Comparator.comparing((ChannelAdapter a) -> getRate(a, token, network, quote))
                .reversed(); // higher rate = more crypto per fiat = better for buyer
    }

    private BigDecimal getRate(ChannelAdapter adapter, String token, String network, String quote) {
        try {
            var rate = currencyRateCache.getExchangeRate(adapter, token, network, quote);
            return rate != null && rate.getPrice() != null ? rate.getPrice() : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get exchange rate from {}: {}", adapter.channelId(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
