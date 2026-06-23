package com.example.kanban.users;

import com.example.kanban.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest extends IntegrationTestSupport {
    @Autowired
    MockMvc mvc;

    @Test
    void superAdministratorCreatesUserAndResetsPassword() throws Exception {
        MockHttpSession admin = loginAsAdmin();

        String created = mvc.perform(post("/api/users").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"wang\",\"displayName\":\"小王\",\"password\":\"secret123\",\"superAdmin\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("wang"))
                .andExpect(jsonPath("$.displayName").value("小王"))
                .andExpect(jsonPath("$.superAdmin").value(false))
                .andReturn().getResponse().getContentAsString();

        long userId = readId(created);

        mvc.perform(patch("/api/users/" + userId + "/password").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"changed123\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"wang\",\"password\":\"changed123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void superAdministratorListsAndUpdatesUsers() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        long userId = createPlainMemberUser("zhang", "小张");

        mvc.perform(get("/api/users").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[1].username").value("zhang"))
                .andExpect(jsonPath("$[1].displayName").value("小张"))
                .andExpect(jsonPath("$[1].superAdmin").value(false))
                .andExpect(jsonPath("$[1].passwordHash").doesNotExist());

        mvc.perform(patch("/api/users/" + userId).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"张三\",\"superAdmin\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("zhang"))
                .andExpect(jsonPath("$.displayName").value("张三"))
                .andExpect(jsonPath("$.superAdmin").value(true));
    }

    @Test
    void duplicateUsernameReturnsConflict() throws Exception {
        MockHttpSession admin = loginAsAdmin();

        mvc.perform(post("/api/users").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"displayName\":\"另一个管理员\",\"password\":\"secret123\",\"superAdmin\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateRejectsBlankDisplayName() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        long userId = createPlainMemberUser("blank-name", "小白");

        mvc.perform(patch("/api/users/" + userId).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotDemoteOnlySuperAdministrator() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        String currentUser = mvc.perform(get("/api/auth/me").session(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(patch("/api/users/" + readId(currentUser)).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"superAdmin\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void nonSuperAdministratorCannotUseUserAdministrationEndpoints() throws Exception {
        MockHttpSession member = createPlainMemberSession();

        mvc.perform(get("/api/users").session(member))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/users").session(member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"li\",\"displayName\":\"小李\",\"password\":\"secret123\",\"superAdmin\":false}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/users/1").session(member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"成员改名\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/users/1/password").session(member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"changed123\"}"))
                .andExpect(status().isForbidden());
    }
}
