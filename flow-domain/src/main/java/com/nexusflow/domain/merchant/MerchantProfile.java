package com.nexusflow.domain.merchant;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MerchantProfile {
    String merchantId;
    String merchantCode;
    String displayName;
    MerchantStatus status;
    Instant createTime;
    Instant updateTime;
}
