package com.nexusflow.permission.repository;

import com.nexusflow.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    List<Permission> findByScope(String scope);

    @Query("""
        SELECT p.code FROM Permission p
        JOIN RolePermission rp ON rp.permissionId = p.id
        JOIN UserRole ur ON ur.roleId = rp.roleId
        WHERE ur.userId = :userId AND ur.scopeType = :scopeType
        AND (ur.scopeId = :scopeId OR (ur.scopeId IS NULL AND :scopeId IS NULL))
    """)
    List<String> findPermissionCodesByUserAndScope(
            @Param("userId") UUID userId,
            @Param("scopeType") String scopeType,
            @Param("scopeId") UUID scopeId);
}
