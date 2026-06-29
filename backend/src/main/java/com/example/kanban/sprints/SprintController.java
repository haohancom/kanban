package com.example.kanban.sprints;

import com.example.kanban.auth.CurrentUser;
import com.example.kanban.teams.AuthorizationService;
import com.example.kanban.teams.TeamRepository;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SprintController {
    private final SprintRepository sprintRepository;
    private final TeamRepository teamRepository;
    private final AuthorizationService authorizationService;

    public SprintController(
            SprintRepository sprintRepository,
            TeamRepository teamRepository,
            AuthorizationService authorizationService) {
        this.sprintRepository = sprintRepository;
        this.teamRepository = teamRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/api/teams/{teamId}/sprints")
    public List<SprintDtos.SprintResponse> list(Authentication authentication, @PathVariable long teamId) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(teamId);
        if (!authorizationService.canViewTeamTree(currentUser.getId(), teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return sprintRepository.listByTeamIds(teamRepository.listSelfAndAncestors(teamId)).stream()
                .map(SprintDtos.SprintResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/api/teams/{teamId}/sprints")
    public SprintDtos.SprintResponse create(
            Authentication authentication,
            @PathVariable long teamId,
            @Valid @RequestBody SprintDtos.CreateSprintRequest request) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(teamId);
        requireCanManageTeam(currentUser.getId(), teamId);
        long sprintId = sprintRepository.create(teamId, request.getName());
        return SprintDtos.SprintResponse.from(findSprintOrThrow(sprintId));
    }

    @PatchMapping("/api/sprints/{id}")
    public SprintDtos.SprintResponse update(
            Authentication authentication,
            @PathVariable long id,
            @RequestParam(required = false) Long teamId,
            @Valid @RequestBody SprintDtos.UpdateSprintRequest request) {
        CurrentUser currentUser = currentUser(authentication);
        SprintRepository.SprintRecord sprint = findSprintOrThrow(id);
        validateSprintTeamScope(currentUser.getId(), sprint, teamId);
        requireCanManageTeam(currentUser.getId(), sprint.getTeamId());
        if (request.getName() != null && !StringUtils.hasText(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String updatedName = request.getName() == null ? sprint.getName() : request.getName();
        boolean updatedActive = request.getActive() == null ? sprint.isActive() : request.getActive();
        sprintRepository.update(id, updatedName, updatedActive);
        return SprintDtos.SprintResponse.from(findSprintOrThrow(id));
    }

    @DeleteMapping("/api/sprints/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable long id, @RequestParam(required = false) Long teamId) {
        CurrentUser currentUser = currentUser(authentication);
        SprintRepository.SprintRecord sprint = findSprintOrThrow(id);
        validateSprintTeamScope(currentUser.getId(), sprint, teamId);
        requireCanManageTeam(currentUser.getId(), sprint.getTeamId());
        sprintRepository.delete(id);
    }

    private void validateSprintTeamScope(long userId, SprintRepository.SprintRecord sprint, Long teamId) {
        if (authorizationService.isSuperAdministrator(userId)) {
            return;
        }
        if (teamId == null || teamId == sprint.getTeamId()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
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

    private SprintRepository.SprintRecord findSprintOrThrow(long sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}
