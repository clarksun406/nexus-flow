package com.nexusflow.api.auth;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserInfoResponse {
    String userId;
    String email;
    String displayName;
    String activeMerchantId;
    List<MembershipDto> memberships;
}
