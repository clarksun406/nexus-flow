package com.nexusflow.infra.router;

import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Default router: filters healthy channels, optionally filters by preferredChannelId.
 */
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
                .collect(Collectors.toList());
    }
}