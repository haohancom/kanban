package com.example.kanban.sprints;

import com.example.kanban.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SprintControllerTest extends IntegrationTestSupport {
    @Test
    void adminCreatesAndRenamesCustomSprint() throws Exception {
        Fixture fixture = createManagedTeam();

        String created = mvc.perform(post("/api/teams/" + fixture.teamId + "/sprints")
                        .session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"2026 Q3 冲刺\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("2026 Q3 冲刺"))
                .andReturn().getResponse().getContentAsString();

        long sprintId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(patch("/api/sprints/" + sprintId).session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"2026 Q3 Sprint A\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("2026 Q3 Sprint A"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void adminPartiallyUpdatesSprintFields() throws Exception {
        Fixture fixture = createManagedTeam();
        long sprintId = createSprint(fixture.teamId, "保留名称", true);

        mvc.perform(patch("/api/sprints/" + sprintId).session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("保留名称"))
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(patch("/api/sprints/" + sprintId).session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"只改名称\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("只改名称"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void teamMemberListsTeamSprints() throws Exception {
        Fixture fixture = createTeamWithMember();
        createSprint(fixture.teamId, "已有冲刺", false);

        mvc.perform(get("/api/teams/" + fixture.teamId + "/sprints").session(fixture.memberSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("已有冲刺"))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void childTeamListsAncestorSprints() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        createSprint(fixture.rootTeamId, "父团队 Sprint", true);

        mvc.perform(get("/api/teams/" + fixture.childTeamId + "/sprints").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("2026 Q3 冲刺", "父团队 Sprint")));
    }

    @Test
    void teamMemberCannotCreateOrRenameSprints() throws Exception {
        Fixture fixture = createTeamWithMember();
        long sprintId = createSprint(fixture.teamId, "已有冲刺", true);

        mvc.perform(post("/api/teams/" + fixture.teamId + "/sprints")
                        .session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"成员冲刺\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/sprints/" + sprintId).session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"成员改名\",\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    private long createSprint(long teamId, String name, boolean active) {
        jdbc.update(
                "insert into sprints (team_id, name, active) values (?, ?, ?)",
                teamId,
                name,
                active ? 1 : 0);
        Long id = jdbc.queryForObject(
                "select id from sprints where team_id = ? and name = ?",
                Long.class,
                teamId,
                name);
        if (id == null) {
            throw new IllegalStateException("Missing sprint");
        }
        return id;
    }
}
