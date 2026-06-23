package com.example.kanban.support;

import com.example.kanban.users.UserService;
import com.example.kanban.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {
    private static final Path DATABASE_PATH = createDatabasePath();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath());
    }

    @BeforeEach
    void resetDatabase() {
        jdbc.update("delete from tasks");
        jdbc.update("delete from sprints");
        jdbc.update("delete from team_memberships");
        jdbc.update("delete from teams");
        jdbc.update("delete from system_settings");
        jdbc.update("delete from users");
        userService.seedAdminIfUsersTableIsEmpty();
    }

    protected MockHttpSession loginAsAdmin() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }

    protected MockHttpSession createPlainMemberSession() throws Exception {
        createPlainMemberUser("member", "普通成员");
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member\",\"password\":\"member123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }

    protected long createPlainMemberUser(String username, String displayName) {
        return userRepository.create(username, displayName, passwordEncoder.encode("member123"), false);
    }

    protected String json(String content) {
        return content;
    }

    protected long readId(String content) throws IOException {
        return objectMapper.readTree(content).path("id").asLong();
    }

    private static Path createDatabasePath() {
        try {
            Path directory = Files.createTempDirectory("kanban-integration-");
            return directory.resolve("integration.sqlite3");
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static class Fixture {
        public long teamId;
        public long rootTeamId;
        public long childTeamId;
        public long sprintId;
        public long memberUserId;
        public long otherUserId;
        public long taskId;
        public MockHttpSession adminSession;
        public MockHttpSession memberSession;
    }
}
