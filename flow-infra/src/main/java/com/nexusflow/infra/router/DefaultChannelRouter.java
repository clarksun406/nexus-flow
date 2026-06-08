package com.nexusflow.infra.router;

import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRouter;
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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultChannelRouter implements ChannelRouter {

    private final List<ChannelAdapter> allAdapters;

    @Override
    public List<ChannelAdapter> route(RouteRequest request) {
        return allAdapters.stream()
                .filter(ChannelAdapter::isHealthy)
                .filter(a -> request.getPreferredChannelId() == null
                        || a.channelId().equalsIgnoreCase(request.getPreferredChannelId()))
                .sorted(rateComparator(request))
                .collect(Collectors.toList());
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
            var rate = adapter.getExchangeRate(token, network, quote);
            return rate != null && rate.getPrice() != null ? rate.getPrice() : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get exchange rate from {}: {}", adapter.channelId(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
