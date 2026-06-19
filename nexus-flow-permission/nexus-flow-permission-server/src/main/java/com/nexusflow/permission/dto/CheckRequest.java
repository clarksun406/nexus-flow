package com.nexusflow.permission.dto;

import java.util.UUID;

public record CheckRequest(
    UUID userId,
    String permission,
    String scopeType,
    UUID scopeId
) {}
