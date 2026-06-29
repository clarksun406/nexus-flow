package com.nexusflow.permission.controller;

import com.nexusflow.permission.dto.RoleCreateRequest;
import com.nexusflow.permission.entity.Role;
import com.nexusflow.permission.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/role")
@RequiredArgsConstructor
public class RoleApiController {

    private final RoleService roleService;

    @GetMapping("/list")
    public ResponseEntity<List<Role>> list(@RequestParam(required = false) String scope) {
        return ResponseEntity.ok(roleService.listRoles(scope));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.getRoleWithPermissions(id));
    }

    @PostMapping
    public ResponseEntity<Role> create(@Valid @RequestBody RoleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Role> update(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest req) {
        return ResponseEntity.ok(roleService.updateRole(id, req.name(), req.description()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<Void> setPermissions(@PathVariable UUID id, @Valid @RequestBody SetPermissionsRequest req) {
        roleService.setRolePermissions(id, req.permissionCodes());
        return ResponseEntity.ok().build();
    }

    public record UpdateRoleRequest(
            @jakarta.validation.constraints.Size(max = 200)
            String name,
            @jakarta.validation.constraints.Size(max = 2000)
            String description) {}

    public record SetPermissionsRequest(
            @jakarta.validation.constraints.NotNull
            List<@jakarta.validation.constraints.Pattern(regexp = "^[a-z][a-z0-9_]*:[a-z][a-z0-9_]*$") String> permissionCodes) {}
}
