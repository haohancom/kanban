package com.example.kanban.auth;

import com.example.kanban.users.UserRepository;

public class CurrentUser {
    private final long id;
    private final String username;
    private final String displayName;
    private final boolean superAdmin;
    private final String avatarUrl;

    public CurrentUser(long id, String username, String displayName, boolean superAdmin) {
        this(id, username, displayName, superAdmin, null);
    }

    public CurrentUser(long id, String username, String displayName, boolean superAdmin, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.superAdmin = superAdmin;
        this.avatarUrl = avatarUrl;
    }

    public static CurrentUser from(UserRepository.UserRecord user) {
        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.isSuperAdmin(),
                avatarUrl(user));
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

    private static String avatarUrl(UserRepository.UserRecord user) {
        if (!user.hasAvatar()) {
            return null;
        }
        return "/api/users/me/avatar?v=" + user.getAvatarUpdatedAt().replaceAll("[^0-9]", "");
    }
}
