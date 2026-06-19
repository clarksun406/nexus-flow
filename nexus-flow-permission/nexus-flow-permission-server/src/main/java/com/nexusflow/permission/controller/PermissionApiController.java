package com.nexusflow.permission.controller;

import com.nexusflow.permission.dto.CheckRequest;
import com.nexusflow.permission.dto.CheckResponse;
import com.nexusflow.permission.dto.EffectivePermissionsResponse;
import com.nexusflow.permission.entity.Permission;
import com.nexusflow.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/permission")
@RequiredArgsConstructor
public class PermissionApiController {

    private final PermissionService permissionService;

    @PostMapping("/check")
    public ResponseEntity<CheckResponse> check(@RequestBody CheckRequest request) {
        CheckResponse response = permissionService.check(request);
        if (response.isGranted()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @GetMapping("/eff")
    public ResponseEntity<EffectivePermissionsResponse> getEffective(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "MERCHANT") String scopeType,
            @RequestParam(required = false) UUID scopeId) {
        return ResponseEntity.ok(permissionService.getEffectivePermissions(userId, scopeType, scopeId));
    }

    @GetMapping("/list")
    public ResponseEntity<List<Permission>> list(@RequestParam(required = false) String scope) {
        return ResponseEntity.ok(permissionService.listPermissions(scope));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permission> get(@PathVariable UUID id) {
        return ResponseEntity.ok(permissionService.getPermission(id));
    }

    @PostMapping
    public ResponseEntity<Permission> create(@RequestBody CreatePermissionRequest req) {
        Permission p = permissionService.createPermission(req.code(), req.name(), req.scope(), req.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(p);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Permission> update(@PathVariable UUID id, @RequestBody UpdatePermissionRequest req) {
        return ResponseEntity.ok(permissionService.updatePermission(id, req.name(), req.description()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }

    public record CreatePermissionRequest(String code, String name, String scope, String description) {}
    public record UpdatePermissionRequest(String name, String description) {}
}
