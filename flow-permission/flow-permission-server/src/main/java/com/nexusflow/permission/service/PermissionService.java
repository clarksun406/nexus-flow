package com.nexusflow.permission.service;

import com.nexusflow.permission.dto.CheckRequest;
import com.nexusflow.permission.dto.CheckResponse;
import com.nexusflow.permission.dto.EffectivePermissionsResponse;
import com.nexusflow.permission.entity.Permission;
import com.nexusflow.permission.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public CheckResponse check(CheckRequest request) {
        if (request.userId() == null) {
            return CheckResponse.denied("userId is required");
        }
        if (request.permission() == null || request.permission().isBlank()) {
            return CheckResponse.denied("permission is required");
        }

        String scopeType = request.scopeType() != null ? request.scopeType() : "MERCHANT";

        List<String> effective = permissionRepository.findPermissionCodesByUserAndScope(
                request.userId(), scopeType, request.scopeId());

        if (effective.contains(request.permission())) {
            return CheckResponse.granted();
        }

        return CheckResponse.denied(
                "Permission " + request.permission() + " not found for user in scope " + scopeType);
    }

    public EffectivePermissionsResponse getEffectivePermissions(UUID userId, String scopeType, UUID scopeId) {
        if (scopeType == null) scopeType = "MERCHANT";

        List<String> permissions = permissionRepository.findPermissionCodesByUserAndScope(
                userId, scopeType, scopeId);

        return new EffectivePermissionsResponse(userId, scopeType, scopeId, permissions);
    }

    public List<Permission> listPermissions(String scope) {
        if (scope != null && !scope.isBlank()) {
            return permissionRepository.findByScope(scope);
        }
        return permissionRepository.findAll();
    }

    public Permission getPermission(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Permission not found: " + id));
    }

    public Permission createPermission(String code, String name, String scope, String description) {
        if (permissionRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Permission already exists: " + code);
        }
        Permission p = new Permission();
        p.setCode(code);
        p.setName(name);
        p.setScope(scope != null ? scope : "MERCHANT");
        p.setDescription(description);
        return permissionRepository.save(p);
    }

    public Permission updatePermission(UUID id, String name, String description) {
        Permission p = getPermission(id);
        if (name != null) p.setName(name);
        if (description != null) p.setDescription(description);
        return permissionRepository.save(p);
    }

    public void deletePermission(UUID id) {
        permissionRepository.deleteById(id);
    }
}
