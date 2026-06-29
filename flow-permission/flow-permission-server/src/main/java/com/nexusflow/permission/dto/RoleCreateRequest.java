package com.nexusflow.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoleCreateRequest(
    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,99}$")
    String code,
    @NotBlank
    @Size(max = 200)
    String name,
    @Pattern(regexp = "^(SYSTEM|MERCHANT|PROVIDER|ORGANIZATION)$")
    String scope,
    @Size(max = 2000)
    String description,
    List<@Pattern(regexp = "^[a-z][a-z0-9_]*:[a-z][a-z0-9_]*$") String> permissionCodes
) {}
