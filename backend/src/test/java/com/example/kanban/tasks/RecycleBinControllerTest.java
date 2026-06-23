package com.example.kanban.tasks;

import com.example.kanban.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecycleBinControllerTest extends IntegrationTestSupport {
    @Test
    void softDeletesRestoresAndPermanentlyDeletesTask() throws Exception {
        Fixture fixture = createTaskFixture();

        mvc.perform(delete("/api/tasks/" + fixture.taskId).session(fixture.adminSession))
                .andExpect(status().isOk());

        mvc.perform(get("/api/teams/" + fixture.teamId + "/board/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(get("/api/teams/" + fixture.teamId + "/recycle-bin/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) fixture.taskId))
                .andExpect(jsonPath("$[0].deletedAt").isNotEmpty());

        mvc.perform(post("/api/recycle-bin/tasks/" + fixture.taskId + "/restore").session(fixture.adminSession))
                .andExpect(status().isOk());

        mvc.perform(get("/api/teams/" + fixture.teamId + "/board/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) fixture.taskId));

        mvc.perform(delete("/api/tasks/" + fixture.taskId).session(fixture.adminSession))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/recycle-bin/tasks/" + fixture.taskId).session(fixture.adminSession))
                .andExpect(status().isOk());

        mvc.perform(get("/api/teams/" + fixture.teamId + "/recycle-bin/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void bulkDeletePermanentlyDeletesSelectedTasksOnly() throws Exception {
        Fixture fixture = createTaskFixture();
        MockHttpSession managerSession = createTeamAdminSession(fixture.teamId);
        long selectedTaskId = createTaskThroughApi(fixture, "批量删除任务");
        long remainingTaskId = createTaskThroughApi(fixture, "保留任务");

        softDelete(fixture, fixture.taskId);
        softDelete(fixture, selectedTaskId);
        softDelete(fixture, remainingTaskId);

        mvc.perform(post("/api/recycle-bin/tasks/bulk-delete").session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskIds\":[" + fixture.taskId + "," + selectedTaskId + "]}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/teams/" + fixture.teamId + "/recycle-bin/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) remainingTaskId));
    }

    @Test
    void deleteAllPermanentlyDeletesDeletedTasksInTeamTreeOnly() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        MockHttpSession managerSession = createTeamAdminSession(fixture.rootTeamId);
        Fixture outsideFixture = createTaskFixture();
        long rootTaskId = createTaskThroughApi(fixture, fixture.rootTeamId, "父团队已删任务");
        long childTaskId = createTaskThroughApi(fixture, fixture.childTeamId, "子团队已删任务");
        long activeTaskId = createTaskThroughApi(fixture, fixture.childTeamId, "仍在看板任务");

        softDelete(fixture, rootTaskId);
        softDelete(fixture, childTaskId);
        softDelete(outsideFixture, outsideFixture.taskId);

        mvc.perform(delete("/api/teams/" + fixture.rootTeamId + "/recycle-bin/tasks").session(managerSession))
                .andExpect(status().isOk());

        mvc.perform(get("/api/teams/" + fixture.rootTeamId + "/recycle-bin/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(get("/api/teams/" + outsideFixture.teamId + "/recycle-bin/tasks").session(outsideFixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) outsideFixture.taskId));

        mvc.perform(get("/api/teams/" + fixture.rootTeamId + "/board/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) activeTaskId));
    }

    @Test
    void teamAdministratorCanSoftDeleteRestoreAndPermanentlyDeleteTask() throws Exception {
        Fixture fixture = createTaskFixture();
        MockHttpSession managerSession = createTeamAdminSession(fixture.teamId);

        mvc.perform(delete("/api/tasks/" + fixture.taskId).session(managerSession))
                .andExpect(status().isOk());

        mvc.perform(post("/api/recycle-bin/tasks/" + fixture.taskId + "/restore").session(managerSession))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/tasks/" + fixture.taskId).session(managerSession))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/recycle-bin/tasks/" + fixture.taskId).session(managerSession))
                .andExpect(status().isOk());

        mvc.perform(get("/api/teams/" + fixture.teamId + "/recycle-bin/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void permanentDeletesRequireTeamManagementPermission() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        long taskId = createTaskThroughApi(fixture, fixture.childTeamId, "成员不能永久删除");
        softDelete(fixture, taskId);

        mvc.perform(delete("/api/recycle-bin/tasks/" + taskId).session(fixture.memberSession))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/recycle-bin/tasks/bulk-delete").session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskIds\":[" + taskId + "]}"))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/teams/" + fixture.rootTeamId + "/recycle-bin/tasks").session(fixture.memberSession))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/teams/" + fixture.rootTeamId + "/recycle-bin/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) taskId));
    }

    @Test
    void teamMemberCannotSoftDeleteAssignedTask() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        long taskId = createAssignedTaskThroughApi(fixture, "成员只可编辑不可删除");

        mvc.perform(delete("/api/tasks/" + taskId).session(fixture.memberSession))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/teams/" + fixture.rootTeamId + "/board/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) taskId));
    }

    @Test
    void normalTaskApisDoNotReadUpdateOrDeleteRecycledTasks() throws Exception {
        Fixture fixture = createTaskFixture();
        softDelete(fixture, fixture.taskId);

        mvc.perform(get("/api/tasks/" + fixture.taskId).session(fixture.adminSession))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/tasks/" + fixture.taskId).session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/tasks/" + fixture.taskId).session(fixture.adminSession))
                .andExpect(status().isNotFound());
    }

    private void softDelete(Fixture fixture, long taskId) throws Exception {
        mvc.perform(delete("/api/tasks/" + taskId).session(fixture.adminSession))
                .andExpect(status().isOk());
    }

    private MockHttpSession createTeamAdminSession(long teamId) throws Exception {
        long managerId = createPlainMemberUser("recycle-manager", "回收站管理员");
        jdbc.update(
                "insert into team_memberships (team_id, user_id, role) values (?, ?, ?)",
                teamId,
                managerId,
                "TEAM_ADMIN");
        return loginAsUser("recycle-manager", "member123");
    }

    private long createTaskThroughApi(Fixture fixture, String title) throws Exception {
        return createTaskThroughApi(fixture, fixture.teamId, title);
    }

    private long createAssignedTaskThroughApi(Fixture fixture, String title) throws Exception {
        String created = mvc.perform(post("/api/teams/" + fixture.childTeamId + "/tasks")
                        .session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"assigneeId\":" + fixture.memberUserId + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(created, "$.id")).longValue();
    }

    private long createTaskThroughApi(Fixture fixture, long teamId, String title) throws Exception {
        String created = mvc.perform(post("/api/teams/" + teamId + "/tasks")
                        .session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(created, "$.id")).longValue();
    }
}
