package com.nexusflow.permission.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "permission")
public class PermissionClientProperties {
    private String serverUrl = "http://localhost:8090";
    private String serviceToken = "";
    private int cacheTtlSeconds = 60;
    private long cacheMaxSize = 10000;
    private boolean enabled = true;
}
