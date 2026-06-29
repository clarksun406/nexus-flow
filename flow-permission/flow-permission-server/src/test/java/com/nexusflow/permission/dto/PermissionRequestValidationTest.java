package com.nexusflow.permission.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRoleCreateRequest() {
        RoleCreateRequest request = new RoleCreateRequest(
                "MERCHANT_REFUND_OPERATOR",
                "Merchant Refund Operator",
                "MERCHANT",
                "Can create refunds",
                List.of("refund:create", "payment_order:read"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidRoleCreateRequest() {
        RoleCreateRequest request = new RoleCreateRequest(
                "bad role",
                "",
                "BAD_SCOPE",
                "description",
                List.of("bad-permission"));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("code", "name", "scope", "permissionCodes[0].<list element>");
    }

    @Test
    void rejectsInvalidCheckRequest() {
        CheckRequest request = new CheckRequest(null, "bad-permission", "BAD_SCOPE", null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("userId", "permission", "scopeType");
    }

    @Test
    void rejectsInvalidGrantRoleRequest() {
        GrantRoleRequest request = new GrantRoleRequest(null, UUID.randomUUID(), "BAD_SCOPE", null, null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("userId", "scopeType");
    }
}
