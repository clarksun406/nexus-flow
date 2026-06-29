package com.nexusflow.permission.client;

import java.util.Set;

public final class PermissionCodes {

    private PermissionCodes() {
    }

    public static final class Merchant {
        public static final String READ = "merchant:read";
        public static final String CREATE = "merchant:create";
        public static final String UPDATE = "merchant:update";
        public static final String ACTIVATE = "merchant:activate";
        public static final String SUSPEND = "merchant:suspend";

        private Merchant() {
        }
    }

    public static final class MerchantUser {
        public static final String READ = "merchant_user:read";
        public static final String INVITE = "merchant_user:invite";
        public static final String UPDATE = "merchant_user:update";
        public static final String DISABLE = "merchant_user:disable";

        private MerchantUser() {
        }
    }

    public static final class ApiKey {
        public static final String READ = "api_key:read";
        public static final String CREATE = "api_key:create";
        public static final String ROTATE = "api_key:rotate";
        public static final String REVOKE = "api_key:revoke";

        private ApiKey() {
        }
    }

    public static final class Webhook {
        public static final String READ = "webhook:read";
        public static final String UPDATE = "webhook:update";
        public static final String TEST = "webhook:test";
        public static final String REPLAY = "webhook:replay";

        private Webhook() {
        }
    }

    public static final class PaymentOrder {
        public static final String READ = "payment_order:read";
        public static final String CREATE = "payment_order:create";
        public static final String EXPORT = "payment_order:export";

        private PaymentOrder() {
        }
    }

    public static final class CryptoPayment {
        public static final String READ = "crypto_payment:read";
        public static final String CREATE = "crypto_payment:create";
        public static final String CONFIRM = "crypto_payment:confirm";
        public static final String FAIL = "crypto_payment:fail";

        private CryptoPayment() {
        }
    }

    public static final class Refund {
        public static final String READ = "refund:read";
        public static final String CREATE = "refund:create";
        public static final String APPROVE = "refund:approve";
        public static final String RETRY = "refund:retry";

        private Refund() {
        }
    }

    public static final class FiatRamp {
        public static final String QUOTE = "fiat_ramp:quote";
        public static final String CREATE = "fiat_ramp:create";
        public static final String READ = "fiat_ramp:read";
        public static final String OPERATE = "fiat_ramp:operate";

        private FiatRamp() {
        }
    }

    public static final class Orphan {
        public static final String READ = "orphan:read";
        public static final String RESOLVE = "orphan:resolve";
        public static final String IGNORE = "orphan:ignore";
        public static final String COMPENSATE = "orphan:compensate";

        private Orphan() {
        }
    }

    public static final class WebhookDeadLetter {
        public static final String READ = "webhook_dlq:read";
        public static final String REPLAY = "webhook_dlq:replay";
        public static final String IGNORE = "webhook_dlq:ignore";

        private WebhookDeadLetter() {
        }
    }

    public static final class OpsDashboard {
        public static final String READ = "ops_dashboard:read";

        private OpsDashboard() {
        }
    }

    public static final class Provider {
        public static final String READ = "provider:read";
        public static final String UPDATE = "provider:update";
        public static final String DISABLE = "provider:disable";
        public static final String ENABLE = "provider:enable";

        private Provider() {
        }
    }

    public static final class PermissionAdmin {
        public static final String PERMISSION_READ = "permission:read";
        public static final String PERMISSION_MANAGE = "permission:manage";
        public static final String ROLE_READ = "role:read";
        public static final String ROLE_MANAGE = "role:manage";
        public static final String USER_ROLE_READ = "user_role:read";
        public static final String USER_ROLE_GRANT = "user_role:grant";
        public static final String USER_ROLE_REVOKE = "user_role:revoke";

        private PermissionAdmin() {
        }
    }

    public static final class Audit {
        public static final String READ = "audit:read";

        private Audit() {
        }
    }

    public static Set<String> all() {
        return Set.of(
                Merchant.READ, Merchant.CREATE, Merchant.UPDATE, Merchant.ACTIVATE, Merchant.SUSPEND,
                MerchantUser.READ, MerchantUser.INVITE, MerchantUser.UPDATE, MerchantUser.DISABLE,
                ApiKey.READ, ApiKey.CREATE, ApiKey.ROTATE, ApiKey.REVOKE,
                Webhook.READ, Webhook.UPDATE, Webhook.TEST, Webhook.REPLAY,
                PaymentOrder.READ, PaymentOrder.CREATE, PaymentOrder.EXPORT,
                CryptoPayment.READ, CryptoPayment.CREATE, CryptoPayment.CONFIRM, CryptoPayment.FAIL,
                Refund.READ, Refund.CREATE, Refund.APPROVE, Refund.RETRY,
                FiatRamp.QUOTE, FiatRamp.CREATE, FiatRamp.READ, FiatRamp.OPERATE,
                Orphan.READ, Orphan.RESOLVE, Orphan.IGNORE, Orphan.COMPENSATE,
                WebhookDeadLetter.READ, WebhookDeadLetter.REPLAY, WebhookDeadLetter.IGNORE,
                OpsDashboard.READ,
                Provider.READ, Provider.UPDATE, Provider.DISABLE, Provider.ENABLE,
                PermissionAdmin.PERMISSION_READ, PermissionAdmin.PERMISSION_MANAGE,
                PermissionAdmin.ROLE_READ, PermissionAdmin.ROLE_MANAGE,
                PermissionAdmin.USER_ROLE_READ, PermissionAdmin.USER_ROLE_GRANT,
                PermissionAdmin.USER_ROLE_REVOKE,
                Audit.READ
        );
    }
}
