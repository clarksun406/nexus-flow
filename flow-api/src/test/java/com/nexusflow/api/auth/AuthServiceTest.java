package com.nexusflow.api.auth;

import com.nexusflow.common.NexusFlowException;
import com.nexusflow.domain.merchant.MerchantProfile;
import com.nexusflow.domain.merchant.MerchantProfileRepository;
import com.nexusflow.domain.merchant.MerchantStatus;
import com.nexusflow.domain.merchant.MerchantUserMembership;
import com.nexusflow.domain.merchant.MerchantUserMembershipRepository;
import com.nexusflow.infra.persistence.MerchantUserEntity;
import com.nexusflow.infra.persistence.SpringDataMerchantUserRepository;
import com.nexusflow.permission.client.PermissionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private SpringDataMerchantUserRepository userRepo;
    private MerchantUserMembershipRepository membershipRepo;
    private MerchantProfileRepository profileRepo;
    private PermissionClient permissionClient;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepo = mock(SpringDataMerchantUserRepository.class);
        membershipRepo = mock(MerchantUserMembershipRepository.class);
        profileRepo = mock(MerchantProfileRepository.class);
        permissionClient = mock(PermissionClient.class);
        authService = new AuthService(userRepo, membershipRepo, profileRepo, permissionClient);
        when(permissionClient.isEnabled()).thenReturn(false);
    }

    @Test
    void loginSetsDefaultActiveMerchantInSession() {
        String userId = UUID.randomUUID().toString();
        String merchantId = "550e8400-e29b-41d4-a716-446655440000";
        MerchantUserEntity user = activeUser(userId, "merchant@example.com");
        when(userRepo.findByEmail("merchant@example.com")).thenReturn(Optional.of(user));
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(membershipRepo.findByUserId(userId)).thenReturn(List.of(activeMembership(userId, merchantId)));
        when(profileRepo.findById(merchantId)).thenReturn(Optional.of(MerchantProfile.builder()
                .merchantId(merchantId)
                .merchantCode("merchant-1")
                .displayName("Merchant One")
                .status(MerchantStatus.ACTIVE)
                .build()));

        MockHttpSession session = new MockHttpSession();
        LoginRequest request = new LoginRequest();
        request.setEmail("merchant@example.com");
        request.setPassword("secret-123");
        UserInfoResponse response = authService.login(request, session);

        assertThat(session.getAttribute(AuthService.SESSION_USER_ID)).isEqualTo(userId);
        assertThat(session.getAttribute(AuthService.SESSION_ACTIVE_MERCHANT_ID)).isEqualTo(merchantId);
        assertThat(response.getActiveMerchantId()).isEqualTo(merchantId);
        assertThat(response.getMemberships()).hasSize(1);
    }

    @Test
    void meBackfillsDefaultActiveMerchantWhenSessionMissingIt() {
        String userId = UUID.randomUUID().toString();
        String merchantId = "550e8400-e29b-41d4-a716-446655440000";
        MerchantUserEntity user = activeUser(userId, "merchant@example.com");
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(membershipRepo.findByUserId(userId)).thenReturn(List.of(activeMembership(userId, merchantId)));
        when(profileRepo.findById(merchantId)).thenReturn(Optional.of(MerchantProfile.builder()
                .merchantId(merchantId)
                .merchantCode("merchant-1")
                .displayName("Merchant One")
                .status(MerchantStatus.ACTIVE)
                .build()));

        MockHttpSession session = new MockHttpSession();
        UserInfoResponse response = authService.me(userId, session);

        assertThat(session.getAttribute(AuthService.SESSION_ACTIVE_MERCHANT_ID)).isEqualTo(merchantId);
        assertThat(response.getActiveMerchantId()).isEqualTo(merchantId);
    }

    @Test
    void switchActiveMerchantRejectsMerchantOutsideMemberships() {
        String userId = UUID.randomUUID().toString();
        when(membershipRepo.findByUserId(userId)).thenReturn(List.of(activeMembership(userId,
                "550e8400-e29b-41d4-a716-446655440000")));

        assertThatThrownBy(() -> authService.switchActiveMerchant(userId,
                "550e8400-e29b-41d4-a716-446655440001", new MockHttpSession()))
                .isInstanceOf(NexusFlowException.class)
                .hasMessageContaining("Merchant is not assigned");
    }

    @Test
    void switchActiveMerchantUpdatesSessionWhenMembershipExists() {
        String userId = UUID.randomUUID().toString();
        String merchantId = "550e8400-e29b-41d4-a716-446655440000";
        when(membershipRepo.findByUserId(userId)).thenReturn(List.of(activeMembership(userId, merchantId)));

        MockHttpSession session = new MockHttpSession();
        authService.switchActiveMerchant(userId, merchantId, session);

        assertThat(session.getAttribute(AuthService.SESSION_ACTIVE_MERCHANT_ID)).isEqualTo(merchantId);
    }

    private MerchantUserEntity activeUser(String userId, String email) {
        MerchantUserEntity user = new MerchantUserEntity();
        user.setUserId(userId);
        user.setEmail(email);
        user.setDisplayName("Merchant User");
        user.setStatus("ACTIVE");
        user.setPasswordHash(BCrypt.hashpw("secret-123", BCrypt.gensalt()));
        return user;
    }

    private MerchantUserMembership activeMembership(String userId, String merchantId) {
        return MerchantUserMembership.builder()
                .userId(userId)
                .merchantId(merchantId)
                .roleCode("MERCHANT_OWNER")
                .status("ACTIVE")
                .build();
    }
}
