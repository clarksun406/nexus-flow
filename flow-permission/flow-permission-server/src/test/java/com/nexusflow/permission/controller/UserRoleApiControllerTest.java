package com.nexusflow.permission.controller;

import com.nexusflow.permission.dto.GrantRoleRequest;
import com.nexusflow.permission.entity.UserRole;
import com.nexusflow.permission.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleApiControllerTest {

    @Mock
    private UserRoleService userRoleService;

    private UserRoleApiController controller;

    @BeforeEach
    void setUp() {
        controller = new UserRoleApiController(userRoleService);
    }

    @Test
    void getUserRolesReturns200() {
        UUID userId = UUID.randomUUID();
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(UUID.randomUUID());
        ur.setScopeType("MERCHANT");
        when(userRoleService.getUserRoles(userId)).thenReturn(List.of(ur));

        ResponseEntity<List<UserRole>> response = controller.getRoles(userId, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void getUserRolesByScopeTypeReturns200() {
        UUID userId = UUID.randomUUID();
        when(userRoleService.getUserRoles(userId, "SYSTEM")).thenReturn(List.of());

        ResponseEntity<List<UserRole>> response = controller.getRoles(userId, "SYSTEM");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void grantRoleReturns201() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        GrantRoleRequest request = new GrantRoleRequest(userId, roleId, "MERCHANT", null, null);
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setScopeType("MERCHANT");
        when(userRoleService.grantRole(any())).thenReturn(ur);

        ResponseEntity<UserRole> response = controller.grantRole(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUserId()).isEqualTo(userId);
        assertThat(response.getBody().getScopeType()).isEqualTo("MERCHANT");
    }

    @Test
    void revokeRoleReturns204() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.revokeRole(userId, roleId, "MERCHANT", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userRoleService).revokeRole(userId, roleId, "MERCHANT", null);
    }

    @Test
    void revokeRoleWithScopeId() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID scopeId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.revokeRole(userId, roleId, "MERCHANT", scopeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userRoleService).revokeRole(userId, roleId, "MERCHANT", scopeId);
    }

    @Test
    void setRolesReturns200() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UserRoleApiController.SetRolesRequest req = new UserRoleApiController.SetRolesRequest(
                "MERCHANT", null, List.of(roleId), null);

        ResponseEntity<Void> response = controller.setRoles(userId, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userRoleService).setUserRoles(eq(userId), eq("MERCHANT"), isNull(), anyList(), isNull());
    }
}
