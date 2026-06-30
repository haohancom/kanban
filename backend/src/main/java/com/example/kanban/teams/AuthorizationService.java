package com.example.kanban.teams;

import com.example.kanban.users.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final MembershipRepository membershipRepository;

    public AuthorizationService(
            UserRepository userRepository,
            TeamRepository teamRepository,
            MembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
    }

    public boolean canViewTeamTree(long userId, long teamId) {
        if (isSuperAdministrator(userId)) {
            return teamRepository.findById(teamId).isPresent();
        }
        Map<Long, TeamRepository.TeamRecord> teamsById = teamsById();
        if (!teamsById.containsKey(teamId)) {
            return false;
        }
        Set<Long> membershipTeamIds = membershipRepository.listByUser(userId).stream()
                .map(MembershipRepository.UserMembershipRecord::getTeamId)
                .collect(Collectors.toSet());
        Long current = teamId;
        while (current != null) {
            if (membershipTeamIds.contains(current)) {
                return true;
            }
            TeamRepository.TeamRecord team = teamsById.get(current);
            current = team == null ? null : team.getParentId();
        }
        return false;
    }

    public boolean canManageTeam(long userId, long teamId) {
        return hasManagementRoleOnTeamOrAncestor(userId, teamId);
    }

    public boolean canManageMembers(long userId, long teamId) {
        return hasManagementRoleOnTeamOrAncestor(userId, teamId);
    }

    public boolean canDeleteTeam(long userId, long teamId) {
        if (isSuperAdministrator(userId)) {
            return teamRepository.findById(teamId).isPresent();
        }
        return roleFor(userId, teamId).orElse(null) == TeamRole.TEAM_CREATOR;
    }

    public List<Long> descendantTeamIds(long teamId) {
        List<TeamRepository.TeamRecord> teams = teamRepository.listAll();
        if (teams.stream().noneMatch(team -> team.getId() == teamId)) {
            return Collections.emptyList();
        }
        Map<Long, List<Long>> childrenByParent = childrenByParent(teams);
        List<Long> descendants = new ArrayList<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(teamId);
        while (!queue.isEmpty()) {
            Long current = queue.remove();
            descendants.add(current);
            List<Long> children = childrenByParent.get(current);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return descendants;
    }

    public Set<Long> visibleTeamIds(long userId) {
        List<TeamRepository.TeamRecord> teams = teamRepository.listAll();
        if (isSuperAdministrator(userId)) {
            return teams.stream().map(TeamRepository.TeamRecord::getId).collect(Collectors.toSet());
        }
        Map<Long, List<Long>> childrenByParent = childrenByParent(teams);
        Set<Long> visible = new HashSet<>();
        for (MembershipRepository.UserMembershipRecord membership : membershipRepository.listByUser(userId)) {
            visible.addAll(descendantTeamIdsFromChildrenMap(membership.getTeamId(), childrenByParent));
        }
        return visible;
    }

    public Optional<TeamRole> roleFor(long userId, long teamId) {
        return membershipRepository.find(teamId, userId)
                .map(MembershipRepository.MembershipRecord::getRole);
    }

    public boolean isSuperAdministrator(long userId) {
        return userRepository.findById(userId)
                .map(UserRepository.UserRecord::isSuperAdmin)
                .orElse(false);
    }

    private boolean hasManagementRoleOnTeamOrAncestor(long userId, long teamId) {
        if (isSuperAdministrator(userId)) {
            return teamRepository.findById(teamId).isPresent();
        }
        Map<Long, TeamRepository.TeamRecord> teamsById = teamsById();
        if (!teamsById.containsKey(teamId)) {
            return false;
        }
        Map<Long, TeamRole> rolesByTeamId = membershipRepository.listByUser(userId).stream()
                .collect(Collectors.toMap(
                        MembershipRepository.UserMembershipRecord::getTeamId,
                        MembershipRepository.UserMembershipRecord::getRole));
        Long current = teamId;
        while (current != null) {
            TeamRole role = rolesByTeamId.get(current);
            if (role == TeamRole.TEAM_CREATOR || role == TeamRole.TEAM_ADMIN) {
                return true;
            }
            TeamRepository.TeamRecord team = teamsById.get(current);
            current = team == null ? null : team.getParentId();
        }
        return false;
    }

    private Map<Long, TeamRepository.TeamRecord> teamsById() {
        return teamRepository.listAll().stream()
                .collect(Collectors.toMap(TeamRepository.TeamRecord::getId, team -> team));
    }

    private Map<Long, List<Long>> childrenByParent(List<TeamRepository.TeamRecord> teams) {
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (TeamRepository.TeamRecord team : teams) {
            if (team.getParentId() != null) {
                childrenByParent.computeIfAbsent(team.getParentId(), ignored -> new ArrayList<>()).add(team.getId());
            }
        }
        return childrenByParent;
    }

    private List<Long> descendantTeamIdsFromChildrenMap(long teamId, Map<Long, List<Long>> childrenByParent) {
        List<Long> descendants = new ArrayList<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(teamId);
        while (!queue.isEmpty()) {
            Long current = queue.remove();
            descendants.add(current);
            List<Long> children = childrenByParent.get(current);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return descendants;
    }
}
