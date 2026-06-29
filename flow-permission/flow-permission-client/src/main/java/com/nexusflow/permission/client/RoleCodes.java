package com.nexusflow.permission.client;

import java.util.Set;

public final class RoleCodes {

    private RoleCodes() {
    }

    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String OPS_ADMIN = "OPS_ADMIN";
    public static final String OPS_SUPPORT = "OPS_SUPPORT";
    public static final String MERCHANT_OWNER = "MERCHANT_OWNER";
    public static final String MERCHANT_DEVELOPER = "MERCHANT_DEVELOPER";
    public static final String MERCHANT_FINANCE = "MERCHANT_FINANCE";
    public static final String MERCHANT_SUPPORT = "MERCHANT_SUPPORT";
    public static final String MERCHANT_VIEWER = "MERCHANT_VIEWER";

    public static Set<String> all() {
        return Set.of(
                SYSTEM_ADMIN,
                OPS_ADMIN,
                OPS_SUPPORT,
                MERCHANT_OWNER,
                MERCHANT_DEVELOPER,
                MERCHANT_FINANCE,
                MERCHANT_SUPPORT,
                MERCHANT_VIEWER
        );
    }

    public static Set<String> systemRoles() {
        return Set.of(SYSTEM_ADMIN, OPS_ADMIN, OPS_SUPPORT);
    }

    public static Set<String> merchantRoles() {
        return Set.of(
                MERCHANT_OWNER,
                MERCHANT_DEVELOPER,
                MERCHANT_FINANCE,
                MERCHANT_SUPPORT,
                MERCHANT_VIEWER
        );
    }
}
