package com.example.kanban;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationSmokeTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void startsAndCreatesCoreTables() {
        Integer userTables = jdbcTemplate.queryForObject(
                "select count(*) from sqlite_master where type = 'table' and name = 'users'",
                Integer.class);
        Integer taskTables = jdbcTemplate.queryForObject(
                "select count(*) from sqlite_master where type = 'table' and name = 'tasks'",
                Integer.class);

        assertThat(userTables).isEqualTo(1);
        assertThat(taskTables).isEqualTo(1);
    }
}
