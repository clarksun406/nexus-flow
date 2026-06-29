package com.nexusflow.permission.entity;

import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleMappingTest {

    @Test
    void userRoleDoesNotDeclareMisleadingNullableUniqueConstraint() {
        Table table = UserRole.class.getAnnotation(Table.class);

        assertThat(table).isNotNull();
        assertThat(table.uniqueConstraints()).isEmpty();
    }
}
