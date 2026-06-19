package com.nexusflow.permission.dto;

import java.util.List;

public record RoleCreateRequest(
    String code,
    String name,
    String scope,
    String description,
    List<String> permissionCodes
) {}
