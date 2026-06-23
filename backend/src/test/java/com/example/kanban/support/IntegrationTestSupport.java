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
        return loginAsUser("member", "member123");
    }

    protected MockHttpSession loginAsUser(String username, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }

    protected long createPlainMemberUser(String username, String displayName) {
        return userRepository.create(username, displayName, passwordEncoder.encode("member123"), false);
    }

    protected Fixture createTeamWithMember() throws Exception {
        Fixture fixture = new Fixture();
        fixture.adminSession = loginAsAdmin();
        fixture.adminUserId = userRepository.findByUsername("admin").get().getId();
        fixture.memberUserId = createPlainMemberUser("team-member", "团队成员");
        fixture.otherUserId = createPlainMemberUser("outsider", "其他成员");
        fixture.teamId = createTeam("研发部", null, fixture.adminUserId);
        jdbc.update(
                "insert into team_memberships (team_id, user_id, role) values (?, ?, ?)",
                fixture.teamId,
                fixture.adminUserId,
                "TEAM_CREATOR");
        jdbc.update(
                "insert into team_memberships (team_id, user_id, role) values (?, ?, ?)",
                fixture.teamId,
                fixture.memberUserId,
                "TEAM_MEMBER");
        fixture.memberSession = loginAsUser("team-member", "member123");
        return fixture;
    }

    protected Fixture createManagedTeam() throws Exception {
        Fixture fixture = new Fixture();
        fixture.adminSession = loginAsAdmin();
        fixture.adminUserId = userRepository.findByUsername("admin").get().getId();
        fixture.teamId = createTeam("研发部", null, fixture.adminUserId);
        jdbc.update(
                "insert into team_memberships (team_id, user_id, role) values (?, ?, ?)",
                fixture.teamId,
                fixture.adminUserId,
                "TEAM_CREATOR");
        return fixture;
    }

    protected Fixture createTeamTreeWithSprintAndAssignees() throws Exception {
        Fixture fixture = new Fixture();
        fixture.adminSession = loginAsAdmin();
        fixture.adminUserId = userRepository.findByUsername("admin").get().getId();
        fixture.memberUserId = createPlainMemberUser("board-member", "看板成员");
        fixture.otherUserId = createPlainMemberUser("board-outsider", "其他成员");
        fixture.rootTeamId = createTeam("研发部", null, fixture.adminUserId);
        fixture.childTeamId = createTeam("平台组", fixture.rootTeamId, fixture.adminUserId);
        fixture.teamId = fixture.childTeamId;
        jdbc.update(
                "insert into team_memberships (team_id, user_id, role) values (?, ?, ?)",
                fixture.rootTeamId,
                fixture.adminUserId,
                "TEAM_CREATOR");
        jdbc.update(
                "insert into team_memberships (team_id, user_id, role) values (?, ?, ?)",
                fixture.childTeamId,
                fixture.memberUserId,
                "TEAM_MEMBER");
        fixture.sprintId = createSprint(fixture.childTeamId, "2026 Q3 冲刺");
        fixture.memberSession = loginAsUser("board-member", "member123");
        return fixture;
    }

    private long createTeam(String name, Long parentId, long createdBy) {
        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            java.sql.PreparedStatement statement = connection.prepareStatement(
                    "insert into teams (name, parent_id, created_by) values (?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            if (parentId == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, parentId);
            }
            statement.setLong(3, createdBy);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not read generated team id");
        }
        return key.longValue();
    }

    private long createSprint(long teamId, String name) {
        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            java.sql.PreparedStatement statement = connection.prepareStatement(
                    "insert into sprints (team_id, name) values (?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, teamId);
            statement.setString(2, name);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not read generated sprint id");
        }
        return key.longValue();
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
        public long adminUserId;
        public long memberUserId;
        public long otherUserId;
        public long taskId;
        public MockHttpSession adminSession;
        public MockHttpSession memberSession;
    }
}
