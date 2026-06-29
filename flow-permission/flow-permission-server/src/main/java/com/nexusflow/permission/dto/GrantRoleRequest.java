package com.nexusflow.permission.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record GrantRoleRequest(
    @NotNull
    UUID userId,
    @NotNull
    UUID roleId,
    @Pattern(regexp = "^(SYSTEM|MERCHANT|PROVIDER|ORGANIZATION)$")
    String scopeType,
    UUID scopeId,
    UUID grantedBy
) {}
