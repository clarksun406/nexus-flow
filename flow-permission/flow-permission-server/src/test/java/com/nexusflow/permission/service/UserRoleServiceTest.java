package com.nexusflow.permission.service;

import com.nexusflow.permission.dto.GrantRoleRequest;
import com.nexusflow.permission.entity.UserRole;
import com.nexusflow.permission.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    private UserRoleService userRoleService;

    @BeforeEach
    void setUp() {
        userRoleService = new UserRoleService(userRoleRepository);
    }

    @Test
    void getUserRolesByUserId() {
        UUID userId = UUID.randomUUID();
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(new UserRole(), new UserRole()));

        assertThat(userRoleService.getUserRoles(userId)).hasSize(2);
    }

    @Test
    void getUserRolesByUserIdAndScopeType() {
        UUID userId = UUID.randomUUID();
        when(userRoleRepository.findByUserIdAndScopeType(userId, "SYSTEM"))
                .thenReturn(List.of(new UserRole()));

        assertThat(userRoleService.getUserRoles(userId, "SYSTEM")).hasSize(1);
    }

    @Test
    void grantRoleSuccess() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        GrantRoleRequest request = new GrantRoleRequest(userId, roleId, "MERCHANT", null, null);
        when(userRoleRepository.existsByUserIdAndRoleIdAndScopeTypeAndScopeId(userId, roleId, "MERCHANT", null))
                .thenReturn(false);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> {
            UserRole ur = inv.getArgument(0);
            ur.setId(UUID.randomUUID());
            return ur;
        });

        UserRole result = userRoleService.grantRole(request);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getRoleId()).isEqualTo(roleId);
        assertThat(result.getScopeType()).isEqualTo("MERCHANT");
    }

    @Test
    void grantRoleDuplicateIsIdempotent() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        GrantRoleRequest request = new GrantRoleRequest(userId, roleId, "MERCHANT", null, null);
        UserRole existing = new UserRole();
        existing.setUserId(userId);
        existing.setRoleId(roleId);

        when(userRoleRepository.existsByUserIdAndRoleIdAndScopeTypeAndScopeId(userId, roleId, "MERCHANT", null))
                .thenReturn(true);
        when(userRoleRepository.findByUserIdAndRoleIdAndScopeTypeAndScopeId(userId, roleId, "MERCHANT", null))
                .thenReturn(Optional.of(existing));

        UserRole result = userRoleService.grantRole(request);

        assertThat(result).isSameAs(existing);
    }

    @Test
    void grantRoleDefaultsScopeTypeToMerchant() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        GrantRoleRequest request = new GrantRoleRequest(userId, roleId, null, null, null);
        when(userRoleRepository.existsByUserIdAndRoleIdAndScopeTypeAndScopeId(userId, roleId, "MERCHANT", null))
                .thenReturn(false);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> {
            UserRole ur = inv.getArgument(0);
            ur.setId(UUID.randomUUID());
            return ur;
        });

        UserRole result = userRoleService.grantRole(request);

        assertThat(result.getScopeType()).isEqualTo("MERCHANT");
    }

    @Test
    void revokeRole() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        userRoleService.revokeRole(userId, roleId, "MERCHANT", null);

        verify(userRoleRepository).deleteByUserIdAndRoleIdAndScopeTypeAndScopeId(userId, roleId, "MERCHANT", null);
    }

    @Test
    void revokeRoleDefaultsScopeType() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        userRoleService.revokeRole(userId, roleId, null, null);

        verify(userRoleRepository).deleteByUserIdAndRoleIdAndScopeTypeAndScopeId(userId, roleId, "MERCHANT", null);
    }

    @Test
    void setUserRolesReplacesExisting() {
        UUID userId = UUID.randomUUID();
        UUID roleId1 = UUID.randomUUID();
        UUID roleId2 = UUID.randomUUID();
        UserRole oldUr = new UserRole();
        oldUr.setUserId(userId);
        oldUr.setRoleId(roleId1);
        oldUr.setScopeType("MERCHANT");
        oldUr.setScopeId(null);

        when(userRoleRepository.findByUserIdAndScopeType(userId, "MERCHANT"))
                .thenReturn(List.of(oldUr));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));

        userRoleService.setUserRoles(userId, "MERCHANT", null, List.of(roleId2), null);

        verify(userRoleRepository).delete(oldUr);
        verify(userRoleRepository).save(any(UserRole.class));
    }
}
