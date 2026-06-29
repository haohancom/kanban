package com.example.kanban.tasks;

import com.example.kanban.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BoardTaskControllerTest extends IntegrationTestSupport {
    @Test
    void parentBoardIncludesDescendantTasksAndAppliesFilters() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();

        mvc.perform(post("/api/teams/" + fixture.childTeamId + "/tasks")
                        .session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"接入登录\",\"status\":\"TODO\",\"sprintId\":" + fixture.sprintId
                                + ",\"assigneeId\":" + fixture.memberUserId
                                + ",\"remarks\":\"先做 session\",\"risks\":\"权限遗漏\"}"))
                .andExpect(status().isOk());

        insertDeletedTask(fixture);

        mvc.perform(get("/api/teams/" + fixture.rootTeamId + "/board/tasks")
                        .session(fixture.adminSession)
                        .param("subTeamId", String.valueOf(fixture.childTeamId))
                        .param("memberId", String.valueOf(fixture.memberUserId))
                        .param("status", "TODO")
                        .param("sprintId", String.valueOf(fixture.sprintId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("接入登录"))
                .andExpect(jsonPath("$[0].teamId").value((int) fixture.childTeamId))
                .andExpect(jsonPath("$[0].remarks").value("先做 session"))
                .andExpect(jsonPath("$[0].risks").value("权限遗漏"));
    }

    @Test
    void administratorCreatesReadsAndUpdatesTaskFields() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();

        String created = mvc.perform(post("/api/teams/" + fixture.childTeamId + "/tasks")
                        .session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"梳理接口\",\"description\":\"列出 MVP API\",\"status\":\"TODO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("梳理接口"))
                .andExpect(jsonPath("$.description").value("列出 MVP API"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn().getResponse().getContentAsString();
        long taskId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(get("/api/tasks/" + taskId).session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("梳理接口"));

        mvc.perform(patch("/api/tasks/" + taskId).session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"梳理接口 v2\",\"description\":\"补充过滤参数\","
                                + "\"remarks\":\"接口稳定\",\"risks\":\"字段遗漏\",\"status\":\"IN_PROGRESS\","
                                + "\"sprintId\":" + fixture.sprintId + ",\"assigneeId\":" + fixture.memberUserId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("梳理接口 v2"))
                .andExpect(jsonPath("$.description").value("补充过滤参数"))
                .andExpect(jsonPath("$.remarks").value("接口稳定"))
                .andExpect(jsonPath("$.risks").value("字段遗漏"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.sprintId").value((int) fixture.sprintId))
                .andExpect(jsonPath("$.assigneeId").value((int) fixture.memberUserId));
    }

    @Test
    void memberCreatesTasksInOwnTeamAndEditsOnlyAssignedTasks() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();

        String assigned = mvc.perform(post("/api/teams/" + fixture.childTeamId + "/tasks")
                        .session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"成员任务\",\"assigneeId\":" + fixture.memberUserId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdBy").value((int) fixture.memberUserId))
                .andReturn().getResponse().getContentAsString();
        long assignedTaskId = ((Number) JsonPath.read(assigned, "$.id")).longValue();

        mvc.perform(patch("/api/tasks/" + assignedTaskId).session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\",\"remarks\":\"我已处理\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.remarks").value("我已处理"));

        long otherTaskId = insertTaskAssignedToOtherUser(fixture);

        mvc.perform(patch("/api/tasks/" + otherTaskId).session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanAssignAncestorTeamSprintWhenUpdatingTask() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        long parentSprintId = createSprint(fixture.rootTeamId, "父团队 Sprint");

        String created = mvc.perform(post("/api/teams/" + fixture.childTeamId + "/tasks")
                        .session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"上级团队 sprint 任务\",\"sprintId\":" + parentSprintId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value((int) fixture.childTeamId))
                .andExpect(jsonPath("$.sprintId").value((int) parentSprintId))
                .andReturn().getResponse().getContentAsString();

        long taskId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(patch("/api/tasks/" + taskId).session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sprintId\":" + parentSprintId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId").value((int) parentSprintId));
    }

    @Test
    void memberCannotCreateTasksInVisibleDescendantTeamWithoutDirectMembership() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        jdbc.update(
                "insert into team_memberships (team_id, user_id, role) values (?, ?, ?)",
                fixture.rootTeamId,
                fixture.memberUserId,
                "TEAM_MEMBER");
        jdbc.update(
                "delete from team_memberships where team_id = ? and user_id = ?",
                fixture.childTeamId,
                fixture.memberUserId);

        mvc.perform(post("/api/teams/" + fixture.childTeamId + "/tasks")
                        .session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"子团队任务\",\"assigneeId\":" + fixture.memberUserId + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void outsiderAssignedToTaskCannotEditWithoutTeamVisibility() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();
        long taskId = insertTaskAssignedToOtherUser(fixture);
        MockHttpSession outsiderSession = loginAsUser("board-outsider", "member123");

        mvc.perform(patch("/api/tasks/" + taskId).session(outsiderSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isForbidden());
    }

    private void insertDeletedTask(Fixture fixture) {
        jdbc.update(
                "insert into tasks (team_id, title, status, sprint_id, assignee_id, created_by, deleted_at) "
                        + "values (?, ?, ?, ?, ?, ?, current_timestamp)",
                fixture.childTeamId,
                "已删除任务",
                "TODO",
                fixture.sprintId,
                fixture.memberUserId,
                fixture.adminUserId);
    }

    private long insertTaskAssignedToOtherUser(Fixture fixture) {
        jdbc.update(
                "insert into tasks (team_id, title, assignee_id, created_by) values (?, ?, ?, ?)",
                fixture.childTeamId,
                "其他成员任务",
                fixture.otherUserId,
                fixture.adminUserId);
        Long id = jdbc.queryForObject(
                "select id from tasks where title = ?",
                Long.class,
                "其他成员任务");
        if (id == null) {
            throw new IllegalStateException("Missing task");
        }
        return id;
    }
}
