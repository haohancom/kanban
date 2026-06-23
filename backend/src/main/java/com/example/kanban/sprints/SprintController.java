package com.example.kanban.sprints;

import com.example.kanban.auth.CurrentUser;
import com.example.kanban.teams.AuthorizationService;
import com.example.kanban.teams.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        return sprintRepository.listByTeam(teamId).stream()
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
            @Valid @RequestBody SprintDtos.UpdateSprintRequest request) {
        CurrentUser currentUser = currentUser(authentication);
        SprintRepository.SprintRecord sprint = findSprintOrThrow(id);
        requireCanManageTeam(currentUser.getId(), sprint.getTeamId());
        if (request.getName() != null && !StringUtils.hasText(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String updatedName = request.getName() == null ? sprint.getName() : request.getName();
        boolean updatedActive = request.getActive() == null ? sprint.isActive() : request.getActive();
        sprintRepository.update(id, updatedName, updatedActive);
        return SprintDtos.SprintResponse.from(findSprintOrThrow(id));
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
