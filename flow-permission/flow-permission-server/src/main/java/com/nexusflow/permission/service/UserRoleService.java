package com.nexusflow.permission.service;

import com.nexusflow.permission.dto.GrantRoleRequest;
import com.nexusflow.permission.entity.UserRole;
import com.nexusflow.permission.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public List<UserRole> getUserRoles(UUID userId) {
        return userRoleRepository.findByUserId(userId);
    }

    public List<UserRole> getUserRoles(UUID userId, String scopeType) {
        return userRoleRepository.findByUserIdAndScopeType(userId, scopeType);
    }

    public UserRole grantRole(GrantRoleRequest request) {
        String scopeType = request.scopeType() != null ? request.scopeType() : "MERCHANT";

        if (userRoleRepository.existsByUserIdAndRoleIdAndScopeTypeAndScopeId(
                request.userId(), request.roleId(), scopeType, request.scopeId())) {
            return userRoleRepository.findByUserIdAndRoleIdAndScopeTypeAndScopeId(
                    request.userId(), request.roleId(), scopeType, request.scopeId()).get();
        }

        UserRole ur = new UserRole();
        ur.setUserId(request.userId());
        ur.setRoleId(request.roleId());
        ur.setScopeType(scopeType);
        ur.setScopeId(request.scopeId());
        ur.setGrantedBy(request.grantedBy());
        return userRoleRepository.save(ur);
    }

    public void revokeRole(UUID userId, UUID roleId, String scopeType, UUID scopeId) {
        if (scopeType == null) scopeType = "MERCHANT";
        userRoleRepository.deleteByUserIdAndRoleIdAndScopeTypeAndScopeId(userId, roleId, scopeType, scopeId);
    }

    public void setUserRoles(UUID userId, String scopeType, UUID scopeId, List<UUID> roleIds, UUID grantedBy) {
        List<UserRole> existing = userRoleRepository.findByUserIdAndScopeType(userId, scopeType);
        for (UserRole ur : existing) {
            if (scopeId == null || scopeId.equals(ur.getScopeId())) {
                userRoleRepository.delete(ur);
            }
        }

        for (UUID roleId : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            ur.setScopeType(scopeType);
            ur.setScopeId(scopeId);
            ur.setGrantedBy(grantedBy);
            userRoleRepository.save(ur);
        }
    }
}
