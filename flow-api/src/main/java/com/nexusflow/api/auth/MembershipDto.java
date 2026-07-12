package com.nexusflow.api.auth;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class MembershipDto {
    String merchantId;
    String merchantCode;
    String displayName;
    String roleCode;
    Set<String> permissions;
}
