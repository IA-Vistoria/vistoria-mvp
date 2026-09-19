package br.com.vistoriapredial;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlMigrationIntegrationTest {

    @Test
    void shouldApplyAllMigrationsOnEmptyPostgreSqlDatabase() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();
            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load();

            MigrateResult result = flyway.migrate();

            assertThat(result.success).isTrue();
            assertThat(result.targetSchemaVersion).isEqualTo("4");

            try (var connection = postgres.createConnection("");
                 var statement = connection.prepareStatement("""
                         SELECT is_nullable, column_default, data_type
                           FROM information_schema.columns
                          WHERE table_schema = 'public'
                            AND table_name = 'tb_vistoria'
                            AND column_name = 'version'
                         """);
                 var columns = statement.executeQuery()) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getString("is_nullable")).isEqualTo("NO");
                assertThat(columns.getString("column_default")).contains("0");
                assertThat(columns.getString("data_type")).isEqualTo("bigint");
            }
        }
    }
}
