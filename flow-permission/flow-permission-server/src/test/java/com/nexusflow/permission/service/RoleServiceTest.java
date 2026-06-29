package com.nexusflow.permission.service;

import com.nexusflow.permission.dto.RoleCreateRequest;
import com.nexusflow.permission.entity.Permission;
import com.nexusflow.permission.entity.Role;
import com.nexusflow.permission.entity.RolePermission;
import com.nexusflow.permission.repository.PermissionRepository;
import com.nexusflow.permission.repository.RolePermissionRepository;
import com.nexusflow.permission.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private PermissionRepository permissionRepository;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepository, rolePermissionRepository, permissionRepository);
    }

    @Test
    void listRolesByScope() {
        when(roleRepository.findByScope("MERCHANT")).thenReturn(List.of(new Role()));
        assertThat(roleService.listRoles("MERCHANT")).hasSize(1);
    }

    @Test
    void listAllRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(new Role(), new Role()));
        assertThat(roleService.listRoles(null)).hasSize(2);
    }

    @Test
    void getRoleByIdFound() {
        UUID id = UUID.randomUUID();
        Role role = new Role();
        role.setId(id);
        role.setCode("MERCHANT_VIEWER");
        when(roleRepository.findById(id)).thenReturn(Optional.of(role));

        assertThat(roleService.getRole(id).getCode()).isEqualTo("MERCHANT_VIEWER");
    }

    @Test
    void getRoleByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(roleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRole(id))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getRoleWithPermissionsSuccess() {
        UUID roleId = UUID.randomUUID();
        UUID permId1 = UUID.randomUUID();
        UUID permId2 = UUID.randomUUID();

        Role role = new Role();
        role.setId(roleId);
        role.setCode("OPS_ADMIN");
        role.setName("Ops Admin");
        role.setScope("SYSTEM");
        role.setSystem(true);

        Permission p1 = new Permission();
        p1.setId(permId1);
        p1.setCode("ops_dashboard:read");
        Permission p2 = new Permission();
        p2.setId(permId2);
        p2.setCode("orphan:read");

        RolePermission rp1 = new RolePermission();
        rp1.setRoleId(roleId);
        rp1.setPermissionId(permId1);
        RolePermission rp2 = new RolePermission();
        rp2.setRoleId(roleId);
        rp2.setPermissionId(permId2);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRoleId(roleId)).thenReturn(List.of(rp1, rp2));
        when(permissionRepository.findById(permId1)).thenReturn(Optional.of(p1));
        when(permissionRepository.findById(permId2)).thenReturn(Optional.of(p2));

        Map<String, Object> result = roleService.getRoleWithPermissions(roleId);

        assertThat(result).containsEntry("code", "OPS_ADMIN");
        assertThat(result).containsEntry("system", true);
        @SuppressWarnings("unchecked")
        var perms = (java.util.Set<String>) result.get("permissions");
        assertThat(perms).containsExactlyInAnyOrder("ops_dashboard:read", "orphan:read");
    }

    @Test
    void getRoleWithPermissionsRoleNotFound() {
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleWithPermissions(roleId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createRoleSuccessWithPermissions() {
        UUID savedRoleId = UUID.randomUUID();
        RoleCreateRequest request = new RoleCreateRequest(
                "CUSTOM_ROLE", "Custom Role", "MERCHANT", "desc",
                List.of("payment_order:read", "refund:create"));

        when(roleRepository.findByCode("CUSTOM_ROLE")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
            Role r = inv.getArgument(0);
            r.setId(savedRoleId);
            return r;
        });
        Role savedRole = new Role();
        savedRole.setId(savedRoleId);
        when(roleRepository.findById(savedRoleId)).thenReturn(Optional.of(savedRole));

        Permission p1 = new Permission();
        p1.setId(UUID.randomUUID());
        p1.setCode("payment_order:read");
        Permission p2 = new Permission();
        p2.setId(UUID.randomUUID());
        p2.setCode("refund:create");
        when(permissionRepository.findByCode("payment_order:read")).thenReturn(Optional.of(p1));
        when(permissionRepository.findByCode("refund:create")).thenReturn(Optional.of(p2));
        when(rolePermissionRepository.save(any(RolePermission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Role result = roleService.createRole(request);

        assertThat(result.getCode()).isEqualTo("CUSTOM_ROLE");
        assertThat(result.isSystem()).isFalse();
        verify(rolePermissionRepository).save(any(RolePermission.class));
    }

    @Test
    void createRoleDuplicateRejected() {
        RoleCreateRequest request = new RoleCreateRequest(
                "DUP_ROLE", "Dup", "MERCHANT", null, List.of());
        when(roleRepository.findByCode("DUP_ROLE")).thenReturn(Optional.of(new Role()));

        assertThatThrownBy(() -> roleService.createRole(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createRoleWithoutPermissions() {
        RoleCreateRequest request = new RoleCreateRequest(
                "NO_PERMS", "No Perms", "SYSTEM", null, null);

        when(roleRepository.findByCode("NO_PERMS")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
            Role r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        Role result = roleService.createRole(request);

        assertThat(result.getCode()).isEqualTo("NO_PERMS");
    }

    @Test
    void updateRoleSuccess() {
        UUID id = UUID.randomUUID();
        Role existing = new Role();
        existing.setId(id);
        existing.setName("Old");
        existing.setSystem(false);
        when(roleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        Role result = roleService.updateRole(id, "New Name", "New Desc");

        assertThat(result.getName()).isEqualTo("New Name");
    }

    @Test
    void updateSystemRoleRejected() {
        UUID id = UUID.randomUUID();
        Role systemRole = new Role();
        systemRole.setId(id);
        systemRole.setSystem(true);
        when(roleRepository.findById(id)).thenReturn(Optional.of(systemRole));

        assertThatThrownBy(() -> roleService.updateRole(id, "New", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("system role");
    }

    @Test
    void deleteRoleSuccess() {
        UUID id = UUID.randomUUID();
        Role role = new Role();
        role.setId(id);
        role.setSystem(false);
        when(roleRepository.findById(id)).thenReturn(Optional.of(role));

        roleService.deleteRole(id);

        verify(rolePermissionRepository).deleteByRoleId(id);
        verify(roleRepository).deleteById(id);
    }

    @Test
    void deleteSystemRoleRejected() {
        UUID id = UUID.randomUUID();
        Role systemRole = new Role();
        systemRole.setId(id);
        systemRole.setSystem(true);
        when(roleRepository.findById(id)).thenReturn(Optional.of(systemRole));

        assertThatThrownBy(() -> roleService.deleteRole(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("system role");
    }

    @Test
    void setRolePermissionsSuccess() {
        UUID roleId = UUID.randomUUID();
        Role role = new Role();
        role.setId(roleId);
        UUID permId = UUID.randomUUID();
        Permission perm = new Permission();
        perm.setId(permId);
        perm.setCode("payment_order:read");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findByCode("payment_order:read")).thenReturn(Optional.of(perm));
        when(rolePermissionRepository.save(any(RolePermission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        roleService.setRolePermissions(roleId, List.of("payment_order:read"));

        verify(rolePermissionRepository).deleteByRoleId(roleId);
        verify(rolePermissionRepository).save(any(RolePermission.class));
    }

    @Test
    void setRolePermissionsUnknownPermissionRejected() {
        UUID roleId = UUID.randomUUID();
        Role role = new Role();
        role.setId(roleId);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findByCode("unknown:perm")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.setRolePermissions(roleId, List.of("unknown:perm")))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("unknown:perm");
    }
}
