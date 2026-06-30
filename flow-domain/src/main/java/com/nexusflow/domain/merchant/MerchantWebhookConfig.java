package com.nexusflow.domain.merchant;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MerchantWebhookConfig {
    String configId;
    String merchantId;
    String url;
    String secretHash;
    boolean active;
    Instant createTime;
    Instant updateTime;
}
