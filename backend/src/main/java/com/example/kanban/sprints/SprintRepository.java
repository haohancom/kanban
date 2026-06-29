package com.example.kanban.sprints;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SprintRepository {
    private final JdbcTemplate jdbc;

    public SprintRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long create(long teamId, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "insert into sprints (team_id, name) values (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, teamId);
            statement.setString(2, name);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not read generated sprint id");
        }
        return key.longValue();
    }

    public Optional<SprintRecord> findById(long id) {
        List<SprintRecord> sprints = jdbc.query(
                "select id, team_id, name, active from sprints where id = ?",
                SprintRepository::mapSprint,
                id);
        return sprints.stream().findFirst();
    }

    public List<SprintRecord> listByTeam(long teamId) {
        return listByTeamIds(Collections.singletonList(teamId));
    }

    public List<SprintRecord> listByTeamIds(List<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }
        String placeholders = teamIds.stream()
                .map(ignored -> "?")
                .collect(Collectors.joining(","));

        Object[] args = teamIds.toArray();
        return jdbc.query(
                "select id, team_id, name, active from sprints where team_id in (" + placeholders + ") order by id",
                SprintRepository::mapSprint,
                args);
    }

    public void update(long id, String name, boolean active) {
        jdbc.update(
                "update sprints set name = ?, active = ?, updated_at = current_timestamp where id = ?",
                name,
                active ? 1 : 0,
                id);
    }

    public void delete(long id) {
        jdbc.update("update tasks set sprint_id = null where sprint_id = ?", id);
        jdbc.update("delete from sprints where id = ?", id);
    }

    private static SprintRecord mapSprint(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SprintRecord(
                resultSet.getLong("id"),
                resultSet.getLong("team_id"),
                resultSet.getString("name"),
                resultSet.getInt("active") == 1);
    }

    public static class SprintRecord {
        private final long id;
        private final long teamId;
        private final String name;
        private final boolean active;

        public SprintRecord(long id, long teamId, String name, boolean active) {
            this.id = id;
            this.teamId = teamId;
            this.name = name;
            this.active = active;
        }

        public long getId() {
            return id;
        }

        public long getTeamId() {
            return teamId;
        }

        public String getName() {
            return name;
        }

        public boolean isActive() {
            return active;
        }
    }
}
