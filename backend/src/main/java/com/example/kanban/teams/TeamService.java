package com.example.kanban.teams;

import com.example.kanban.tasks.TaskRepository;
import com.example.kanban.sprints.SprintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final MembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;

    public TeamService(
            TeamRepository teamRepository,
            MembershipRepository membershipRepository,
            AuthorizationService authorizationService,
            TaskRepository taskRepository,
            SprintRepository sprintRepository) {
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
        this.taskRepository = taskRepository;
        this.sprintRepository = sprintRepository;
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
        List<Long> teamIds = authorizationService.descendantTeamIds(id);
        if (teamIds.isEmpty()) {
            return;
        }
        taskRepository.hardDeleteByTeamIds(teamIds);
        sprintRepository.deleteByTeamIds(teamIds);

        for (int index = teamIds.size() - 1; index >= 0; index--) {
            teamRepository.delete(teamIds.get(index));
        }
    }
}
