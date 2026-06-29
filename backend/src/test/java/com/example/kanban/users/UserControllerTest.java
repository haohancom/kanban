package com.example.kanban.users;

import com.example.kanban.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest extends IntegrationTestSupport {
    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

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
    void currentUserCanUpdateDisplayName() throws Exception {
        MockHttpSession member = createPlainMemberSession();

        mvc.perform(patch("/api/users/me")
                        .session(member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"新名字\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("新名字"))
                .andExpect(jsonPath("$.username").value("member"));
    }

    @Test
    void regularUserMustProvideCurrentPasswordWhenChangingPassword() throws Exception {
        MockHttpSession member = createPlainMemberSession();

        mvc.perform(patch("/api/users/me/password").session(member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"changed123\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/users/me/password").session(member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"changed123\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/users/me/password").session(member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"member123\",\"newPassword\":\"changed123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("普通成员"));

        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"member\",\"password\":\"changed123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void superAdministratorCanChangeOwnPasswordWithoutCurrentPassword() throws Exception {
        MockHttpSession admin = loginAsAdmin();

        mvc.perform(patch("/api/users/me/password").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"changed123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("超级管理员"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"changed123\"}"))
                .andExpect(status().isOk());
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
    void demotingSuperAdministratorIsAtomic() {
        long firstAdminId = userRepository.findByUsername("admin").get().getId();
        long secondAdminId = userRepository.create(
                "second-admin",
                "第二管理员",
                "unused-password-hash",
                true);

        assertThat(userRepository.demoteSuperAdministratorIfAnotherExists(firstAdminId)).isEqualTo(1);
        assertThat(userRepository.demoteSuperAdministratorIfAnotherExists(secondAdminId)).isEqualTo(0);
    }

    @Test
    void currentUserUploadsFetchesReplacesAndDeletesAvatar() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        byte[] firstAvatar = new byte[] {1, 2, 3};
        byte[] secondAvatar = new byte[] {4, 5};

        String firstUpload = mvc.perform(multipart("/api/users/me/avatar")
                        .file(new MockMultipartFile("file", "avatar.png", "image/png", firstAvatar))
                        .session(admin)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(startsWith("/api/users/me/avatar?v=")))
                .andReturn().getResponse().getContentAsString();
        String firstAvatarUrl = objectMapper.readTree(firstUpload).path("avatarUrl").asText();

        mvc.perform(get("/api/users/me/avatar").session(admin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(firstAvatar));

        String secondUpload = mvc.perform(multipart("/api/users/me/avatar")
                        .file(new MockMultipartFile("file", "avatar.jpg", "image/jpeg", secondAvatar))
                        .session(admin)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(startsWith("/api/users/me/avatar?v=")))
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(secondUpload).path("avatarUrl").asText()).isNotEqualTo(firstAvatarUrl);

        mvc.perform(get("/api/users/me/avatar").session(admin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(content().bytes(secondAvatar));

        mvc.perform(delete("/api/users/me/avatar").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());

        mvc.perform(get("/api/users/me/avatar").session(admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void avatarUploadRejectsInvalidFiles() throws Exception {
        MockHttpSession admin = loginAsAdmin();

        mvc.perform(multipart("/api/users/me/avatar")
                        .file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0]))
                        .session(admin)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest());

        mvc.perform(multipart("/api/users/me/avatar")
                        .file(new MockMultipartFile("file", "avatar.txt", "text/plain", new byte[] {1}))
                        .session(admin)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest());

        mvc.perform(multipart("/api/users/me/avatar")
                        .file(new MockMultipartFile("file", "large.png", "image/png", new byte[2 * 1024 * 1024 + 1]))
                        .session(admin)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest());
    }

    @Test
    void avatarEndpointsRequireAuthentication() throws Exception {
        mvc.perform(patch("/api/users/me").contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"x\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(
                        patch("/api/users/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"newPassword\":\"changed123\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(multipart("/api/users/me/avatar")
                        .file(new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1}))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/users/me/avatar"))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/users/me/avatar"))
                .andExpect(status().isUnauthorized());
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
