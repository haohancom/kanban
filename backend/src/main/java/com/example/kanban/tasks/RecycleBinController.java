package com.example.kanban.tasks;

import com.example.kanban.auth.CurrentUser;
import com.example.kanban.teams.AuthorizationService;
import com.example.kanban.teams.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class RecycleBinController {
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final AuthorizationService authorizationService;

    public RecycleBinController(
            TaskRepository taskRepository,
            TeamRepository teamRepository,
            AuthorizationService authorizationService) {
        this.taskRepository = taskRepository;
        this.teamRepository = teamRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/api/teams/{teamId}/recycle-bin/tasks")
    public List<TaskDtos.TaskResponse> list(Authentication authentication, @PathVariable long teamId) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(teamId);
        if (!authorizationService.canViewTeamTree(currentUser.getId(), teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return taskRepository.listDeletedTasks(authorizationService.descendantTeamIds(teamId)).stream()
                .map(TaskDtos.TaskResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/api/recycle-bin/tasks/{id}/restore")
    public void restore(Authentication authentication, @PathVariable long id) {
        CurrentUser currentUser = currentUser(authentication);
        TaskRepository.TaskRecord task = findDeletedTaskOrThrow(id);
        requireCanManageTeam(currentUser.getId(), task.getTeamId());
        taskRepository.restore(id);
    }

    @DeleteMapping("/api/recycle-bin/tasks/{id}")
    public void permanentlyDelete(Authentication authentication, @PathVariable long id) {
        CurrentUser currentUser = currentUser(authentication);
        TaskRepository.TaskRecord task = findDeletedTaskOrThrow(id);
        requireCanManageTeam(currentUser.getId(), task.getTeamId());
        taskRepository.permanentlyDelete(id);
    }

    @PostMapping("/api/recycle-bin/tasks/bulk-delete")
    public void permanentlyDeleteSelected(
            Authentication authentication,
            @RequestBody TaskDtos.BulkDeleteTasksRequest request) {
        CurrentUser currentUser = currentUser(authentication);
        List<Long> taskIds = validatedTaskIds(request);
        for (Long taskId : taskIds) {
            TaskRepository.TaskRecord task = findDeletedTaskOrThrow(taskId);
            requireCanManageTeam(currentUser.getId(), task.getTeamId());
        }
        taskRepository.permanentlyDeleteByIds(taskIds);
    }

    @DeleteMapping("/api/teams/{teamId}/recycle-bin/tasks")
    public void permanentlyDeleteAll(Authentication authentication, @PathVariable long teamId) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(teamId);
        requireCanManageTeam(currentUser.getId(), teamId);
        taskRepository.permanentlyDeleteAllInTeamTree(authorizationService.descendantTeamIds(teamId));
    }

    private List<Long> validatedTaskIds(TaskDtos.BulkDeleteTasksRequest request) {
        if (request == null || request.getTaskIds() == null || request.getTaskIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        List<Long> ids = new ArrayList<>();
        for (Long taskId : request.getTaskIds()) {
            if (taskId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            ids.add(taskId);
        }
        return ids;
    }

    private TaskRepository.TaskRecord findDeletedTaskOrThrow(long id) {
        TaskRepository.TaskRecord task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (task.getDeletedAt() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return task;
    }

    private void requireCanManageTeam(long userId, long teamId) {
        if (!authorizationService.canManageTeam(userId, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private TeamRepository.TeamRecord findTeamOrThrow(long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}
