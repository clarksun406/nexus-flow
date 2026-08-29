package com.nexusflow.api.bootstrap;

import com.nexusflow.api.security.ApiKeyHasher;
import com.nexusflow.domain.merchant.MerchantProfile;
import com.nexusflow.domain.merchant.MerchantProfileRepository;
import com.nexusflow.domain.merchant.MerchantStatus;
import com.nexusflow.domain.merchant.MerchantUserMembership;
import com.nexusflow.domain.merchant.MerchantUserMembershipRepository;
import com.nexusflow.infra.persistence.MerchantCredentialEntity;
import com.nexusflow.infra.persistence.MerchantUserEntity;
import com.nexusflow.infra.persistence.SpringDataMerchantCredentialRepository;
import com.nexusflow.infra.persistence.SpringDataMerchantUserRepository;
import com.nexusflow.permission.client.RoleCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalSeedDataRunnerTest {

    private final ApiKeyHasher apiKeyHasher = new ApiKeyHasher();

    private MerchantProfileRepository profileRepo;
    private SpringDataMerchantCredentialRepository credentialRepo;
    private SpringDataMerchantUserRepository userRepo;
    private MerchantUserMembershipRepository membershipRepo;
    private LocalSeedDataRunner runner;

    @BeforeEach
    void setUp() {
        profileRepo = mock(MerchantProfileRepository.class);
        credentialRepo = mock(SpringDataMerchantCredentialRepository.class);
        userRepo = mock(SpringDataMerchantUserRepository.class);
        membershipRepo = mock(MerchantUserMembershipRepository.class);
        runner = new LocalSeedDataRunner(profileRepo, credentialRepo, userRepo, membershipRepo, apiKeyHasher);
    }

    @Test
    void seedsAllDemoDataWhenDatabaseIsEmpty() {
        when(profileRepo.findById(LocalSeedDataRunner.DEMO_MERCHANT_ID)).thenReturn(Optional.empty());
        when(credentialRepo.findById(LocalSeedDataRunner.DEMO_CREDENTIAL_ID)).thenReturn(Optional.empty());
        when(userRepo.findByEmail(LocalSeedDataRunner.DEMO_EMAIL)).thenReturn(Optional.empty());
        when(membershipRepo.findByUserId(LocalSeedDataRunner.DEMO_USER_ID)).thenReturn(List.of());

        runner.seedDemoData();

        verify(profileRepo).save(any(MerchantProfile.class));

        ArgumentCaptor<MerchantCredentialEntity> credentialCaptor =
                ArgumentCaptor.forClass(MerchantCredentialEntity.class);
        verify(credentialRepo).save(credentialCaptor.capture());
        MerchantCredentialEntity credential = credentialCaptor.getValue();
        assertEquals(LocalSeedDataRunner.DEMO_CREDENTIAL_ID, credential.getCredentialId());
        assertEquals(LocalSeedDataRunner.DEMO_MERCHANT_ID, credential.getMerchantId());
        assertEquals(apiKeyHasher.hash(LocalSeedDataRunner.DEMO_API_KEY), credential.getKeyHash());
        assertTrue(credential.isActive());

        ArgumentCaptor<MerchantUserEntity> userCaptor = ArgumentCaptor.forClass(MerchantUserEntity.class);
        verify(userRepo).save(userCaptor.capture());
        MerchantUserEntity user = userCaptor.getValue();
        assertEquals(LocalSeedDataRunner.DEMO_USER_ID, user.getUserId());
        assertEquals(LocalSeedDataRunner.DEMO_EMAIL, user.getEmail());
        assertTrue(BCrypt.checkpw(LocalSeedDataRunner.DEMO_PASSWORD, user.getPasswordHash()));

        ArgumentCaptor<MerchantUserMembership> membershipCaptor =
                ArgumentCaptor.forClass(MerchantUserMembership.class);
        verify(membershipRepo).save(membershipCaptor.capture());
        MerchantUserMembership membership = membershipCaptor.getValue();
        assertEquals(LocalSeedDataRunner.DEMO_MERCHANT_ID, membership.getMerchantId());
        assertEquals(LocalSeedDataRunner.DEMO_USER_ID, membership.getUserId());
        assertEquals(RoleCodes.MERCHANT_OWNER, membership.getRoleCode());
    }

    @Test
    void isIdempotentWhenDemoDataAlreadyExists() {
        when(profileRepo.findById(LocalSeedDataRunner.DEMO_MERCHANT_ID)).thenReturn(Optional.of(
                MerchantProfile.builder()
                        .merchantId(LocalSeedDataRunner.DEMO_MERCHANT_ID)
                        .merchantCode(LocalSeedDataRunner.DEMO_MERCHANT_CODE)
                        .displayName(LocalSeedDataRunner.DEMO_MERCHANT_NAME)
                        .status(MerchantStatus.ACTIVE)
                        .build()));
        when(credentialRepo.findById(LocalSeedDataRunner.DEMO_CREDENTIAL_ID))
                .thenReturn(Optional.of(new MerchantCredentialEntity()));
        when(userRepo.findByEmail(LocalSeedDataRunner.DEMO_EMAIL))
                .thenReturn(Optional.of(new MerchantUserEntity()));
        when(membershipRepo.findByUserId(LocalSeedDataRunner.DEMO_USER_ID)).thenReturn(List.of(
                MerchantUserMembership.builder()
                        .merchantId(LocalSeedDataRunner.DEMO_MERCHANT_ID)
                        .userId(LocalSeedDataRunner.DEMO_USER_ID)
                        .roleCode(RoleCodes.MERCHANT_OWNER)
                        .status("ACTIVE")
                        .build()));

        runner.seedDemoData();

        verify(profileRepo, never()).save(any());
        verify(credentialRepo, never()).save(any());
        verify(userRepo, never()).save(any());
        verify(membershipRepo, never()).save(any(MerchantUserMembership.class));
    }

    @Test
    void selfHealsPartiallySeededDatabase() {
        // Merchant profile exists, but credential, user and membership are missing.
        when(profileRepo.findById(LocalSeedDataRunner.DEMO_MERCHANT_ID)).thenReturn(Optional.of(
                MerchantProfile.builder()
                        .merchantId(LocalSeedDataRunner.DEMO_MERCHANT_ID)
                        .merchantCode(LocalSeedDataRunner.DEMO_MERCHANT_CODE)
                        .displayName(LocalSeedDataRunner.DEMO_MERCHANT_NAME)
                        .status(MerchantStatus.ACTIVE)
                        .build()));
        when(credentialRepo.findById(LocalSeedDataRunner.DEMO_CREDENTIAL_ID)).thenReturn(Optional.empty());
        when(userRepo.findByEmail(LocalSeedDataRunner.DEMO_EMAIL)).thenReturn(Optional.empty());
        when(membershipRepo.findByUserId(LocalSeedDataRunner.DEMO_USER_ID)).thenReturn(List.of());

        runner.seedDemoData();

        verify(profileRepo, never()).save(any());
        verify(credentialRepo).save(any(MerchantCredentialEntity.class));
        verify(userRepo).save(any(MerchantUserEntity.class));
        verify(membershipRepo).save(any(MerchantUserMembership.class));
    }
}
