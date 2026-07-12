package com.nexusflow.domain.merchant;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MerchantUserMembership {
    String merchantId;
    String userId;
    String roleCode;
    String status;
    Instant createdAt;
}
