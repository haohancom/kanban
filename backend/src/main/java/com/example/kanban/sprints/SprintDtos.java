package com.example.kanban.sprints;

import javax.validation.constraints.NotBlank;

public final class SprintDtos {
    private SprintDtos() {
    }

    public static class SprintResponse {
        private final long id;
        private final long teamId;
        private final String name;
        private final boolean active;

        public SprintResponse(long id, long teamId, String name, boolean active) {
            this.id = id;
            this.teamId = teamId;
            this.name = name;
            this.active = active;
        }

        public static SprintResponse from(SprintRepository.SprintRecord sprint) {
            return new SprintResponse(
                    sprint.getId(),
                    sprint.getTeamId(),
                    sprint.getName(),
                    sprint.isActive());
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

    public static class CreateSprintRequest {
        @NotBlank
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class UpdateSprintRequest {
        private String name;

        private Boolean active;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }
}
