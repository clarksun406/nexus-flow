package com.nexusflow.permission.client;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class RoleCodesTest {

    private static final Pattern ROLE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    @Test
    void allRoleCodesAreStableAndUnique() {
        assertThat(RoleCodes.all())
                .hasSize(8)
                .allMatch(code -> ROLE_PATTERN.matcher(code).matches());
    }

    @Test
    void separatesSystemAndMerchantRoleTemplates() {
        assertThat(RoleCodes.systemRoles())
                .containsExactlyInAnyOrder(
                        RoleCodes.SYSTEM_ADMIN,
                        RoleCodes.OPS_ADMIN,
                        RoleCodes.OPS_SUPPORT
                );
        assertThat(RoleCodes.merchantRoles())
                .containsExactlyInAnyOrder(
                        RoleCodes.MERCHANT_OWNER,
                        RoleCodes.MERCHANT_DEVELOPER,
                        RoleCodes.MERCHANT_FINANCE,
                        RoleCodes.MERCHANT_SUPPORT,
                        RoleCodes.MERCHANT_VIEWER
                );
    }
}
