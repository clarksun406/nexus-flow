package com.nexusflow.permission.dto;

import java.util.UUID;

public record GrantRoleRequest(
    UUID userId,
    UUID roleId,
    String scopeType,
    UUID scopeId,
    UUID grantedBy
) {}
