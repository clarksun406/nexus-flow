package com.nexusflow.permission.service;

import com.nexusflow.permission.dto.RoleCreateRequest;
import com.nexusflow.permission.entity.Permission;
import com.nexusflow.permission.entity.Role;
import com.nexusflow.permission.entity.RolePermission;
import com.nexusflow.permission.repository.PermissionRepository;
import com.nexusflow.permission.repository.RolePermissionRepository;
import com.nexusflow.permission.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public List<Role> listRoles(String scope) {
        if (scope != null && !scope.isBlank()) {
            return roleRepository.findByScope(scope);
        }
        return roleRepository.findAll();
    }

    public Role getRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + id));
    }

    public Map<String, Object> getRoleWithPermissions(UUID id) {
        Role role = getRole(id);
        List<RolePermission> rps = rolePermissionRepository.findByRoleId(id);
        Set<String> permissionCodes = rps.stream()
                .map(rp -> permissionRepository.findById(rp.getPermissionId()))
                .filter(Optional::isPresent)
                .map(p -> p.get().getCode())
                .collect(Collectors.toSet());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", role.getId());
        result.put("code", role.getCode());
        result.put("name", role.getName());
        result.put("scope", role.getScope());
        result.put("system", role.isSystem());
        result.put("description", role.getDescription());
        result.put("permissions", permissionCodes);
        return result;
    }

    @Transactional
    public Role createRole(RoleCreateRequest request) {
        if (roleRepository.findByCode(request.code()).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + request.code());
        }

        Role role = new Role();
        role.setCode(request.code());
        role.setName(request.name());
        role.setScope(request.scope() != null ? request.scope() : "MERCHANT");
        role.setDescription(request.description());
        role.setSystem(false);
        Role saved = roleRepository.save(role);

        if (request.permissionCodes() != null && !request.permissionCodes().isEmpty()) {
            setRolePermissions(saved.getId(), request.permissionCodes());
        }

        return saved;
    }

    public Role updateRole(UUID id, String name, String description) {
        Role role = getRole(id);
        if (role.isSystem()) {
            throw new IllegalStateException("Cannot modify system role");
        }
        if (name != null) role.setName(name);
        if (description != null) role.setDescription(description);
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(UUID id) {
        Role role = getRole(id);
        if (role.isSystem()) {
            throw new IllegalStateException("Cannot delete system role");
        }
        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }

    @Transactional
    public void setRolePermissions(UUID roleId, List<String> permissionCodes) {
        getRole(roleId);
        rolePermissionRepository.deleteByRoleId(roleId);

        for (String code : permissionCodes) {
            Permission perm = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new NoSuchElementException("Permission not found: " + code));
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(perm.getId());
            rolePermissionRepository.save(rp);
        }
    }
}
