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
        ensureAvatarColumns();
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
                "select id, username, display_name, password_hash, super_admin, avatar_content_type, avatar_updated_at "
                        + "from users where username = ?",
                UserRepository::mapUser,
                username);
        return users.stream().findFirst();
    }

    public Optional<UserRecord> findById(long id) {
        List<UserRecord> users = jdbc.query(
                "select id, username, display_name, password_hash, super_admin, avatar_content_type, avatar_updated_at "
                        + "from users where id = ?",
                UserRepository::mapUser,
                id);
        return users.stream().findFirst();
    }

    public List<UserRecord> listUsers() {
        return jdbc.query(
                "select id, username, display_name, password_hash, super_admin, avatar_content_type, avatar_updated_at "
                        + "from users order by id",
                UserRepository::mapUser);
    }

    public void update(long id, String displayName, boolean superAdmin) {
        jdbc.update(
                "update users set display_name = ?, super_admin = ?, updated_at = current_timestamp where id = ?",
                displayName,
                superAdmin ? 1 : 0,
                id);
    }

    public int demoteSuperAdministratorIfAnotherExists(long id) {
        return jdbc.update(
                "update users set super_admin = 0, updated_at = current_timestamp "
                        + "where id = ? and super_admin = 1 "
                        + "and exists (select 1 from users where super_admin = 1 and id <> ?)",
                id,
                id);
    }

    public void updatePasswordHash(long id, String passwordHash) {
        jdbc.update(
                "update users set password_hash = ?, updated_at = current_timestamp where id = ?",
                passwordHash,
                id);
    }

    public void updateAvatar(long id, byte[] data, String contentType) {
        jdbc.update(
                "update users set avatar_data = ?, avatar_content_type = ?, "
                        + "avatar_updated_at = current_timestamp, updated_at = current_timestamp where id = ?",
                data,
                contentType,
                id);
    }

    public Optional<AvatarRecord> findAvatarById(long id) {
        List<AvatarRecord> avatars = jdbc.query(
                "select avatar_data, avatar_content_type from users "
                        + "where id = ? and avatar_data is not null and avatar_content_type is not null",
                (resultSet, rowNumber) -> new AvatarRecord(
                        resultSet.getBytes("avatar_data"),
                        resultSet.getString("avatar_content_type")),
                id);
        return avatars.stream().findFirst();
    }

    public void removeAvatar(long id) {
        jdbc.update(
                "update users set avatar_data = null, avatar_content_type = null, "
                        + "avatar_updated_at = null, updated_at = current_timestamp where id = ?",
                id);
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
                resultSet.getInt("super_admin") == 1,
                resultSet.getString("avatar_content_type"),
                resultSet.getString("avatar_updated_at"));
    }

    private void ensureAvatarColumns() {
        if (!usersTableExists()) {
            return;
        }
        addColumnIfMissing("avatar_data", "blob");
        addColumnIfMissing("avatar_content_type", "text");
        addColumnIfMissing("avatar_updated_at", "text");
    }

    private boolean usersTableExists() {
        Integer count = jdbc.queryForObject(
                "select count(*) from sqlite_master where type = 'table' and name = 'users'",
                Integer.class);
        return count != null && count > 0;
    }

    private void addColumnIfMissing(String columnName, String columnDefinition) {
        Integer count = jdbc.queryForObject(
                "select count(*) from pragma_table_info('users') where name = ?",
                Integer.class,
                columnName);
        if (count == null || count == 0) {
            jdbc.execute("alter table users add column " + columnName + " " + columnDefinition);
        }
    }

    public static class UserRecord {
        private final long id;
        private final String username;
        private final String displayName;
        private final String passwordHash;
        private final boolean superAdmin;
        private final String avatarContentType;
        private final String avatarUpdatedAt;

        public UserRecord(long id, String username, String displayName, String passwordHash, boolean superAdmin) {
            this(id, username, displayName, passwordHash, superAdmin, null, null);
        }

        public UserRecord(
                long id,
                String username,
                String displayName,
                String passwordHash,
                boolean superAdmin,
                String avatarContentType,
                String avatarUpdatedAt) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
            this.passwordHash = passwordHash;
            this.superAdmin = superAdmin;
            this.avatarContentType = avatarContentType;
            this.avatarUpdatedAt = avatarUpdatedAt;
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

        public String getAvatarContentType() {
            return avatarContentType;
        }

        public String getAvatarUpdatedAt() {
            return avatarUpdatedAt;
        }

        public boolean hasAvatar() {
            return avatarContentType != null && avatarUpdatedAt != null;
        }
    }

    public static class AvatarRecord {
        private final byte[] data;
        private final String contentType;

        public AvatarRecord(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }

        public byte[] getData() {
            return data;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
