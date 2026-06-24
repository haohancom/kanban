package com.example.kanban.teams;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public final class TeamDtos {
    private TeamDtos() {
    }

    public static class TeamResponse {
        private final long id;
        private final String name;
        private final Long parentId;
        private final TeamRole role;
        private final List<TeamResponse> children;

        public TeamResponse(long id, String name, Long parentId, TeamRole role, List<TeamResponse> children) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
            this.role = role;
            this.children = children == null ? new ArrayList<>() : children;
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

        public TeamRole getRole() {
            return role;
        }

        public List<TeamResponse> getChildren() {
            return children;
        }
    }

    public static class MembershipResponse {
        private final long id;
        private final long teamId;
        private final long userId;
        private final String username;
        private final String displayName;
        private final TeamRole role;

        public MembershipResponse(
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

        public static MembershipResponse from(MembershipRepository.MembershipRecord membership) {
            return new MembershipResponse(
                    membership.getId(),
                    membership.getTeamId(),
                    membership.getUserId(),
                    membership.getUsername(),
                    membership.getDisplayName(),
                    membership.getRole());
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

    public static class AssignableUserResponse {
        private final long id;
        private final String username;
        private final String displayName;

        public AssignableUserResponse(long id, String username, String displayName) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
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
    }

    public static class CreateTeamRequest {
        @NotBlank
        private String name;

        private Long parentId;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }
    }

    public static class UpdateTeamRequest {
        @NotBlank
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class CreateMembershipRequest {
        @NotNull
        private Long userId;

        @NotNull
        private TeamRole role;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public TeamRole getRole() {
            return role;
        }

        public void setRole(TeamRole role) {
            this.role = role;
        }
    }

    public static class UpdateMembershipRequest {
        @NotNull
        private TeamRole role;

        public TeamRole getRole() {
            return role;
        }

        public void setRole(TeamRole role) {
            this.role = role;
        }
    }
}
