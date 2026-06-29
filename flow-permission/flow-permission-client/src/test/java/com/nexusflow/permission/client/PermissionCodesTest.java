package com.nexusflow.permission.client;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionCodesTest {

    private static final Pattern CODE_PATTERN =
            Pattern.compile("^[a-z][a-z0-9_]*:[a-z][a-z0-9_]*$");

    @Test
    void allPermissionCodesAreNamespacedAndUnique() {
        assertThat(PermissionCodes.all())
                .hasSize(52)
                .allMatch(code -> CODE_PATTERN.matcher(code).matches());
    }

    @Test
    void includesCurrentFlowApiPermissions() {
        assertThat(PermissionCodes.all())
                .contains(
                        PermissionCodes.PaymentOrder.CREATE,
                        PermissionCodes.PaymentOrder.READ,
                        PermissionCodes.Refund.CREATE,
                        PermissionCodes.FiatRamp.QUOTE,
                        PermissionCodes.FiatRamp.CREATE,
                        PermissionCodes.CryptoPayment.CONFIRM,
                        PermissionCodes.Orphan.COMPENSATE,
                        PermissionCodes.WebhookDeadLetter.REPLAY,
                        PermissionCodes.OpsDashboard.READ
                );
    }
}
