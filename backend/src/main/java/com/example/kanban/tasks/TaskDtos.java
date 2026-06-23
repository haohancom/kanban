package com.example.kanban.tasks;

import javax.validation.constraints.NotBlank;
import java.util.List;

public final class TaskDtos {
    private TaskDtos() {
    }

    public static class TaskResponse {
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

        public TaskResponse(
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

        public static TaskResponse from(TaskRepository.TaskRecord task) {
            return new TaskResponse(
                    task.getId(),
                    task.getTeamId(),
                    task.getTeamName(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getRemarks(),
                    task.getRisks(),
                    task.getStatus(),
                    task.getSprintId(),
                    task.getSprintName(),
                    task.getAssigneeId(),
                    task.getAssigneeDisplayName(),
                    task.getCreatedBy(),
                    task.getCreatedByDisplayName(),
                    task.getDeletedAt());
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

    public static class BulkDeleteTasksRequest {
        private List<Long> taskIds;

        public List<Long> getTaskIds() {
            return taskIds;
        }

        public void setTaskIds(List<Long> taskIds) {
            this.taskIds = taskIds;
        }
    }

    public static class CreateTaskRequest {
        @NotBlank
        private String title;

        private String description;

        private String remarks;

        private String risks;

        private TaskStatus status;

        private Long sprintId;

        private Long assigneeId;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public String getRisks() {
            return risks;
        }

        public void setRisks(String risks) {
            this.risks = risks;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public void setStatus(TaskStatus status) {
            this.status = status;
        }

        public Long getSprintId() {
            return sprintId;
        }

        public void setSprintId(Long sprintId) {
            this.sprintId = sprintId;
        }

        public Long getAssigneeId() {
            return assigneeId;
        }

        public void setAssigneeId(Long assigneeId) {
            this.assigneeId = assigneeId;
        }
    }

    public static class UpdateTaskRequest {
        private String title;

        private String description;

        private String remarks;

        private String risks;

        private TaskStatus status;

        private Long sprintId;

        private boolean sprintIdSet;

        private Long assigneeId;

        private boolean assigneeIdSet;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public String getRisks() {
            return risks;
        }

        public void setRisks(String risks) {
            this.risks = risks;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public void setStatus(TaskStatus status) {
            this.status = status;
        }

        public Long getSprintId() {
            return sprintId;
        }

        public void setSprintId(Long sprintId) {
            this.sprintId = sprintId;
            this.sprintIdSet = true;
        }

        public boolean hasSprintId() {
            return sprintIdSet;
        }

        public Long getAssigneeId() {
            return assigneeId;
        }

        public void setAssigneeId(Long assigneeId) {
            this.assigneeId = assigneeId;
            this.assigneeIdSet = true;
        }

        public boolean hasAssigneeId() {
            return assigneeIdSet;
        }
    }
}
