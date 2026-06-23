package com.example.kanban.tasks;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskRepository {
    private static final String TASK_SELECT = ""
            + "select t.id, t.team_id, teams.name as team_name, t.title, t.description, "
            + "t.remarks, t.risks, t.status, t.sprint_id, sprints.name as sprint_name, "
            + "t.assignee_id, assignee.display_name as assignee_display_name, t.created_by, "
            + "creator.display_name as created_by_display_name, t.deleted_at "
            + "from tasks t "
            + "join teams on teams.id = t.team_id "
            + "left join sprints on sprints.id = t.sprint_id "
            + "left join users assignee on assignee.id = t.assignee_id "
            + "join users creator on creator.id = t.created_by";

    private final JdbcTemplate jdbc;

    public TaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long create(
            long teamId,
            String title,
            String description,
            String remarks,
            String risks,
            TaskStatus status,
            Long sprintId,
            Long assigneeId,
            long createdBy) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "insert into tasks "
                            + "(team_id, title, description, remarks, risks, status, sprint_id, assignee_id, created_by) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, teamId);
            statement.setString(2, title);
            statement.setString(3, description);
            statement.setString(4, remarks);
            statement.setString(5, risks);
            statement.setString(6, status.name());
            setNullableLong(statement, 7, sprintId);
            setNullableLong(statement, 8, assigneeId);
            statement.setLong(9, createdBy);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not read generated task id");
        }
        return key.longValue();
    }

    public Optional<TaskRecord> findById(long id) {
        List<TaskRecord> tasks = jdbc.query(
                TASK_SELECT + " where t.id = ?",
                TaskRepository::mapTask,
                id);
        return tasks.stream().findFirst();
    }

    public List<TaskRecord> listBoardTasks(List<Long> teamIds, BoardTaskFilters filters) {
        if (teamIds.isEmpty()) {
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder(TASK_SELECT);
        List<Object> args = new ArrayList<>();
        sql.append(" where t.deleted_at is null");
        sql.append(" and t.team_id in (").append(placeholders(teamIds.size())).append(")");
        args.addAll(teamIds);

        if (filters.getSubTeamId() != null) {
            sql.append(" and t.team_id = ?");
            args.add(filters.getSubTeamId());
        }
        if (filters.getMemberId() != null) {
            sql.append(" and t.assignee_id = ?");
            args.add(filters.getMemberId());
        }
        if (filters.getStatus() != null) {
            sql.append(" and t.status = ?");
            args.add(filters.getStatus().name());
        }
        if (filters.getSprintId() != null) {
            sql.append(" and t.sprint_id = ?");
            args.add(filters.getSprintId());
        }

        sql.append(" order by t.id");
        return jdbc.query(sql.toString(), TaskRepository::mapTask, args.toArray());
    }

    public void update(
            long id,
            String title,
            String description,
            String remarks,
            String risks,
            TaskStatus status,
            Long sprintId,
            Long assigneeId) {
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "update tasks set title = ?, description = ?, remarks = ?, risks = ?, status = ?, "
                            + "sprint_id = ?, assignee_id = ?, updated_at = current_timestamp where id = ?");
            statement.setString(1, title);
            statement.setString(2, description);
            statement.setString(3, remarks);
            statement.setString(4, risks);
            statement.setString(5, status.name());
            setNullableLong(statement, 6, sprintId);
            setNullableLong(statement, 7, assigneeId);
            statement.setLong(8, id);
            return statement;
        });
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static String placeholders(int count) {
        StringBuilder placeholders = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }
        return placeholders.toString();
    }

    private static TaskRecord mapTask(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TaskRecord(
                resultSet.getLong("id"),
                resultSet.getLong("team_id"),
                resultSet.getString("team_name"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("remarks"),
                resultSet.getString("risks"),
                TaskStatus.valueOf(resultSet.getString("status")),
                nullableLong(resultSet, "sprint_id"),
                resultSet.getString("sprint_name"),
                nullableLong(resultSet, "assignee_id"),
                resultSet.getString("assignee_display_name"),
                resultSet.getLong("created_by"),
                resultSet.getString("created_by_display_name"),
                resultSet.getString("deleted_at"));
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    public static class BoardTaskFilters {
        private final Long subTeamId;
        private final Long memberId;
        private final TaskStatus status;
        private final Long sprintId;

        public BoardTaskFilters(Long subTeamId, Long memberId, TaskStatus status, Long sprintId) {
            this.subTeamId = subTeamId;
            this.memberId = memberId;
            this.status = status;
            this.sprintId = sprintId;
        }

        public Long getSubTeamId() {
            return subTeamId;
        }

        public Long getMemberId() {
            return memberId;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public Long getSprintId() {
            return sprintId;
        }
    }

    public static class TaskRecord {
        private final long id;
        private final long teamId;
        private final String teamName;
        private final String title;
        private final String description;
        private final String remarks;
        private final String risks;
        private final TaskStatus status;
        private final Long sprintId;
        private final String sprintName;
        private final Long assigneeId;
        private final String assigneeDisplayName;
        private final long createdBy;
        private final String createdByDisplayName;
        private final String deletedAt;

        public TaskRecord(
                long id,
                long teamId,
                String teamName,
                String title,
                String description,
                String remarks,
                String risks,
                TaskStatus status,
                Long sprintId,
                String sprintName,
                Long assigneeId,
                String assigneeDisplayName,
                long createdBy,
                String createdByDisplayName,
                String deletedAt) {
            this.id = id;
            this.teamId = teamId;
            this.teamName = teamName;
            this.title = title;
            this.description = description;
            this.remarks = remarks;
            this.risks = risks;
            this.status = status;
            this.sprintId = sprintId;
            this.sprintName = sprintName;
            this.assigneeId = assigneeId;
            this.assigneeDisplayName = assigneeDisplayName;
            this.createdBy = createdBy;
            this.createdByDisplayName = createdByDisplayName;
            this.deletedAt = deletedAt;
        }

        public long getId() {
            return id;
        }

        public long getTeamId() {
            return teamId;
        }

        public String getTeamName() {
            return teamName;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getRemarks() {
            return remarks;
        }

        public String getRisks() {
            return risks;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public Long getSprintId() {
            return sprintId;
        }

        public String getSprintName() {
            return sprintName;
        }

        public Long getAssigneeId() {
            return assigneeId;
        }

        public String getAssigneeDisplayName() {
            return assigneeDisplayName;
        }

        public long getCreatedBy() {
            return createdBy;
        }

        public String getCreatedByDisplayName() {
            return createdByDisplayName;
        }

        public String getDeletedAt() {
            return deletedAt;
        }
    }
}
