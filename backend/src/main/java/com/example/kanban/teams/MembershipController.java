package com.example.kanban.teams;

import com.example.kanban.auth.CurrentUser;
import com.example.kanban.users.UserRepository;
import org.springframework.dao.DataAccessException;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams/{teamId}/members")
public class MembershipController {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final MembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;

    public MembershipController(
            UserRepository userRepository,
            TeamRepository teamRepository,
            MembershipRepository membershipRepository,
            AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<TeamDtos.MembershipResponse> list(Authentication authentication, @PathVariable long teamId) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(teamId);
        if (!authorizationService.canViewTeamTree(currentUser.getId(), teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return membershipRepository.listByTeam(teamId).stream()
                .map(TeamDtos.MembershipResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping
    public TeamDtos.MembershipResponse create(
            Authentication authentication,
            @PathVariable long teamId,
            @Valid @RequestBody TeamDtos.CreateMembershipRequest request) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(teamId);
        requireCanManageMembers(currentUser.getId(), teamId);
        findUserOrThrow(request.getUserId());
        try {
            membershipRepository.create(teamId, request.getUserId(), request.getRole());
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return TeamDtos.MembershipResponse.from(findMembershipOrThrow(teamId, request.getUserId()));
    }

    @PatchMapping("/{membershipId}")
    public TeamDtos.MembershipResponse update(
            Authentication authentication,
            @PathVariable long teamId,
            @PathVariable long membershipId,
            @Valid @RequestBody TeamDtos.UpdateMembershipRequest request) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(teamId);
        requireCanManageMembers(currentUser.getId(), teamId);
        if (membershipRepository.updateRoleById(teamId, membershipId, request.getRole()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return TeamDtos.MembershipResponse.from(findMembershipByIdOrThrow(teamId, membershipId));
    }

    @DeleteMapping("/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @PathVariable long teamId,
            @PathVariable long membershipId) {
        CurrentUser currentUser = currentUser(authentication);
        findTeamOrThrow(teamId);
        requireCanManageMembers(currentUser.getId(), teamId);
        if (membershipRepository.deleteById(teamId, membershipId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private void requireCanManageMembers(long userId, long teamId) {
        if (!authorizationService.canManageMembers(userId, teamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void findTeamOrThrow(long id) {
        teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void findUserOrThrow(long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private MembershipRepository.MembershipRecord findMembershipOrThrow(long teamId, long userId) {
        return membershipRepository.find(teamId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private MembershipRepository.MembershipRecord findMembershipByIdOrThrow(long teamId, long id) {
        return membershipRepository.findById(teamId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}
