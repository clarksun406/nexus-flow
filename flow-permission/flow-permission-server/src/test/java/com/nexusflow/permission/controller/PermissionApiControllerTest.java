package com.nexusflow.permission.controller;

import com.nexusflow.permission.dto.CheckRequest;
import com.nexusflow.permission.dto.CheckResponse;
import com.nexusflow.permission.dto.EffectivePermissionsResponse;
import com.nexusflow.permission.entity.Permission;
import com.nexusflow.permission.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionApiControllerTest {

    @Mock
    private PermissionService permissionService;

    private PermissionApiController controller;

    @BeforeEach
    void setUp() {
        controller = new PermissionApiController(permissionService);
    }

    @Test
    void checkGrantedReturns200() {
        UUID userId = UUID.randomUUID();
        CheckRequest request = new CheckRequest(userId, "payment_order:read", "MERCHANT", null);
        when(permissionService.check(any())).thenReturn(CheckResponse.granted());

        ResponseEntity<CheckResponse> response = controller.check(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isGranted()).isTrue();
    }

    @Test
    void checkDeniedReturns403() {
        UUID userId = UUID.randomUUID();
        CheckRequest request = new CheckRequest(userId, "ops_dashboard:read", "SYSTEM", null);
        when(permissionService.check(any())).thenReturn(CheckResponse.denied("Not authorized"));

        ResponseEntity<CheckResponse> response = controller.check(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().isGranted()).isFalse();
    }

    @Test
    void getEffectiveReturns200() {
        UUID userId = UUID.randomUUID();
        EffectivePermissionsResponse resp = new EffectivePermissionsResponse(
                userId, "MERCHANT", null, List.of("payment_order:read"));
        when(permissionService.getEffectivePermissions(userId, "MERCHANT", null)).thenReturn(resp);

        ResponseEntity<EffectivePermissionsResponse> response = controller.getEffective(userId, "MERCHANT", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPermissions()).contains("payment_order:read");
    }

    @Test
    void listPermissionsReturns200() {
        Permission p = new Permission();
        p.setCode("payment_order:read");
        p.setName("Read Payment Orders");
        p.setScope("MERCHANT");
        when(permissionService.listPermissions(null)).thenReturn(List.of(p));

        ResponseEntity<List<Permission>> response = controller.list(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getCode()).isEqualTo("payment_order:read");
    }

    @Test
    void getPermissionByIdReturns200() {
        UUID id = UUID.randomUUID();
        Permission p = new Permission();
        p.setId(id);
        p.setCode("refund:create");
        when(permissionService.getPermission(id)).thenReturn(p);

        ResponseEntity<Permission> response = controller.get(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo("refund:create");
    }

    @Test
    void getPermissionByIdNotFoundThrows400() {
        UUID id = UUID.randomUUID();
        when(permissionService.getPermission(id))
                .thenThrow(new NoSuchElementException("Permission not found: " + id));

        assertThatThrownBy(() -> controller.get(id))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createPermissionReturns201() {
        PermissionApiController.CreatePermissionRequest req =
                new PermissionApiController.CreatePermissionRequest("refund:create", "Create Refund", "MERCHANT", "desc");
        Permission p = new Permission();
        p.setCode("refund:create");
        p.setName("Create Refund");
        p.setScope("MERCHANT");
        when(permissionService.createPermission(any(), any(), any(), any())).thenReturn(p);

        ResponseEntity<Permission> response = controller.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getCode()).isEqualTo("refund:create");
    }

    @Test
    void createPermissionDuplicateThrows400() {
        PermissionApiController.CreatePermissionRequest req =
                new PermissionApiController.CreatePermissionRequest("refund:create", "Create Refund", "MERCHANT", null);
        when(permissionService.createPermission(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Permission already exists: refund:create"));

        assertThatThrownBy(() -> controller.create(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatePermissionReturns200() {
        UUID id = UUID.randomUUID();
        PermissionApiController.UpdatePermissionRequest req =
                new PermissionApiController.UpdatePermissionRequest("Updated Name", "Updated Desc");
        Permission p = new Permission();
        p.setId(id);
        p.setName("Updated Name");
        when(permissionService.updatePermission(id, "Updated Name", "Updated Desc")).thenReturn(p);

        ResponseEntity<Permission> response = controller.update(id, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Updated Name");
    }

    @Test
    void updatePermissionNotFoundThrows404() {
        UUID id = UUID.randomUUID();
        PermissionApiController.UpdatePermissionRequest req =
                new PermissionApiController.UpdatePermissionRequest("Updated", null);
        when(permissionService.updatePermission(id, "Updated", null))
                .thenThrow(new NoSuchElementException("Permission not found: " + id));

        assertThatThrownBy(() -> controller.update(id, req))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deletePermissionReturns204() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = controller.delete(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(permissionService).deletePermission(id);
    }
}
