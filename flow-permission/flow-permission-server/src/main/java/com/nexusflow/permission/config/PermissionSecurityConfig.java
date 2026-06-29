package com.nexusflow.permission.config;

import com.nexusflow.permission.security.ServiceTokenFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PermissionSecurityConfig {

    @Value("${permission.auth.enabled:true}")
    private boolean authEnabled;

    @Value("${permission.service-token:}")
    private String serviceToken;

    @Bean
    public FilterRegistrationBean<Filter> permissionServiceTokenFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ServiceTokenFilter(authEnabled, serviceToken));
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(0);
        registration.setName("permissionServiceTokenFilter");
        return registration;
    }
}
