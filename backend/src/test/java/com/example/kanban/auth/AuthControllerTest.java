package com.example.kanban.auth;

import com.example.kanban.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    private static final Path DATABASE_PATH = createDatabasePath();

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath());
    }

    @Test
    void seedsSuperAdministratorWhenUsersTableIsEmpty() {
        Integer count = jdbc.queryForObject(
                "select count(*) from users where username = 'admin' and super_admin = 1",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void loginReturnsCurrentUserAndSession() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.superAdmin").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(request().sessionAttribute(AuthController.SESSION_USER_ID, 1L))
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    void invalidLoginReturnsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCurrentUserForAuthenticatedSession() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.displayName").value("超级管理员"))
                .andExpect(jsonPath("$.superAdmin").value(true));
    }

    @Test
    void meReturnsAvatarUrlWhenCurrentUserHasAvatar() throws Exception {
        MockHttpSession session = loginAsAdmin();
        jdbc.update(
                "update users set avatar_data = ?, avatar_content_type = ?, avatar_updated_at = '2026-06-29 15:00:00' where username = 'admin'",
                new byte[] {1, 2, 3},
                "image/png");

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/api/users/me/avatar?v=20260629150000"));
    }

    @Test
    void meWithoutSessionReturnsUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedApiRequestDoesNotCreateSession() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void meWithOldSessionAfterLogoutReturnsUnauthorized() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void securityContextWithoutSessionUserIdDoesNotAuthenticate() throws Exception {
        MockHttpSession session = loginAsAdmin();
        session.removeAttribute(AuthController.SESSION_USER_ID);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                staleAdminSecurityContext());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSessionUserIdDoesNotAuthenticate() throws Exception {
        MockHttpSession session = loginAsAdmin();
        session.setAttribute(AuthController.SESSION_USER_ID, 999999L);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                staleAdminSecurityContext());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession loginAsAdmin() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttribute(AuthController.SESSION_USER_ID, 1L))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private SecurityContextImpl staleAdminSecurityContext() {
        UserRepository.UserRecord user = new UserRepository.UserRecord(
                1L,
                "admin",
                "超级管理员",
                "unused",
                true);
        return new SecurityContextImpl(SecurityConfig.authenticationFor(user));
    }

    private static Path createDatabasePath() {
        try {
            Path directory = Files.createTempDirectory("kanban-auth-");
            return directory.resolve("auth-controller.sqlite3");
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
