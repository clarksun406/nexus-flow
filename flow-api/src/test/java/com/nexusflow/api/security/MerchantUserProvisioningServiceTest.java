package com.nexusflow.api.security;

import com.nexusflow.domain.merchant.MerchantProfile;
import com.nexusflow.domain.merchant.MerchantProfileRepository;
import com.nexusflow.domain.merchant.MerchantStatus;
import com.nexusflow.permission.client.PermissionClient;
import com.nexusflow.permission.client.RoleCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MerchantUserProvisioningServiceTest {

    private PermissionClient permissionClient;
    private MerchantProfileRepository merchantProfileRepository;
    private MerchantUserProvisioningService service;

    @BeforeEach
    void setUp() {
        permissionClient = mock(PermissionClient.class);
        merchantProfileRepository = mock(MerchantProfileRepository.class);
        service = new MerchantUserProvisioningService(permissionClient, merchantProfileRepository);
    }

    @Test
    void provisionsOpsUserWhenNotAlreadyProvisioned() {
        when(permissionClient.isEnabled()).thenReturn(true);
        when(permissionClient.hasRoles(UserIdMapper.OPS_USER_ID, "SYSTEM")).thenReturn(false);
        UUID opsAdminRoleId = UUID.randomUUID();
        when(permissionClient.getRoleByCode(RoleCodes.OPS_ADMIN)).thenReturn(Optional.of(opsAdminRoleId));
        when(permissionClient.grantRole(eq(UserIdMapper.OPS_USER_ID), eq(opsAdminRoleId), eq("SYSTEM"), isNull()))
                .thenReturn(true);
        when(merchantProfileRepository.findAll()).thenReturn(List.of());

        service.provisionOnStartup();

        verify(permissionClient).grantRole(UserIdMapper.OPS_USER_ID, opsAdminRoleId, "SYSTEM", null);
    }

    @Test
    void skipsOpsUserWhenAlreadyProvisioned() {
        when(permissionClient.isEnabled()).thenReturn(true);
        when(permissionClient.hasRoles(UserIdMapper.OPS_USER_ID, "SYSTEM")).thenReturn(true);
        when(merchantProfileRepository.findAll()).thenReturn(List.of());

        service.provisionOnStartup();

        verify(permissionClient, never()).grantRole(any(), any(), any(), any());
    }

    @Test
    void provisionsMerchantUsersWithOwnerRole() {
        when(permissionClient.isEnabled()).thenReturn(true);
        when(permissionClient.hasRoles(UserIdMapper.OPS_USER_ID, "SYSTEM")).thenReturn(true);

        MerchantProfile profile = MerchantProfile.builder()
                .merchantId("550e8400-e29b-41d4-a716-446655440000")
                .merchantCode("merchant-1")
                .displayName("Merchant One")
                .status(MerchantStatus.ACTIVE)
                .build();
        when(merchantProfileRepository.findAll()).thenReturn(List.of(profile));

        UUID merchantOwnerRoleId = UUID.randomUUID();
        when(permissionClient.getRoleByCode(RoleCodes.MERCHANT_OWNER)).thenReturn(Optional.of(merchantOwnerRoleId));

        UUID expectedUserId = UserIdMapper.toUuid("550e8400-e29b-41d4-a716-446655440000");
        when(permissionClient.hasRoles(expectedUserId, "MERCHANT")).thenReturn(false);
        when(permissionClient.grantRole(eq(expectedUserId), eq(merchantOwnerRoleId), eq("MERCHANT"), any()))
                .thenReturn(true);

        service.provisionOnStartup();

        verify(permissionClient).grantRole(eq(expectedUserId), eq(merchantOwnerRoleId), eq("MERCHANT"),
                eq(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")));
    }

    @Test
    void handlesPermissionServerUnavailable() {
        when(permissionClient.isEnabled()).thenReturn(true);
        when(permissionClient.hasRoles(any(), any())).thenThrow(new RuntimeException("Connection refused"));
        when(merchantProfileRepository.findAll()).thenReturn(List.of());

        service.provisionOnStartup(); // should not throw
    }

    @Test
    void skipsWhenPermissionClientDisabled() {
        when(permissionClient.isEnabled()).thenReturn(false);

        service.provisionOnStartup();

        verify(permissionClient, never()).hasRoles(any(), any());
        verify(merchantProfileRepository, never()).findAll();
    }

    @Test
    void skipsMerchantWithNonUuidMerchantId() {
        when(permissionClient.isEnabled()).thenReturn(true);
        when(permissionClient.hasRoles(UserIdMapper.OPS_USER_ID, "SYSTEM")).thenReturn(true);

        MerchantProfile profile = MerchantProfile.builder()
                .merchantId("merchant-1") // not a UUID
                .merchantCode("m-1")
                .displayName("Non UUID Merchant")
                .status(MerchantStatus.ACTIVE)
                .build();
        when(merchantProfileRepository.findAll()).thenReturn(List.of(profile));
        when(permissionClient.getRoleByCode(RoleCodes.MERCHANT_OWNER)).thenReturn(Optional.of(UUID.randomUUID()));

        service.provisionOnStartup(); // should not throw, should skip this merchant

        verify(permissionClient, never()).grantRole(any(), any(), eq("MERCHANT"), any());
    }
}
