package com.nexusflow.api.security;

import com.nexusflow.domain.merchant.MerchantProfile;
import com.nexusflow.domain.merchant.MerchantProfileRepository;
import com.nexusflow.permission.client.PermissionClient;
import com.nexusflow.permission.client.RoleCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Seeds service-account user roles in the permission server so that
 * @CheckPermission can resolve permissions for API-key-based auth.
 *
 * <p>Runs once on startup. Idempotent — skips users that already have roles.
 * Gracefully handles permission-server-unavailable (logs warning, does not block startup).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantUserProvisioningService {

    private final PermissionClient permissionClient;
    private final MerchantProfileRepository merchantProfileRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void provisionOnStartup() {
        provisionOpsUser();
        provisionMerchantUsers();
    }

    private void provisionOpsUser() {
        if (!permissionClient.isEnabled()) {
            return;
        }
        try {
            if (permissionClient.hasRoles(UserIdMapper.OPS_USER_ID, "SYSTEM")) {
                log.debug("Ops user already provisioned");
                return;
            }
            Optional<UUID> roleId = permissionClient.getRoleByCode(RoleCodes.OPS_ADMIN);
            if (roleId.isEmpty()) {
                log.warn("OPS_ADMIN role not found in permission server — skipping ops user provisioning");
                return;
            }
            boolean ok = permissionClient.grantRole(UserIdMapper.OPS_USER_ID, roleId.get(), "SYSTEM", null);
            if (ok) {
                log.info("Provisioned ops user with OPS_ADMIN role in SYSTEM scope");
            } else {
                log.warn("Failed to provision ops user — permission server may be unavailable");
            }
        } catch (Exception e) {
            log.warn("Ops user provisioning failed (permission server may be down): {}", e.getMessage());
        }
    }

    private void provisionMerchantUsers() {
        if (!permissionClient.isEnabled()) {
            return;
        }
        Optional<UUID> merchantOwnerRoleId = permissionClient.getRoleByCode(RoleCodes.MERCHANT_OWNER);
        if (merchantOwnerRoleId.isEmpty()) {
            log.warn("MERCHANT_OWNER role not found in permission server — skipping merchant user provisioning");
            return;
        }

        for (MerchantProfile profile : merchantProfileRepository.findAll()) {
            try {
                UUID userId = UserIdMapper.toUuid(profile.getMerchantId());
                // merchantId must be UUID format for MERCHANT scope to work.
                // If not a valid UUID, skip provisioning for this merchant.
                UUID scopeId;
                try {
                    scopeId = UUID.fromString(profile.getMerchantId());
                } catch (IllegalArgumentException e) {
                    log.warn("Merchant '{}' has non-UUID merchantId — skipping permission provisioning. "
                            + "MerchantId must be UUID format for RBAC to work.", profile.getMerchantId());
                    continue;
                }
                if (permissionClient.hasRoles(userId, "MERCHANT")) {
                    continue;
                }
                boolean ok = permissionClient.grantRole(userId, merchantOwnerRoleId.get(), "MERCHANT", scopeId);
                if (ok) {
                    log.info("Provisioned merchant '{}' with MERCHANT_OWNER role", profile.getMerchantId());
                } else {
                    log.warn("Failed to provision merchant '{}'", profile.getMerchantId());
                }
            } catch (Exception e) {
                log.warn("Merchant provisioning failed for '{}': {}", profile.getMerchantId(), e.getMessage());
            }
        }
    }
}
