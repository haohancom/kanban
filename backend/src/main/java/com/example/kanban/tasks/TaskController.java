package com.example.kanban.tasks;

import com.example.kanban.auth.CurrentUser;
import com.example.kanban.sprints.SprintRepository;
import com.example.kanban.teams.AuthorizationService;
import com.example.kanban.teams.TeamRepository;
import com.example.kanban.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TaskController {
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    public TaskController(
            TaskRepository taskRepository,
            TeamRepository teamRepository,
            SprintRepository sprintRepository,
            UserRepository userRepository,
            AuthorizationService authorizationService) {
        this.taskRepository = taskRepository;
        this.teamRepository = teamRepository;
        this.sprintRepository = sprintRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/api/teams/{teamId}/board/tasks")
    public List<TaskDtos.TaskResponse> listBoardTasks(
            Authentication authentication,
            @PathVariable long teamId,
            @RequestParam(required = false) Long subTeamId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long sprintId) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(teamId);
        if (!authorizationService.canViewTeamTree(currentUser.getId(), teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<Long> teamIds = authorizationService.descendantTeamIds(teamId);
        if (subTeamId != null && !teamIds.contains(subTeamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        TaskRepository.BoardTaskFilters filters = new TaskRepository.BoardTaskFilters(
                subTeamId,
                memberId,
                status,
                sprintId);
        return taskRepository.listBoardTasks(teamIds, filters).stream()
                .map(TaskDtos.TaskResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/api/teams/{teamId}/tasks")
    public TaskDtos.TaskResponse create(
            Authentication authentication,
            @PathVariable long teamId,
            @Valid @RequestBody TaskDtos.CreateTaskRequest request) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(teamId);
        if (!canCreateTask(currentUser.getId(), teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        validateSprint(teamId, request.getSprintId());
        validateAssignee(request.getAssigneeId());

        long taskId = taskRepository.create(
                teamId,
                request.getTitle(),
                defaultText(request.getDescription()),
                defaultText(request.getRemarks()),
                defaultText(request.getRisks()),
                defaultStatus(request.getStatus()),
                request.getSprintId(),
                request.getAssigneeId(),
                currentUser.getId());
        return TaskDtos.TaskResponse.from(findTaskOrThrow(taskId));
    }

    @GetMapping("/api/tasks/{id}")
    public TaskDtos.TaskResponse detail(Authentication authentication, @PathVariable long id) {
        CurrentUser currentUser = currentUser(authentication);
        TaskRepository.TaskRecord task = findTaskOrThrow(id);
        if (!authorizationService.canViewTeamTree(currentUser.getId(), task.getTeamId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return TaskDtos.TaskResponse.from(task);
    }

    @PatchMapping("/api/tasks/{id}")
    public TaskDtos.TaskResponse update(
            Authentication authentication,
            @PathVariable long id,
            @RequestBody TaskDtos.UpdateTaskRequest request) {
        CurrentUser currentUser = currentUser(authentication);
        TaskRepository.TaskRecord task = findTaskOrThrow(id);
        if (!canUpdateTask(currentUser.getId(), task)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String title = task.getTitle();
        if (request.getTitle() != null) {
            if (!StringUtils.hasText(request.getTitle())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            title = request.getTitle();
        }

        String description = request.getDescription() == null ? task.getDescription() : request.getDescription();
        String remarks = request.getRemarks() == null ? task.getRemarks() : request.getRemarks();
        String risks = request.getRisks() == null ? task.getRisks() : request.getRisks();
        TaskStatus status = request.getStatus() == null ? task.getStatus() : request.getStatus();
        Long sprintId = request.hasSprintId() ? request.getSprintId() : task.getSprintId();
        Long assigneeId = request.hasAssigneeId() ? request.getAssigneeId() : task.getAssigneeId();

        validateSprint(task.getTeamId(), sprintId);
        validateAssignee(assigneeId);
        taskRepository.update(id, title, description, remarks, risks, status, sprintId, assigneeId);
        return TaskDtos.TaskResponse.from(findTaskOrThrow(id));
    }

    @DeleteMapping("/api/tasks/{id}")
    public void softDelete(Authentication authentication, @PathVariable long id) {
        CurrentUser currentUser = currentUser(authentication);
        TaskRepository.TaskRecord task = findTaskOrThrow(id);
        if (!authorizationService.canManageTeam(currentUser.getId(), task.getTeamId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (taskRepository.softDelete(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private boolean canCreateTask(long userId, long teamId) {
        return authorizationService.canManageTeam(userId, teamId)
                || authorizationService.roleFor(userId, teamId).isPresent();
    }

    private boolean canUpdateTask(long userId, TaskRepository.TaskRecord task) {
        return authorizationService.canManageTeam(userId, task.getTeamId())
                || (task.getAssigneeId() != null
                && task.getAssigneeId() == userId
                && authorizationService.canViewTeamTree(userId, task.getTeamId()));
    }

    private void validateSprint(long teamId, Long sprintId) {
        if (sprintId == null) {
            return;
        }
        SprintRepository.SprintRecord sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!teamRepository.listSelfAndAncestors(teamId).contains(sprint.getTeamId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void validateAssignee(Long assigneeId) {
        if (assigneeId != null && !userRepository.findById(assigneeId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private TaskStatus defaultStatus(TaskStatus status) {
        return status == null ? TaskStatus.TODO : status;
    }

    private TeamRepository.TeamRecord findTeamOrThrow(long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private TaskRepository.TaskRecord findTaskOrThrow(long taskId) {
        TaskRepository.TaskRecord task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (task.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return task;
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}
