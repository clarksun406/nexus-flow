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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Seeds demo data when running locally with the {@code h2} profile
 * (see {@code application-h2.yml}).
 *
 * <p>Idempotent: every entity is only created when missing, so the runner can
 * safely execute on every startup and self-heal partially seeded databases.
 *
 * <p>Seeded artifacts:
 * <ul>
 *   <li>demo merchant profile</li>
 *   <li>merchant API key usable as {@code X-API-Key} header on merchant endpoints</li>
 *   <li>portal login user + merchant membership for session auth ({@code POST /auth/login})</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("h2")
@RequiredArgsConstructor
public class LocalSeedDataRunner {

    public static final String DEMO_MERCHANT_ID = "11111111-1111-4111-8111-111111111111";
    public static final String DEMO_MERCHANT_CODE = "DEMO-001";
    public static final String DEMO_MERCHANT_NAME = "Demo Merchant (local)";
    public static final String DEMO_CREDENTIAL_ID = "local-demo-credential-001";
    public static final String DEMO_API_KEY = "nexusflow-local-api-key";
    public static final String DEMO_USER_ID = "22222222-2222-4222-8222-222222222222";
    public static final String DEMO_EMAIL = "demo@nexusflow.local";
    public static final String DEMO_PASSWORD = "demo1234";

    private final MerchantProfileRepository merchantProfileRepository;
    private final SpringDataMerchantCredentialRepository merchantCredentialRepository;
    private final SpringDataMerchantUserRepository merchantUserRepository;
    private final MerchantUserMembershipRepository membershipRepository;
    private final ApiKeyHasher apiKeyHasher;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDemoData() {
        seedMerchantProfile();
        seedApiKey();
        seedPortalUser();
        logStartupBanner();
    }

    private void seedMerchantProfile() {
        if (merchantProfileRepository.findById(DEMO_MERCHANT_ID).isPresent()) {
            log.debug("Demo merchant profile already seeded");
            return;
        }
        Instant now = Instant.now();
        merchantProfileRepository.save(MerchantProfile.builder()
                .merchantId(DEMO_MERCHANT_ID)
                .merchantCode(DEMO_MERCHANT_CODE)
                .displayName(DEMO_MERCHANT_NAME)
                .status(MerchantStatus.ACTIVE)
                .createTime(now)
                .updateTime(now)
                .build());
        log.info("Seeded demo merchant profile '{}'", DEMO_MERCHANT_ID);
    }

    private void seedApiKey() {
        if (merchantCredentialRepository.findById(DEMO_CREDENTIAL_ID).isPresent()) {
            log.debug("Demo merchant API key already seeded");
            return;
        }
        Instant now = Instant.now();
        MerchantCredentialEntity credential = new MerchantCredentialEntity();
        credential.setCredentialId(DEMO_CREDENTIAL_ID);
        credential.setMerchantId(DEMO_MERCHANT_ID);
        credential.setKeyHash(apiKeyHasher.hash(DEMO_API_KEY));
        credential.setKeyPrefix(DEMO_API_KEY.substring(0, 8));
        credential.setActive(true);
        credential.setCreateTime(now);
        credential.setUpdateTime(now);
        merchantCredentialRepository.save(credential);
        log.info("Seeded demo merchant API key (credential '{}')", DEMO_CREDENTIAL_ID);
    }

    private void seedPortalUser() {
        if (merchantUserRepository.findByEmail(DEMO_EMAIL).isPresent()) {
            log.debug("Demo portal user already seeded");
            return;
        }
        Instant now = Instant.now();
        MerchantUserEntity user = new MerchantUserEntity();
        user.setUserId(DEMO_USER_ID);
        user.setEmail(DEMO_EMAIL);
        user.setPasswordHash(BCrypt.hashpw(DEMO_PASSWORD, BCrypt.gensalt()));
        user.setDisplayName("Demo Merchant Admin");
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        merchantUserRepository.save(user);
        log.info("Seeded demo portal user '{}'", DEMO_EMAIL);

        if (membershipRepository.findByUserId(DEMO_USER_ID).stream()
                .noneMatch(membership -> DEMO_MERCHANT_ID.equals(membership.getMerchantId()))) {
            membershipRepository.save(MerchantUserMembership.builder()
                    .merchantId(DEMO_MERCHANT_ID)
                    .userId(DEMO_USER_ID)
                    .roleCode(RoleCodes.MERCHANT_OWNER)
                    .status("ACTIVE")
                    .createdAt(now)
                    .build());
            log.info("Seeded demo merchant membership for user '{}'", DEMO_EMAIL);
        }
    }

    private void logStartupBanner() {
        log.info("""

                        ============================================================
                        Local 'h2' profile demo data
                        ============================================================
                        Merchant API key : %s   (X-API-Key header)
                        Merchant id      : %s
                        Portal login     : %s / %s
                        H2 console       : http://localhost:8080/h2-console
                        ============================================================
                        """.formatted(DEMO_API_KEY, DEMO_MERCHANT_ID, DEMO_EMAIL, DEMO_PASSWORD));
    }
}
