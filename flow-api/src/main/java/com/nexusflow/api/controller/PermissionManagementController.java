package com.nexusflow.api.controller;

import com.nexusflow.permission.client.CheckPermission;
import com.nexusflow.permission.client.PermissionClientProperties;
import com.nexusflow.permission.client.PermissionCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * Proxy controller for permission server admin APIs.
 * Forwards requests to flow-permission-server using the service token.
 * All endpoints require SYSTEM scope permission.
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class PermissionManagementController {

    private final PermissionClientProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    // ── Roles ──

    @GetMapping("/roles")
    @CheckPermission(value = PermissionCodes.PermissionAdmin.ROLE_READ, scopeType = "SYSTEM")
    public ResponseEntity<String> listRoles(@RequestParam(required = false) String scope) {
        String url = properties.getServerUrl() + "/api/v1/role/list";
        if (scope != null) url += "?scope=" + scope;
        return proxyGet(url);
    }

    @GetMapping("/roles/{id}")
    @CheckPermission(value = PermissionCodes.PermissionAdmin.ROLE_READ, scopeType = "SYSTEM")
    public ResponseEntity<String> getRole(@PathVariable String id) {
        return proxyGet(properties.getServerUrl() + "/api/v1/role/" + id);
    }

    @PostMapping("/roles")
    @CheckPermission(value = PermissionCodes.PermissionAdmin.ROLE_MANAGE, scopeType = "SYSTEM")
    public ResponseEntity<String> createRole(@RequestBody String body) {
        return proxyPost(properties.getServerUrl() + "/api/v1/role", body);
    }

    @PutMapping("/roles/{id}")
    @CheckPermission(value = PermissionCodes.PermissionAdmin.ROLE_MANAGE, scopeType = "SYSTEM")
    public ResponseEntity<String> updateRole(@PathVariable String id, @RequestBody String body) {
        return proxyPut(properties.getServerUrl() + "/api/v1/role/" + id, body);
    }

    @DeleteMapping("/roles/{id}")
    @CheckPermission(value = PermissionCodes.PermissionAdmin.ROLE_MANAGE, scopeType = "SYSTEM")
    public ResponseEntity<Void> deleteRole(@PathVariable String id) {
        proxyDelete(properties.getServerUrl() + "/api/v1/role/" + id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/roles/{id}/permissions")
    @CheckPermission(value = PermissionCodes.PermissionAdmin.ROLE_MANAGE, scopeType = "SYSTEM")
    public ResponseEntity<String> setRolePermissions(@PathVariable String id, @RequestBody String body) {
        return proxyPut(properties.getServerUrl() + "/api/v1/role/" + id + "/permissions", body);
    }

    // ── Users ──

    @GetMapping("/users/{userId}/roles")
    @CheckPermission(value = PermissionCodes.PermissionAdmin.USER_ROLE_READ, scopeType = "SYSTEM")
    public ResponseEntity<String> getUserRoles(@PathVariable String userId,
                                               @RequestParam(required = false) String scopeType) {
        String url = properties.getServerUrl() + "/api/v1/user/" + userId + "/roles";
        if (scopeType != null) url += "?scopeType=" + scopeType;
        return proxyGet(url);
    }

    @PostMapping("/users/grant-role")
    @CheckPermission(value = PermissionCodes.PermissionAdmin.USER_ROLE_GRANT, scopeType = "SYSTEM")
    public ResponseEntity<String> grantRole(@RequestBody String body) {
        return proxyPost(properties.getServerUrl() + "/api/v1/user/grant-role", body);
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @CheckPermission(value = PermissionCodes.PermissionAdmin.USER_ROLE_REVOKE, scopeType = "SYSTEM")
    public ResponseEntity<Void> revokeRole(@PathVariable String userId,
                                           @PathVariable String roleId,
                                           @RequestParam(defaultValue = "MERCHANT") String scopeType,
                                           @RequestParam(required = false) String scopeId) {
        String url = properties.getServerUrl() + "/api/v1/user/" + userId + "/roles/" + roleId
                + "?scopeType=" + scopeType;
        if (scopeId != null) url += "&scopeId=" + scopeId;
        proxyDelete(url);
        return ResponseEntity.noContent().build();
    }

    // ── Permissions ──

    @GetMapping("/permissions")
    @CheckPermission(value = PermissionCodes.PermissionAdmin.ROLE_READ, scopeType = "SYSTEM")
    public ResponseEntity<String> listPermissions(@RequestParam(required = false) String scope) {
        String url = properties.getServerUrl() + "/api/v1/permission/list";
        if (scope != null) url += "?scope=" + scope;
        return proxyGet(url);
    }

    // ── Proxy helpers ──

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getServiceToken());
        return headers;
    }

    private ResponseEntity<String> proxyGet(String url) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            log.error("Proxy GET {} failed: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"permission server unavailable\"}");
        }
    }

    private ResponseEntity<String> proxyPost(String url, String body) {
        try {
            HttpEntity<String> entity = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            log.error("Proxy POST {} failed: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"permission server unavailable\"}");
        }
    }

    private ResponseEntity<String> proxyPut(String url, String body) {
        try {
            HttpEntity<String> entity = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            log.error("Proxy PUT {} failed: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"permission server unavailable\"}");
        }
    }

    private void proxyDelete(String url) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
        } catch (Exception e) {
            log.error("Proxy DELETE {} failed: {}", url, e.getMessage());
        }
    }
}
