package com.example.kanban.teams;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class MembershipRepository {
    private final JdbcTemplate jdbc;

    public MembershipRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long create(long teamId, long userId, TeamRole role) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "insert into team_memberships (team_id, user_id, role) values (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, teamId);
            statement.setLong(2, userId);
            statement.setString(3, role.name());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not read generated membership id");
        }
        return key.longValue();
    }

    public Optional<MembershipRecord> find(long teamId, long userId) {
        List<MembershipRecord> memberships = jdbc.query(
                "select m.id, m.team_id, m.user_id, u.username, u.display_name, m.role "
                        + "from team_memberships m "
                        + "join users u on u.id = m.user_id "
                        + "where m.team_id = ? and m.user_id = ?",
                MembershipRepository::mapMembership,
                teamId,
                userId);
        return memberships.stream().findFirst();
    }

    public Optional<MembershipRecord> findById(long teamId, long id) {
        List<MembershipRecord> memberships = jdbc.query(
                "select m.id, m.team_id, m.user_id, u.username, u.display_name, m.role "
                        + "from team_memberships m "
                        + "join users u on u.id = m.user_id "
                        + "where m.team_id = ? and m.id = ?",
                MembershipRepository::mapMembership,
                teamId,
                id);
        return memberships.stream().findFirst();
    }

    public List<MembershipRecord> listByTeam(long teamId) {
        return jdbc.query(
                "select m.id, m.team_id, m.user_id, u.username, u.display_name, m.role "
                        + "from team_memberships m "
                        + "join users u on u.id = m.user_id "
                        + "where m.team_id = ? "
                        + "order by m.id",
                MembershipRepository::mapMembership,
                teamId);
    }

    public List<UserMembershipRecord> listByUser(long userId) {
        return jdbc.query(
                "select team_id, role from team_memberships where user_id = ? order by id",
                MembershipRepository::mapUserMembership,
                userId);
    }

    public int updateRoleById(long teamId, long id, TeamRole role) {
        return jdbc.update(
                "update team_memberships set role = ? where team_id = ? and id = ?",
                role.name(),
                teamId,
                id);
    }

    public int deleteById(long teamId, long id) {
        return jdbc.update(
                "delete from team_memberships where team_id = ? and id = ?",
                teamId,
                id);
    }

    private static MembershipRecord mapMembership(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MembershipRecord(
                resultSet.getLong("id"),
                resultSet.getLong("team_id"),
                resultSet.getLong("user_id"),
                resultSet.getString("username"),
                resultSet.getString("display_name"),
                TeamRole.valueOf(resultSet.getString("role")));
    }

    private static UserMembershipRecord mapUserMembership(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UserMembershipRecord(
                resultSet.getLong("team_id"),
                TeamRole.valueOf(resultSet.getString("role")));
    }

    public static class MembershipRecord {
        private final long id;
        private final long teamId;
        private final long userId;
        private final String username;
        private final String displayName;
        private final TeamRole role;

        public MembershipRecord(
                long id,
                long teamId,
                long userId,
                String username,
                String displayName,
                TeamRole role) {
            this.id = id;
            this.teamId = teamId;
            this.userId = userId;
            this.username = username;
            this.displayName = displayName;
            this.role = role;
        }

        public long getId() {
            return id;
        }

        public long getTeamId() {
            return teamId;
        }

        public long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public TeamRole getRole() {
            return role;
        }
    }

    public static class UserMembershipRecord {
        private final long teamId;
        private final TeamRole role;

        public UserMembershipRecord(long teamId, TeamRole role) {
            this.teamId = teamId;
            this.role = role;
        }

        public long getTeamId() {
            return teamId;
        }

        public TeamRole getRole() {
            return role;
        }
    }
}
