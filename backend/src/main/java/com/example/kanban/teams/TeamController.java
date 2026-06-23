package com.example.kanban.teams;

import com.example.kanban.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private final TeamRepository teamRepository;
    private final AuthorizationService authorizationService;
    private final TeamService teamService;

    public TeamController(
            TeamRepository teamRepository,
            AuthorizationService authorizationService,
            TeamService teamService) {
        this.teamRepository = teamRepository;
        this.authorizationService = authorizationService;
        this.teamService = teamService;
    }

    @GetMapping
    public List<TeamDtos.TeamResponse> list(Authentication authentication) {
        CurrentUser currentUser = currentUser(authentication);
        List<TeamRepository.TeamRecord> teams = teamRepository.listAll();
        Set<Long> visibleIds = authorizationService.visibleTeamIds(currentUser.getId());
        return buildTree(currentUser.getId(), teams, visibleIds);
    }

    @PostMapping
    public TeamDtos.TeamResponse create(
            Authentication authentication,
            @Valid @RequestBody TeamDtos.CreateTeamRequest request) {
        CurrentUser currentUser = currentUser(authentication);
        if (request.getParentId() != null) {
            findTeamOrThrow(request.getParentId());
            requireCanManageTeam(currentUser.getId(), request.getParentId());
        }
        return toResponse(
                currentUser.getId(),
                teamService.createTeam(request.getName(), request.getParentId(), currentUser.getId()),
                new ArrayList<>());
    }

    @PatchMapping("/{id}")
    public TeamDtos.TeamResponse update(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody TeamDtos.UpdateTeamRequest request) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(id);
        requireCanManageTeam(currentUser.getId(), id);
        teamRepository.updateName(id, request.getName());
        return toResponse(currentUser.getId(), findTeamOrThrow(id), new ArrayList<>());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable long id) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(id);
        requireCanManageTeam(currentUser.getId(), id);
        if (teamRepository.hasChildren(id) || teamRepository.hasSprints(id) || teamRepository.hasTasks(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        teamService.deleteTeam(id);
    }

    private List<TeamDtos.TeamResponse> buildTree(
            long userId,
            List<TeamRepository.TeamRecord> teams,
            Set<Long> visibleIds) {
        Map<Long, List<TeamRepository.TeamRecord>> visibleChildrenByParent = new HashMap<>();
        for (TeamRepository.TeamRecord team : teams) {
            if (visibleIds.contains(team.getId()) && team.getParentId() != null && visibleIds.contains(team.getParentId())) {
                visibleChildrenByParent.computeIfAbsent(team.getParentId(), ignored -> new ArrayList<>()).add(team);
            }
        }
        return teams.stream()
                .filter(team -> visibleIds.contains(team.getId()))
                .filter(team -> team.getParentId() == null || !visibleIds.contains(team.getParentId()))
                .map(team -> toTreeResponse(userId, team, visibleChildrenByParent))
                .collect(Collectors.toList());
    }

    private TeamDtos.TeamResponse toTreeResponse(
            long userId,
            TeamRepository.TeamRecord team,
            Map<Long, List<TeamRepository.TeamRecord>> childrenByParent) {
        List<TeamDtos.TeamResponse> children = childrenByParent
                .getOrDefault(team.getId(), new ArrayList<>())
                .stream()
                .map(child -> toTreeResponse(userId, child, childrenByParent))
                .collect(Collectors.toList());
        return toResponse(userId, team, children);
    }

    private TeamDtos.TeamResponse toResponse(
            long userId,
            TeamRepository.TeamRecord team,
            List<TeamDtos.TeamResponse> children) {
        TeamRole role = authorizationService.roleFor(userId, team.getId()).orElse(null);
        return new TeamDtos.TeamResponse(team.getId(), team.getName(), team.getParentId(), role, children);
    }

    private void requireCanManageTeam(long userId, long teamId) {
        if (!authorizationService.canManageTeam(userId, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private TeamRepository.TeamRecord findTeamOrThrow(long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}
