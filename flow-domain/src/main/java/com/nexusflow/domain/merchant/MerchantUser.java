package com.nexusflow.domain.merchant;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MerchantUser {
    String userId;
    String email;
    String displayName;
    MerchantUserStatus status;
    Instant createdAt;
    Instant updatedAt;
}
