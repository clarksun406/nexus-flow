package com.nexusflow.permission.controller;

import com.nexusflow.permission.dto.CheckRequest;
import com.nexusflow.permission.dto.CheckResponse;
import com.nexusflow.permission.dto.EffectivePermissionsResponse;
import com.nexusflow.permission.entity.Permission;
import com.nexusflow.permission.service.PermissionService;
import jakarta.validation.Valid;
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
    public ResponseEntity<CheckResponse> check(@Valid @RequestBody CheckRequest request) {
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
    public ResponseEntity<Permission> create(@Valid @RequestBody CreatePermissionRequest req) {
        Permission p = permissionService.createPermission(req.code(), req.name(), req.scope(), req.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(p);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Permission> update(@PathVariable UUID id, @Valid @RequestBody UpdatePermissionRequest req) {
        return ResponseEntity.ok(permissionService.updatePermission(id, req.name(), req.description()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }

    public record CreatePermissionRequest(
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Pattern(regexp = "^[a-z][a-z0-9_]*:[a-z][a-z0-9_]*$")
            String code,
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 200)
            String name,
            @jakarta.validation.constraints.Pattern(regexp = "^(SYSTEM|MERCHANT|PROVIDER|ORGANIZATION)$")
            String scope,
            @jakarta.validation.constraints.Size(max = 2000)
            String description) {}

    public record UpdatePermissionRequest(
            @jakarta.validation.constraints.Size(max = 200)
            String name,
            @jakarta.validation.constraints.Size(max = 2000)
            String description) {}
}
