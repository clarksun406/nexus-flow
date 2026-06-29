package com.nexusflow.permission.controller;

import com.nexusflow.permission.dto.RoleCreateRequest;
import com.nexusflow.permission.entity.Role;
import com.nexusflow.permission.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleApiControllerTest {

    @Mock
    private RoleService roleService;

    private RoleApiController controller;

    @BeforeEach
    void setUp() {
        controller = new RoleApiController(roleService);
    }

    @Test
    void listRolesReturns200() {
        Role r = new Role();
        r.setCode("MERCHANT_VIEWER");
        when(roleService.listRoles(null)).thenReturn(List.of(r));

        ResponseEntity<List<Role>> response = controller.list(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getCode()).isEqualTo("MERCHANT_VIEWER");
    }

    @Test
    void listRolesByScopeReturns200() {
        when(roleService.listRoles("SYSTEM")).thenReturn(List.of(new Role()));

        ResponseEntity<List<Role>> response = controller.list("SYSTEM");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getRoleWithPermissionsReturns200() {
        UUID id = UUID.randomUUID();
        Map<String, Object> result = Map.of(
                "id", id, "code", "MERCHANT_OWNER", "name", "Merchant Owner",
                "scope", "MERCHANT", "system", true, "description", "desc",
                "permissions", Set.of("payment_order:read"));
        when(roleService.getRoleWithPermissions(id)).thenReturn(result);

        ResponseEntity<Map<String, Object>> response = controller.get(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("code", "MERCHANT_OWNER");
    }

    @Test
    void getRoleNotFoundThrows404() {
        UUID id = UUID.randomUUID();
        when(roleService.getRoleWithPermissions(id))
                .thenThrow(new NoSuchElementException("Role not found: " + id));

        assertThatThrownBy(() -> controller.get(id))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createRoleReturns201() {
        RoleCreateRequest request = new RoleCreateRequest(
                "MERCHANT_CUSTOM", "Custom Role", "MERCHANT", "desc", List.of("payment_order:read"));
        Role r = new Role();
        r.setCode("MERCHANT_CUSTOM");
        when(roleService.createRole(any())).thenReturn(r);

        ResponseEntity<Role> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getCode()).isEqualTo("MERCHANT_CUSTOM");
    }

    @Test
    void createRoleDuplicateThrows400() {
        RoleCreateRequest request = new RoleCreateRequest(
                "DUP_ROLE", "Dup", "MERCHANT", null, null);
        when(roleService.createRole(any()))
                .thenThrow(new IllegalArgumentException("Role already exists: DUP_ROLE"));

        assertThatThrownBy(() -> controller.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateRoleReturns200() {
        UUID id = UUID.randomUUID();
        RoleApiController.UpdateRoleRequest req = new RoleApiController.UpdateRoleRequest("Updated Role", "Updated desc");
        Role r = new Role();
        r.setId(id);
        r.setName("Updated Role");
        when(roleService.updateRole(id, "Updated Role", "Updated desc")).thenReturn(r);

        ResponseEntity<Role> response = controller.update(id, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Updated Role");
    }

    @Test
    void updateSystemRoleThrows409() {
        UUID id = UUID.randomUUID();
        RoleApiController.UpdateRoleRequest req = new RoleApiController.UpdateRoleRequest("Updated", null);
        when(roleService.updateRole(id, "Updated", null))
                .thenThrow(new IllegalStateException("Cannot modify system role"));

        assertThatThrownBy(() -> controller.update(id, req))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteRoleReturns204() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = controller.delete(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(roleService).deleteRole(id);
    }

    @Test
    void setRolePermissionsReturns200() {
        UUID roleId = UUID.randomUUID();
        RoleApiController.SetPermissionsRequest req = new RoleApiController.SetPermissionsRequest(
                List.of("payment_order:read", "refund:create"));

        ResponseEntity<Void> response = controller.setPermissions(roleId, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(roleService).setRolePermissions(roleId, List.of("payment_order:read", "refund:create"));
    }

    @Test
    void setRolePermissionsUnknownPermissionThrows404() {
        UUID roleId = UUID.randomUUID();
        RoleApiController.SetPermissionsRequest req = new RoleApiController.SetPermissionsRequest(
                List.of("unknown:perm"));
        doThrow(new NoSuchElementException("Permission not found: unknown:perm"))
                .when(roleService).setRolePermissions(roleId, List.of("unknown:perm"));

        assertThatThrownBy(() -> controller.setPermissions(roleId, req))
                .isInstanceOf(NoSuchElementException.class);
    }
}
