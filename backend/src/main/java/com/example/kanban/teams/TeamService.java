package com.example.kanban.teams;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final MembershipRepository membershipRepository;

    public TeamService(TeamRepository teamRepository, MembershipRepository membershipRepository) {
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public TeamRepository.TeamRecord createTeam(String name, Long parentId, long createdBy) {
        long id = teamRepository.create(name, parentId, createdBy);
        membershipRepository.create(id, createdBy, TeamRole.TEAM_CREATOR);
        return teamRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Created team cannot be loaded"));
    }

    @Transactional
    public void deleteTeam(long id) {
        teamRepository.delete(id);
    }
}
