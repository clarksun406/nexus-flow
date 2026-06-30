package com.nexusflow.domain.merchant;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MerchantCredential {
    String credentialId;
    String merchantId;
    String keyHash;
    String keyPrefix;
    boolean active;
    Instant expiresAt;
    Instant createTime;
    Instant updateTime;
}
