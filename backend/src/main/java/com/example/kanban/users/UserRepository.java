package com.example.kanban.users;

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
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long create(String username, String displayName, String passwordHash, boolean superAdmin) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "insert into users (username, display_name, password_hash, super_admin) values (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, username);
            statement.setString(2, displayName);
            statement.setString(3, passwordHash);
            statement.setInt(4, superAdmin ? 1 : 0);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not read generated user id");
        }
        return key.longValue();
    }

    public Optional<UserRecord> findByUsername(String username) {
        List<UserRecord> users = jdbc.query(
                "select id, username, display_name, password_hash, super_admin from users where username = ?",
                UserRepository::mapUser,
                username);
        return users.stream().findFirst();
    }

    public Optional<UserRecord> findById(long id) {
        List<UserRecord> users = jdbc.query(
                "select id, username, display_name, password_hash, super_admin from users where id = ?",
                UserRepository::mapUser,
                id);
        return users.stream().findFirst();
    }

    public List<UserRecord> listUsers() {
        return jdbc.query(
                "select id, username, display_name, password_hash, super_admin from users order by id",
                UserRepository::mapUser);
    }

    public void update(long id, String displayName, boolean superAdmin) {
        jdbc.update(
                "update users set display_name = ?, super_admin = ?, updated_at = current_timestamp where id = ?",
                displayName,
                superAdmin ? 1 : 0,
                id);
    }

    public void updatePasswordHash(long id, String passwordHash) {
        jdbc.update(
                "update users set password_hash = ?, updated_at = current_timestamp where id = ?",
                passwordHash,
                id);
    }

    public int countSuperAdministrators() {
        Integer count = jdbc.queryForObject(
                "select count(*) from users where super_admin = 1",
                Integer.class);
        return count == null ? 0 : count;
    }

    public int countUsers() {
        Integer count = jdbc.queryForObject("select count(*) from users", Integer.class);
        return count == null ? 0 : count;
    }

    private static UserRecord mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UserRecord(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("display_name"),
                resultSet.getString("password_hash"),
                resultSet.getInt("super_admin") == 1);
    }

    public static class UserRecord {
        private final long id;
        private final String username;
        private final String displayName;
        private final String passwordHash;
        private final boolean superAdmin;

        public UserRecord(long id, String username, String displayName, String passwordHash, boolean superAdmin) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
            this.passwordHash = passwordHash;
            this.superAdmin = superAdmin;
        }

        public long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public boolean isSuperAdmin() {
            return superAdmin;
        }
    }
}
