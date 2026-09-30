package br.com.vistoriapredial;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.Map;

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
            assertThat(result.targetSchemaVersion).isEqualTo("9");

            try (var connection = postgres.createConnection("");
                 var statement = connection.prepareStatement("""
                         SELECT column_name, is_nullable, data_type
                           FROM information_schema.columns
                          WHERE table_schema = 'public'
                            AND table_name = 'tb_ambiente_vistoria'
                          ORDER BY ordinal_position
                         """);
                 var columns = statement.executeQuery()) {
                Map<String, String> nullability = new HashMap<>();
                while (columns.next()) {
                    nullability.put(columns.getString("column_name"), columns.getString("is_nullable"));
                }
                assertThat(nullability)
                        .containsEntry("vistoria_id", "NO")
                        .containsEntry("tipo", "NO")
                        .containsEntry("nome", "NO")
                        .containsEntry("ordem", "NO");
            }

            try (var connection = postgres.createConnection("");
                 var statement = connection.prepareStatement("""
                         SELECT column_name, is_nullable
                           FROM information_schema.columns
                          WHERE table_schema = 'public'
                            AND table_name = 'tb_imagem_vistoria'
                            AND column_name IN ('ambiente_id', 'categoria')
                         """);
                 var columns = statement.executeQuery()) {
                Map<String, String> nullability = new HashMap<>();
                while (columns.next()) {
                    nullability.put(columns.getString("column_name"), columns.getString("is_nullable"));
                }
                assertThat(nullability)
                        .containsEntry("ambiente_id", "YES")
                        .containsEntry("categoria", "YES");
            }

            try (var connection = postgres.createConnection("");
                 var statement = connection.prepareStatement("""
                         SELECT data_type
                           FROM information_schema.columns
                          WHERE table_schema = 'public'
                            AND table_name = 'tb_vistoria'
                            AND column_name = 'revisao_usuario'
                         """);
                 var columns = statement.executeQuery()) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getString("data_type")).isEqualTo("text");
            }

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

            try (var connection = postgres.createConnection("");
                 var statement = connection.prepareStatement("""
                         SELECT is_nullable, column_default, data_type
                           FROM information_schema.columns
                          WHERE table_schema = 'public'
                            AND table_name = 'tb_vistoria'
                            AND column_name = 'roteiro_revisao'
                         """);
                 var columns = statement.executeQuery()) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getString("is_nullable")).isEqualTo("NO");
                assertThat(columns.getString("column_default")).contains("0");
                assertThat(columns.getString("data_type")).isEqualTo("integer");
            }

            try (var connection = postgres.createConnection("");
                 var statement = connection.prepareStatement("""
                         SELECT indexname, indexdef FROM pg_indexes
                          WHERE schemaname = 'public' AND tablename = 'tb_vistoria'
                         """);
                 var indexes = statement.executeQuery()) {
                Map<String, String> indexDefinitions = new HashMap<>();
                while (indexes.next()) {
                    indexDefinitions.put(
                            indexes.getString("indexname"),
                            indexes.getString("indexdef"));
                }
                assertThat(indexDefinitions).containsKeys(
                        "idx_vistoria_cliente_id",
                        "idx_vistoria_status",
                        "idx_vistoria_cliente_criacao_id",
                        "idx_vistoria_status_criacao_id");
                assertThat(indexDefinitions.get("idx_vistoria_cliente_criacao_id"))
                        .contains("(cliente_id, data_criacao DESC, id DESC)");
                assertThat(indexDefinitions.get("idx_vistoria_status_criacao_id"))
                        .contains("(status, data_criacao DESC, id DESC)");
            }
        }
    }
}
