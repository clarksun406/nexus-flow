package com.nexusflow.permission.client;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class CheckPermissionAspectTest {

    private CheckPermissionAspect aspect;

    @Mock
    private PermissionClient permissionClient;
    @Mock
    private JoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        aspect = new CheckPermissionAspect(permissionClient);
        doReturn(methodSignature).when(joinPoint).getSignature();
        doReturn(DummyController.getMethod()).when(methodSignature).getMethod();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void throwsWhenNoRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        assertThatThrownBy(() -> aspect.check(joinPoint, annotationWith("payment_order:read", "MERCHANT")))
                .isInstanceOf(CheckPermissionAspect.PermissionDeniedException.class)
                .hasMessageContaining("No request context");
    }

    @Test
    void throwsWhenNoUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(() -> aspect.check(joinPoint, annotationWith("payment_order:read", "MERCHANT")))
                .isInstanceOf(CheckPermissionAspect.PermissionDeniedException.class)
                .hasMessageContaining("Not authenticated");
    }

    @Test
    void grantsWhenPermissionCheckPasses() {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId.toString());
        request.setAttribute("merchantId", merchantId.toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(permissionClient.check(userId, "payment_order:read", "MERCHANT", merchantId))
                .thenReturn(true);

        aspect.check(joinPoint, annotationWith("payment_order:read", "MERCHANT"));
    }

    @Test
    void deniesWhenPermissionCheckFails() {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId.toString());
        request.setAttribute("merchantId", merchantId.toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(permissionClient.check(userId, "payment_order:read", "MERCHANT", merchantId))
                .thenReturn(false);

        assertThatThrownBy(() -> aspect.check(joinPoint, annotationWith("payment_order:read", "MERCHANT")))
                .isInstanceOf(CheckPermissionAspect.PermissionDeniedException.class)
                .hasMessageContaining("payment_order:read");
    }

    @Test
    void systemScopeDoesNotLookupScopeId() {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId.toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(permissionClient.check(userId, "ops_dashboard:read", "SYSTEM", null))
                .thenReturn(true);

        aspect.check(joinPoint, annotationWith("ops_dashboard:read", "SYSTEM"));
    }

    @Test
    void merchantScopeUsesMerchantIdFromRequest() {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId.toString());
        request.setAttribute("merchantId", merchantId.toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(permissionClient.check(userId, "payment_order:create", "MERCHANT", merchantId))
                .thenReturn(true);

        aspect.check(joinPoint, annotationWith("payment_order:create", "MERCHANT"));
    }

    @Test
    void organizationScopeUsesOrgIdFromRequest() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId.toString());
        request.setAttribute("orgId", orgId.toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(permissionClient.check(userId, "org:perm", "ORGANIZATION", orgId))
                .thenReturn(true);

        aspect.check(joinPoint, annotationWith("org:perm", "ORGANIZATION"));
    }

    @Test
    void userIdAsUuidTypeWorks() {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId);
        request.setAttribute("merchantId", merchantId.toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(permissionClient.check(userId, "payment_order:read", "MERCHANT", merchantId))
                .thenReturn(true);

        aspect.check(joinPoint, annotationWith("payment_order:read", "MERCHANT"));
    }

    private static CheckPermission annotationWith(String value, String scopeType) {
        return new CheckPermission() {
            @Override
            public Class<CheckPermission> annotationType() {
                return CheckPermission.class;
            }

            @Override
            public String value() {
                return value;
            }

            @Override
            public String scopeType() {
                return scopeType;
            }
        };
    }

    static class DummyController {
        @CheckPermission("test:perm")
        void dummyMethod() {}

        static java.lang.reflect.Method getMethod() {
            try {
                return DummyController.class.getDeclaredMethod("dummyMethod");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
