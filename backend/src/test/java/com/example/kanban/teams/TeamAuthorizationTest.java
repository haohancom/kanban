package com.example.kanban.teams;

import com.example.kanban.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamAuthorizationTest extends IntegrationTestSupport {
    @SpyBean
    MembershipRepository membershipRepository;

    @Test
    void creatorCanCreateSubTeamAndSeeTree() throws Exception {
        MockHttpSession session = loginAsAdmin();

        String root = mvc.perform(post("/api/teams").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"研发部\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("研发部"))
                .andExpect(jsonPath("$.role").value("TEAM_CREATOR"))
                .andReturn().getResponse().getContentAsString();

        long rootId = readJsonLong(root, "$.id");

        mvc.perform(post("/api/teams").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"平台组\",\"parentId\":" + rootId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(rootId))
                .andExpect(jsonPath("$.role").value("TEAM_CREATOR"));

        mvc.perform(get("/api/teams").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("研发部"))
                .andExpect(jsonPath("$[0].role").value("TEAM_CREATOR"))
                .andExpect(jsonPath("$[0].children[0].name").value("平台组"));
    }

    @Test
    void teamAdministratorManagesMembersAndTeamDetails() throws Exception {
        Fixture fixture = createTeamWithMember();
        long memberMembershipId = moveMembershipIdAwayFromUserId(fixture.teamId, fixture.memberUserId, 10000);

        mvc.perform(patch("/api/teams/" + fixture.teamId + "/members/" + memberMembershipId)
                        .session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEAM_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEAM_ADMIN"));

        mvc.perform(patch("/api/teams/" + fixture.teamId).session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"交付组\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("交付组"));

        String createdMembership = mvc.perform(post("/api/teams/" + fixture.teamId + "/members").session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + fixture.otherUserId + ",\"role\":\"TEAM_MEMBER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(fixture.otherUserId))
                .andExpect(jsonPath("$.role").value("TEAM_MEMBER"))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/teams/" + fixture.teamId + "/members").session(fixture.memberSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", containsInAnyOrder(
                        (int) fixture.adminUserId,
                        (int) fixture.memberUserId,
                        (int) fixture.otherUserId)));

        long otherMembershipId = readJsonLong(createdMembership, "$.id") + 20000;
        jdbc.update(
                "update team_memberships set id = ? where team_id = ? and user_id = ?",
                otherMembershipId,
                fixture.teamId,
                fixture.otherUserId);

        mvc.perform(delete("/api/teams/" + fixture.teamId + "/members/" + otherMembershipId)
                        .session(fixture.memberSession))
                .andExpect(status().isNoContent());
    }

    @Test
    void memberCannotManageMemberships() throws Exception {
        Fixture fixture = createTeamWithMember();

        mvc.perform(post("/api/teams/" + fixture.teamId + "/members").session(fixture.memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + fixture.otherUserId + ",\"role\":\"TEAM_MEMBER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingChildTeamWithMissingParentReturnsNotFound() throws Exception {
        MockHttpSession admin = loginAsAdmin();

        mvc.perform(post("/api/teams").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"幽灵组\",\"parentId\":99999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void failedCreatorMembershipRollsBackTeamCreation() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        doThrow(new IllegalStateException("boom"))
                .when(membershipRepository)
                .create(anyLong(), anyLong(), eq(TeamRole.TEAM_CREATOR));

        try {
            mvc.perform(post("/api/teams").session(admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"孤儿组\"}"));
        } catch (Exception ignored) {
            // The rollback assertion below is the behavior under test.
        }

        Integer count = jdbc.queryForObject(
                "select count(*) from teams where name = '孤儿组'",
                Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void membersOnlySeeTheirVisibleTeamTree() throws Exception {
        Fixture fixture = createTeamWithMember();

        mvc.perform(get("/api/teams").session(fixture.memberSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fixture.teamId))
                .andExpect(jsonPath("$[0].children").isEmpty());

        MockHttpSession outsider = loginAsUser("outsider", "member123");
        mvc.perform(get("/api/teams").session(outsider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(get("/api/teams/" + fixture.teamId + "/members").session(outsider))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorDeletesOnlyEmptyTeams() throws Exception {
        Fixture fixture = createTeamWithMember();

        String child = mvc.perform(post("/api/teams").session(fixture.adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"子组\",\"parentId\":" + fixture.teamId + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long childId = readJsonLong(child, "$.id");

        mvc.perform(delete("/api/teams/" + fixture.teamId).session(fixture.adminSession))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/teams/" + childId).session(fixture.adminSession))
                .andExpect(status().isNoContent());
    }

    @Test
    void failedTeamDeleteKeepsMemberships() throws Exception {
        Fixture fixture = createTeamWithMember();
        jdbc.execute("create trigger fail_team_delete before delete on teams "
                + "when old.id = " + fixture.teamId + " "
                + "begin select raise(abort, 'fail team delete'); end");

        try {
            try {
                mvc.perform(delete("/api/teams/" + fixture.teamId).session(fixture.adminSession));
            } catch (Exception ignored) {
                // The rollback assertion below is the behavior under test.
            }

            Integer count = jdbc.queryForObject(
                    "select count(*) from team_memberships where team_id = ?",
                    Integer.class,
                    fixture.teamId);
            assertThat(count).isEqualTo(2);
        } finally {
            jdbc.execute("drop trigger if exists fail_team_delete");
        }
    }

    private long readJsonLong(String json, String path) {
        Number value = JsonPath.read(json, path);
        return value.longValue();
    }

    private long moveMembershipIdAwayFromUserId(long teamId, long userId, long offset) {
        long membershipId = membershipIdFor(teamId, userId);
        long movedMembershipId = membershipId + offset;
        jdbc.update(
                "update team_memberships set id = ? where id = ?",
                movedMembershipId,
                membershipId);
        return movedMembershipId;
    }

    private long membershipIdFor(long teamId, long userId) {
        Long membershipId = jdbc.queryForObject(
                "select id from team_memberships where team_id = ? and user_id = ?",
                Long.class,
                teamId,
                userId);
        if (membershipId == null) {
            throw new IllegalStateException("Missing membership");
        }
        return membershipId;
    }
}
