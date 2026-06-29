package com.nexusflow.permission.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PermissionClient {

    private final PermissionClientProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Cache<String, Set<String>> cache;

    public PermissionClient(PermissionClientProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(properties.getCacheMaxSize())
                .build();
    }

    public boolean check(UUID userId, String permission, String scopeType, UUID scopeId) {
        if (!properties.isEnabled()) {
            return true;
        }

        Set<String> effective = getEffectivePermissions(userId, scopeType, scopeId);
        return effective.contains(permission);
    }

    public Set<String> getEffectivePermissions(UUID userId, String scopeType, UUID scopeId) {
        String cacheKey = userId + ":" + scopeType + ":" + (scopeId != null ? scopeId : "null");
        return cache.get(cacheKey, key -> loadEffectivePermissions(userId, scopeType, scopeId));
    }

    public void evictCache(UUID userId) {
        cache.asMap().keySet().removeIf(key -> key.startsWith(userId.toString()));
    }

    public void evictAll() {
        cache.invalidateAll();
    }

    @SuppressWarnings("unchecked")
    private Set<String> loadEffectivePermissions(UUID userId, String scopeType, UUID scopeId) {
        try {
            String url = properties.getServerUrl() + "/api/v1/permission/eff"
                    + "?userId=" + userId
                    + "&scopeType=" + scopeType;
            if (scopeId != null) {
                url += "&scopeId=" + scopeId;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getServiceToken())
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
                Object perms = body.get("permissions");
                if (perms instanceof List<?> list) {
                    return new HashSet<>(list.stream().map(Object::toString).toList());
                }
            }

            log.warn("Permission service returned {}: {}", response.statusCode(), response.body());
            return Set.of();

        } catch (Exception e) {
            log.error("Failed to load permissions from service: {}", e.getMessage());
            return Set.of();
        }
    }
}
