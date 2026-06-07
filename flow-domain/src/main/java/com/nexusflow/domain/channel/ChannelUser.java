package com.nexusflow.domain.channel;

import lombok.Builder;
import lombok.Value;

@Value @Builder
public class ChannelUser {
    String channelUserId;
    String channelId;
    boolean newlyCreated;
}