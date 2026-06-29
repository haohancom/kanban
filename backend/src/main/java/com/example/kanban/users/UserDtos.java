package com.example.kanban.users;

import javax.validation.constraints.NotBlank;

public final class UserDtos {
    private UserDtos() {
    }

    public static class UserResponse {
        private final long id;
        private final String username;
        private final String displayName;
        private final boolean superAdmin;
        private final String avatarUrl;

        public UserResponse(long id, String username, String displayName, boolean superAdmin) {
            this(id, username, displayName, superAdmin, null);
        }

        public UserResponse(long id, String username, String displayName, boolean superAdmin, String avatarUrl) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
            this.superAdmin = superAdmin;
            this.avatarUrl = avatarUrl;
        }

        public static UserResponse from(UserRepository.UserRecord user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.isSuperAdmin());
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

        public boolean isSuperAdmin() {
            return superAdmin;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }
    }

    public static class CreateUserRequest {
        @NotBlank
        private String username;

        @NotBlank
        private String displayName;

        @NotBlank
        private String password;

        private boolean superAdmin;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isSuperAdmin() {
            return superAdmin;
        }

        public void setSuperAdmin(boolean superAdmin) {
            this.superAdmin = superAdmin;
        }
    }

    public static class UpdateUserRequest {
        private String displayName;

        private Boolean superAdmin;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Boolean getSuperAdmin() {
            return superAdmin;
        }

        public void setSuperAdmin(Boolean superAdmin) {
            this.superAdmin = superAdmin;
        }
    }

    public static class ResetPasswordRequest {
        @NotBlank
        private String password;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
