package com.nexusflow.permission.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PermissionClientProperties.class)
@ConditionalOnProperty(prefix = "permission", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PermissionClientAutoConfiguration {

    @Bean
    public PermissionClient permissionClient(PermissionClientProperties properties) {
        return new PermissionClient(properties);
    }

    @Bean
    public CheckPermissionAspect checkPermissionAspect(PermissionClient permissionClient) {
        return new CheckPermissionAspect(permissionClient);
    }
}
