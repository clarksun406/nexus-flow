package com.nexusflow.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CheckRequest(
    @NotNull
    UUID userId,
    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]*:[a-z][a-z0-9_]*$")
    String permission,
    @Pattern(regexp = "^(SYSTEM|MERCHANT|PROVIDER|ORGANIZATION)$")
    String scopeType,
    UUID scopeId
) {}
