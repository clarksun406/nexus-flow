package com.nexusflow.api.auth;

import com.nexusflow.api.security.UserIdMapper;
import com.nexusflow.common.ErrorCode;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.domain.merchant.MerchantProfile;
import com.nexusflow.domain.merchant.MerchantProfileRepository;
import com.nexusflow.domain.merchant.MerchantUserMembership;
import com.nexusflow.domain.merchant.MerchantUserMembershipRepository;
import com.nexusflow.infra.persistence.MerchantUserEntity;
import com.nexusflow.infra.persistence.SpringDataMerchantUserRepository;
import com.nexusflow.permission.client.PermissionClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String SESSION_USER_ID = "nexusflow.auth.userId";
    public static final String SESSION_EMAIL = "nexusflow.auth.email";
    public static final String SESSION_ACTIVE_MERCHANT_ID = "nexusflow.auth.activeMerchantId";

    private final SpringDataMerchantUserRepository userRepo;
    private final MerchantUserMembershipRepository membershipRepo;
    private final MerchantProfileRepository profileRepo;
    private final PermissionClient permissionClient;

    @Transactional
    public UserInfoResponse login(LoginRequest request, HttpSession session) {
        MerchantUserEntity user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new NexusFlowException(ErrorCode.UNAUTHORIZED, "Invalid email or password"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new NexusFlowException(ErrorCode.UNAUTHORIZED, "Account is not active");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new NexusFlowException(ErrorCode.UNAUTHORIZED, "Password login not configured for this account");
        }

        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new NexusFlowException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }

        // Update last login timestamp
        user.setLastLoginAt(Instant.now());
        userRepo.save(user);

        // Populate session
        session.setAttribute(SESSION_USER_ID, user.getUserId());
        session.setAttribute(SESSION_EMAIL, user.getEmail());
        selectDefaultActiveMerchant(user.getUserId(), session);

        log.info("User '{}' logged in", user.getEmail());

        return buildUserInfo(user.getUserId(), currentActiveMerchantId(session));
    }

    public UserInfoResponse me(String userId, HttpSession session) {
        if (currentActiveMerchantId(session).isEmpty()) {
            selectDefaultActiveMerchant(userId, session);
        }
        return buildUserInfo(userId, currentActiveMerchantId(session));
    }

    public void switchActiveMerchant(String userId, String merchantId, HttpSession session) {
        boolean allowed = membershipRepo.findByUserId(userId).stream()
                .anyMatch(membership -> "ACTIVE".equals(membership.getStatus())
                        && merchantId.equals(membership.getMerchantId()));
        if (!allowed) {
            throw new NexusFlowException(ErrorCode.UNAUTHORIZED, "Merchant is not assigned to this user");
        }
        session.setAttribute(SESSION_ACTIVE_MERCHANT_ID, merchantId);
    }

    private void selectDefaultActiveMerchant(String userId, HttpSession session) {
        membershipRepo.findByUserId(userId).stream()
                .filter(membership -> "ACTIVE".equals(membership.getStatus()))
                .map(MerchantUserMembership::getMerchantId)
                .findFirst()
                .ifPresentOrElse(
                        merchantId -> session.setAttribute(SESSION_ACTIVE_MERCHANT_ID, merchantId),
                        () -> session.removeAttribute(SESSION_ACTIVE_MERCHANT_ID));
    }

    private Optional<String> currentActiveMerchantId(HttpSession session) {
        Object value = session.getAttribute(SESSION_ACTIVE_MERCHANT_ID);
        if (value instanceof String merchantId && !merchantId.isBlank()) {
            return Optional.of(merchantId);
        }
        return Optional.empty();
    }

    private UserInfoResponse buildUserInfo(String userId, Optional<String> activeMerchantId) {
        MerchantUserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new NexusFlowException(ErrorCode.UNAUTHORIZED, "User not found"));

        List<MerchantUserMembership> memberships = membershipRepo.findByUserId(userId);
        List<MembershipDto> membershipDtos = new ArrayList<>();

        for (MerchantUserMembership membership : memberships) {
            if (!"ACTIVE".equals(membership.getStatus())) {
                continue;
            }

            String merchantCode = "";
            String displayName = "";
            MerchantProfile profile = profileRepo.findById(membership.getMerchantId()).orElse(null);
            if (profile != null) {
                merchantCode = profile.getMerchantCode() != null ? profile.getMerchantCode() : "";
                displayName = profile.getDisplayName() != null ? profile.getDisplayName() : "";
            }

            Set<String> permissions = Set.of();
            if (permissionClient.isEnabled()) {
                try {
                    UUID scopeId = UUID.fromString(membership.getMerchantId());
                    UUID uid = UUID.fromString(userId);
                    permissions = permissionClient.getEffectivePermissions(uid, "MERCHANT", scopeId);
                } catch (IllegalArgumentException e) {
                    log.warn("Cannot load permissions for user '{}' merchant '{}': non-UUID ids",
                            userId, membership.getMerchantId());
                }
            }

            membershipDtos.add(MembershipDto.builder()
                    .merchantId(membership.getMerchantId())
                    .merchantCode(merchantCode)
                    .displayName(displayName)
                    .roleCode(membership.getRoleCode())
                    .permissions(permissions)
                    .build());
        }

        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .activeMerchantId(activeMerchantId.orElse(null))
                .memberships(membershipDtos)
                .build();
    }
}
