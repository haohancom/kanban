package com.example.kanban.auth;

import com.example.kanban.users.UserRepository;

public class CurrentUser {
    private final long id;
    private final String username;
    private final String displayName;
    private final boolean superAdmin;

    public CurrentUser(long id, String username, String displayName, boolean superAdmin) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.superAdmin = superAdmin;
    }

    public static CurrentUser from(UserRepository.UserRecord user) {
        return new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName(), user.isSuperAdmin());
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
}
