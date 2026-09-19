package br.com.vistoriapredial;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlMigrationIntegrationTest {

    @Test
    void shouldApplyAllMigrationsOnEmptyPostgreSqlDatabase() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();
            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load();

            MigrateResult result = flyway.migrate();

            assertThat(result.success).isTrue();
            assertThat(result.targetSchemaVersion).isEqualTo("3");
        }
    }
}
