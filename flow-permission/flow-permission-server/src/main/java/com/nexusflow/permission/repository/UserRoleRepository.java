package com.nexusflow.permission.repository;

import com.nexusflow.permission.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserId(UUID userId);

    List<UserRole> findByUserIdAndScopeType(UUID userId, String scopeType);

    Optional<UserRole> findByUserIdAndRoleIdAndScopeTypeAndScopeId(
            UUID userId, UUID roleId, String scopeType, UUID scopeId);

    boolean existsByUserIdAndRoleIdAndScopeTypeAndScopeId(
            UUID userId, UUID roleId, String scopeType, UUID scopeId);

    void deleteByUserIdAndRoleIdAndScopeTypeAndScopeId(
            UUID userId, UUID roleId, String scopeType, UUID scopeId);
}
