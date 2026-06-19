package com.nexusflow.permission.controller;

import com.nexusflow.permission.dto.GrantRoleRequest;
import com.nexusflow.permission.entity.UserRole;
import com.nexusflow.permission.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserRoleApiController {

    private final UserRoleService userRoleService;

    @GetMapping("/{userId}/roles")
    public ResponseEntity<List<UserRole>> getRoles(
            @PathVariable UUID userId,
            @RequestParam(required = false) String scopeType) {
        if (scopeType != null) {
            return ResponseEntity.ok(userRoleService.getUserRoles(userId, scopeType));
        }
        return ResponseEntity.ok(userRoleService.getUserRoles(userId));
    }

    @PostMapping("/grant-role")
    public ResponseEntity<UserRole> grantRole(@RequestBody GrantRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userRoleService.grantRole(request));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> revokeRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId,
            @RequestParam(defaultValue = "MERCHANT") String scopeType,
            @RequestParam(required = false) UUID scopeId) {
        userRoleService.revokeRole(userId, roleId, scopeType, scopeId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<Void> setRoles(
            @PathVariable UUID userId,
            @RequestBody SetRolesRequest req) {
        userRoleService.setUserRoles(userId, req.scopeType(), req.scopeId(), req.roleIds(), req.grantedBy());
        return ResponseEntity.ok().build();
    }

    public record SetRolesRequest(String scopeType, UUID scopeId, List<UUID> roleIds, UUID grantedBy) {}
}
