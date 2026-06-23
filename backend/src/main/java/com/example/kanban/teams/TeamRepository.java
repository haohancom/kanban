package com.example.kanban.teams;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class TeamRepository {
    private final JdbcTemplate jdbc;

    public TeamRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long create(String name, Long parentId, long createdBy) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "insert into teams (name, parent_id, created_by) values (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            if (parentId == null) {
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(2, parentId);
            }
            statement.setLong(3, createdBy);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not read generated team id");
        }
        return key.longValue();
    }

    public Optional<TeamRecord> findById(long id) {
        List<TeamRecord> teams = jdbc.query(
                "select id, name, parent_id, created_by from teams where id = ?",
                TeamRepository::mapTeam,
                id);
        return teams.stream().findFirst();
    }

    public List<TeamRecord> listAll() {
        return jdbc.query(
                "select id, name, parent_id, created_by from teams order by id",
                TeamRepository::mapTeam);
    }

    public void updateName(long id, String name) {
        jdbc.update(
                "update teams set name = ?, updated_at = current_timestamp where id = ?",
                name,
                id);
    }

    public boolean hasChildren(long id) {
        Integer count = jdbc.queryForObject(
                "select count(*) from teams where parent_id = ?",
                Integer.class,
                id);
        return count != null && count > 0;
    }

    public boolean hasSprints(long id) {
        Integer count = jdbc.queryForObject(
                "select count(*) from sprints where team_id = ?",
                Integer.class,
                id);
        return count != null && count > 0;
    }

    public boolean hasTasks(long id) {
        Integer count = jdbc.queryForObject(
                "select count(*) from tasks where team_id = ?",
                Integer.class,
                id);
        return count != null && count > 0;
    }

    public void delete(long id) {
        jdbc.update("delete from team_memberships where team_id = ?", id);
        jdbc.update("delete from teams where id = ?", id);
    }

    private static TeamRecord mapTeam(ResultSet resultSet, int rowNumber) throws SQLException {
        Long parentId = resultSet.getObject("parent_id") == null ? null : resultSet.getLong("parent_id");
        return new TeamRecord(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                parentId,
                resultSet.getLong("created_by"));
    }

    public static class TeamRecord {
        private final long id;
        private final String name;
        private final Long parentId;
        private final long createdBy;

        public TeamRecord(long id, String name, Long parentId, long createdBy) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
            this.createdBy = createdBy;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Long getParentId() {
            return parentId;
        }

        public long getCreatedBy() {
            return createdBy;
        }
    }
}
