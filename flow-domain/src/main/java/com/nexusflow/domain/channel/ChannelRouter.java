package com.nexusflow.domain.channel;

import lombok.Builder;
import lombok.Value;

import java.util.List;

public interface ChannelRouter {

    @Value @Builder
    class RouteRequest {
        String merchantId;
        String token;
        String network;
        String currencyFiat;
        String preferredChannelId;
    }

    List<ChannelAdapter> route(RouteRequest request);
}