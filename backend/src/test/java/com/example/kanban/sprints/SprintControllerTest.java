package com.example.kanban.sprints;

import com.example.kanban.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

        mvc.perform(delete("/api/sprints/" + sprintId).session(fixture.memberSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdministratorCanDeleteSprintFromAnyTeamContext() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        long parentSprintId = createSprint(fixture.rootTeamId, "父团队 Sprint", true);

        mvc.perform(delete("/api/sprints/" + parentSprintId)
                        .session(fixture.adminSession)
                        .param("teamId", String.valueOf(fixture.childTeamId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void superAdministratorCanUpdateSprintFromAnyTeamContext() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        long parentSprintId = createSprint(fixture.rootTeamId, "父团队 Sprint", true);

        mvc.perform(patch("/api/sprints/" + parentSprintId)
                        .session(fixture.adminSession)
                        .param("teamId", String.valueOf(fixture.childTeamId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"父团队 Sprint 已更新\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("父团队 Sprint 已更新"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void normalTeamAdminCannotUpdateSprintFromDifferentTeamContext() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        long normalAdminUserId = createPlainMemberUser("team-admin", "团队管理员");
        jdbc.update(
                "insert into team_memberships (team_id, user_id, role) values (?, ?, ?)",
                fixture.rootTeamId,
                normalAdminUserId,
                "TEAM_ADMIN");
        MockHttpSession normalAdminSession = loginAsUser("team-admin", "member123");
        long parentSprintId = createSprint(fixture.rootTeamId, "父团队 Sprint", true);

        mvc.perform(patch("/api/sprints/" + parentSprintId)
                        .session(normalAdminSession)
                        .param("teamId", String.valueOf(fixture.childTeamId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void normalTeamAdminCannotDeleteSprintFromDifferentTeamContext() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        long normalAdminUserId = createPlainMemberUser("team-admin-2", "团队管理员2");
        jdbc.update(
                "insert into team_memberships (team_id, user_id, role) values (?, ?, ?)",
                fixture.rootTeamId,
                normalAdminUserId,
                "TEAM_ADMIN");
        MockHttpSession normalAdminSession = loginAsUser("team-admin-2", "member123");
        long parentSprintId = createSprint(fixture.rootTeamId, "父团队 Sprint", true);

        mvc.perform(delete("/api/sprints/" + parentSprintId)
                        .session(normalAdminSession)
                        .param("teamId", String.valueOf(fixture.childTeamId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingChildSprintDoesNotAffectParentSprint() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        long parentSprintId = createSprint(fixture.rootTeamId, "父团队 Sprint", true);

        mvc.perform(delete("/api/sprints/" + fixture.sprintId).session(fixture.adminSession))
                .andExpect(status().isNoContent());

        assertThat(
                jdbc.queryForObject("select count(*) from sprints where id = ?", Integer.class, fixture.sprintId))
                .isEqualTo(0);
        assertThat(
                jdbc.queryForObject("select count(*) from sprints where id = ?", Integer.class, parentSprintId))
                .isEqualTo(1);
    }

    @Test
    void deletingChildSprintInChildTeamContextDoesNotAffectParentSprint() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        long parentSprintId = createSprint(fixture.rootTeamId, "父团队 Sprint", true);

        mvc.perform(delete("/api/sprints/" + fixture.sprintId)
                        .session(fixture.adminSession)
                        .param("teamId", String.valueOf(fixture.childTeamId)))
                .andExpect(status().isNoContent());

        assertThat(
                jdbc.queryForObject("select count(*) from sprints where id = ?", Integer.class, fixture.sprintId))
                .isEqualTo(0);
        assertThat(
                jdbc.queryForObject("select count(*) from sprints where id = ?", Integer.class, parentSprintId))
                .isEqualTo(1);
    }

    @Test
    void adminCanDeleteSprintAndReleaseBoundTasks() throws Exception {
        Fixture fixture = createManagedTeam();
        long sprintId = createSprint(fixture.teamId, "可删除冲刺", true);

        org.springframework.jdbc.support.GeneratedKeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            java.sql.PreparedStatement statement = connection.prepareStatement(
                    "insert into tasks (team_id, title, created_by, sprint_id) values (?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, fixture.teamId);
            statement.setString(2, "Sprint 关联任务");
            statement.setLong(3, fixture.adminUserId);
            statement.setLong(4, sprintId);
            return statement;
        }, keyHolder);
        Number taskKey = keyHolder.getKey();
        if (taskKey == null) {
            throw new IllegalStateException("Could not read generated task id");
        }
        long taskId = taskKey.longValue();

        mvc.perform(delete("/api/sprints/" + sprintId).session(fixture.adminSession))
                .andExpect(status().isNoContent());

        Integer remaining = jdbc.queryForObject("select count(*) from sprints where id = ?", Integer.class, sprintId);
        assertThat(remaining).isEqualTo(0);

        Integer detachedTasks = jdbc.queryForObject(
                "select count(*) from tasks where id = ? and sprint_id is null",
                Integer.class,
                taskId);
        assertThat(detachedTasks).isEqualTo(1);
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
