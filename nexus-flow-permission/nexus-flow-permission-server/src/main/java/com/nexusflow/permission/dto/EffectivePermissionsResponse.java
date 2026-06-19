package com.nexusflow.permission.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class EffectivePermissionsResponse {

    private final UUID userId;
    private final String scopeType;
    private final UUID scopeId;
    private final List<String> permissions;
}
