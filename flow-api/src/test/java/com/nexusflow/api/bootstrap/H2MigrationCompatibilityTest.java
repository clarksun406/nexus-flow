package com.nexusflow.api.bootstrap;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the local 'h2' profile: the standard PostgreSQL Flyway scripts under
 * {@code db/migration} must also apply cleanly on H2 in PostgreSQL compatibility
 * mode. Fails fast when a new migration introduces PostgreSQL-only syntax that
 * would break the zero-dependency local startup.
 */
class H2MigrationCompatibilityTest {

    private static final String H2_URL =
            "jdbc:h2:mem:nexusflow_flyway_compat;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=KEY,VALUE;DB_CLOSE_DELAY=-1";

    @Test
    void allMigrationsApplyCleanlyOnH2PostgreSQLMode() {
        MigrateResult result = Flyway.configure()
                .dataSource(H2_URL, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        assertEquals(14, result.migrationsExecuted, "every db/migration script must apply on H2");
    }
}
