package com.nexusflow.permission.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Aspect
@Order(200)
@RequiredArgsConstructor
public class CheckPermissionAspect {

    private final PermissionClient permissionClient;

    @Before("@annotation(checkPermission)")
    public void check(JoinPoint joinPoint, CheckPermission checkPermission) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            throw new PermissionDeniedException("No request context");
        }

        UUID userId = getAttributeUUID(request, "userId");
        if (userId == null) {
            throw new PermissionDeniedException("Not authenticated");
        }

        String scopeType = checkPermission.scopeType();
        UUID scopeId = null;
        if (!"SYSTEM".equals(scopeType)) {
            if ("MERCHANT".equals(scopeType)) {
                scopeId = getAttributeUUID(request, "merchantId");
            } else if ("ORGANIZATION".equals(scopeType)) {
                scopeId = getAttributeUUID(request, "orgId");
            }
        }

        boolean granted = permissionClient.check(userId, checkPermission.value(), scopeType, scopeId);

        if (!granted) {
            String method = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
            log.warn("Permission denied: user={} method={} required={} scope={}",
                    userId, method, checkPermission.value(), scopeType);
            throw new PermissionDeniedException("Insufficient permissions: " + checkPermission.value());
        }
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private UUID getAttributeUUID(HttpServletRequest request, String name) {
        try {
            Object attr = request.getAttribute(name);
            if (attr instanceof UUID uuid) return uuid;
            if (attr instanceof String str) return UUID.fromString(str);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static class PermissionDeniedException extends RuntimeException {
        public PermissionDeniedException(String message) {
            super(message);
        }
    }
}
