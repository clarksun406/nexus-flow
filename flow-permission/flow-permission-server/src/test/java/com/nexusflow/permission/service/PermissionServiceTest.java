package com.nexusflow.permission.service;

import com.nexusflow.permission.dto.CheckRequest;
import com.nexusflow.permission.dto.CheckResponse;
import com.nexusflow.permission.dto.EffectivePermissionsResponse;
import com.nexusflow.permission.entity.Permission;
import com.nexusflow.permission.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService(permissionRepository);
    }

    @Test
    void checkGrantedWhenPermissionExists() {
        UUID userId = UUID.randomUUID();
        CheckRequest request = new CheckRequest(userId, "payment_order:read", "MERCHANT", null);
        when(permissionRepository.findPermissionCodesByUserAndScope(userId, "MERCHANT", null))
                .thenReturn(List.of("payment_order:read", "refund:create"));

        CheckResponse response = permissionService.check(request);

        assertThat(response.isGranted()).isTrue();
        assertThat(response.getReason()).isNull();
    }

    @Test
    void checkDeniedWhenPermissionNotGranted() {
        UUID userId = UUID.randomUUID();
        CheckRequest request = new CheckRequest(userId, "ops_dashboard:read", "MERCHANT", null);
        when(permissionRepository.findPermissionCodesByUserAndScope(userId, "MERCHANT", null))
                .thenReturn(List.of("payment_order:read"));

        CheckResponse response = permissionService.check(request);

        assertThat(response.isGranted()).isFalse();
        assertThat(response.getReason()).contains("ops_dashboard:read");
    }

    @Test
    void checkDeniedWhenUserIdMissing() {
        CheckRequest request = new CheckRequest(null, "payment_order:read", "MERCHANT", null);

        CheckResponse response = permissionService.check(request);

        assertThat(response.isGranted()).isFalse();
        assertThat(response.getReason()).contains("userId");
    }

    @Test
    void checkDeniedWhenPermissionMissing() {
        CheckRequest request = new CheckRequest(UUID.randomUUID(), "", "MERCHANT", null);

        CheckResponse response = permissionService.check(request);

        assertThat(response.isGranted()).isFalse();
        assertThat(response.getReason()).contains("permission");
    }

    @Test
    void checkDefaultsScopTypeToMerchant() {
        UUID userId = UUID.randomUUID();
        CheckRequest request = new CheckRequest(userId, "payment_order:read", null, null);
        when(permissionRepository.findPermissionCodesByUserAndScope(userId, "MERCHANT", null))
                .thenReturn(List.of("payment_order:read"));

        CheckResponse response = permissionService.check(request);

        assertThat(response.isGranted()).isTrue();
    }

    @Test
    void getEffectivePermissionsReturnsResult() {
        UUID userId = UUID.randomUUID();
        when(permissionRepository.findPermissionCodesByUserAndScope(userId, "SYSTEM", null))
                .thenReturn(List.of("ops_dashboard:read", "orphan:read"));

        EffectivePermissionsResponse response = permissionService.getEffectivePermissions(userId, "SYSTEM", null);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getScopeType()).isEqualTo("SYSTEM");
        assertThat(response.getScopeId()).isNull();
        assertThat(response.getPermissions()).containsExactlyInAnyOrder("ops_dashboard:read", "orphan:read");
    }

    @Test
    void getEffectivePermissionsDefaultsScopeToMerchant() {
        UUID userId = UUID.randomUUID();
        when(permissionRepository.findPermissionCodesByUserAndScope(userId, "MERCHANT", null))
                .thenReturn(List.of());

        EffectivePermissionsResponse response = permissionService.getEffectivePermissions(userId, null, null);

        assertThat(response.getScopeType()).isEqualTo("MERCHANT");
    }

    @Test
    void listPermissionsByScope() {
        String scope = "SYSTEM";
        Permission p1 = new Permission();
        p1.setCode("ops_dashboard:read");
        Permission p2 = new Permission();
        p2.setCode("orphan:read");
        when(permissionRepository.findByScope(scope)).thenReturn(List.of(p1, p2));

        List<Permission> result = permissionService.listPermissions(scope);

        assertThat(result).hasSize(2);
    }

    @Test
    void listAllPermissionsWhenScopeIsBlank() {
        when(permissionRepository.findAll()).thenReturn(List.of(new Permission()));

        List<Permission> result = permissionService.listPermissions(null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPermissionByIdFound() {
        UUID id = UUID.randomUUID();
        Permission p = new Permission();
        p.setId(id);
        p.setCode("payment_order:read");
        when(permissionRepository.findById(id)).thenReturn(Optional.of(p));

        Permission result = permissionService.getPermission(id);

        assertThat(result.getCode()).isEqualTo("payment_order:read");
    }

    @Test
    void getPermissionByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(permissionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.getPermission(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void createPermissionSuccess() {
        when(permissionRepository.findByCode("refund:create")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Permission result = permissionService.createPermission("refund:create", "Create Refund", "MERCHANT", "desc");

        assertThat(result.getCode()).isEqualTo("refund:create");
        assertThat(result.getScope()).isEqualTo("MERCHANT");
    }

    @Test
    void createPermissionDuplicateRejected() {
        when(permissionRepository.findByCode("refund:create")).thenReturn(Optional.of(new Permission()));

        assertThatThrownBy(() -> permissionService.createPermission("refund:create", "Create Refund", "MERCHANT", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createPermissionDefaultsScope() {
        when(permissionRepository.findByCode("refund:create")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Permission result = permissionService.createPermission("refund:create", "Create Refund", null, null);

        assertThat(result.getScope()).isEqualTo("MERCHANT");
    }

    @Test
    void updatePermissionSuccess() {
        UUID id = UUID.randomUUID();
        Permission existing = new Permission();
        existing.setId(id);
        existing.setName("Old Name");
        when(permissionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        Permission result = permissionService.updatePermission(id, "New Name", "New Desc");

        assertThat(result.getName()).isEqualTo("New Name");
    }

    @Test
    void updatePermissionNotFound() {
        UUID id = UUID.randomUUID();
        when(permissionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.updatePermission(id, "Name", null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deletePermissionById() {
        UUID id = UUID.randomUUID();

        permissionService.deletePermission(id);

        verify(permissionRepository).deleteById(id);
    }
}
