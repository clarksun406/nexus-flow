package com.nexusflow.permission.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Always registers the {@link PermissionClient} bean: the client itself degrades
 * gracefully when {@code permission.enabled=false} (isEnabled() == false and
 * check() passes through). Removing the bean entirely would break the
 * unconditional injection points in flow-api (AuthService,
 * MerchantUserProvisioningService, CheckPermissionAspect).
 */
@Configuration
@EnableConfigurationProperties(PermissionClientProperties.class)
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
