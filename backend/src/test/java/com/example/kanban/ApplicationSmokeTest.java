package com.example.kanban;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ApplicationSmokeTest {
    private static final Path DATABASE_PATH = createDatabasePath();

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath());
    }

    @Test
    void startsWithIsolatedDatabaseAndCreatesCoreSchema() {
        assertThat(DATABASE_PATH).exists();
        assertThat(DATABASE_PATH.getFileName().toString()).isEqualTo("application-smoke.sqlite3");
        assertThat(DATABASE_PATH.getParent().getFileName().toString()).startsWith("kanban-smoke-");

        List<String> tableNames = jdbcTemplate.queryForList(
                "select name from sqlite_master where type = 'table'",
                String.class);
        assertThat(tableNames).containsAll(Arrays.asList(
                "users",
                "teams",
                "team_memberships",
                "sprints",
                "tasks",
                "system_settings"));

        assertUniqueIndexExists("idx_users_username", "users");
        assertUniqueIndexExists("idx_team_memberships_team_user", "team_memberships");
        assertIndexColumns("idx_users_username", "username");
        assertIndexColumns("idx_team_memberships_team_user", "team_id", "user_id");
    }

    @Test
    void enforcesSqliteForeignKeys() {
        Integer foreignKeys = jdbcTemplate.queryForObject("pragma foreign_keys", Integer.class);

        assertThat(foreignKeys).isEqualTo(1);
        assertThatThrownBy(insertOrphanTask())
                .isInstanceOf(DataAccessException.class);
    }

    private void assertUniqueIndexExists(String indexName, String tableName) {
        Integer indexes = jdbcTemplate.queryForObject(
                "select count(*) from pragma_index_list(?) where name = ? and \"unique\" = 1",
                Integer.class,
                tableName,
                indexName);
        assertThat(indexes).isEqualTo(1);
    }

    private void assertIndexColumns(String indexName, String... columnNames) {
        List<String> actualColumnNames = jdbcTemplate.queryForList(
                "select name from pragma_index_info(?) order by seqno",
                String.class,
                indexName);
        assertThat(actualColumnNames).containsExactly(columnNames);
    }

    private ThrowingCallable insertOrphanTask() {
        return () -> jdbcTemplate.update(
                "insert into tasks (team_id, title, created_by) values (?, ?, ?)",
                999999,
                "orphan task",
                999999);
    }

    private static Path createDatabasePath() {
        try {
            Path directory = Files.createTempDirectory("kanban-smoke-");
            return directory.resolve("application-smoke.sqlite3");
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
