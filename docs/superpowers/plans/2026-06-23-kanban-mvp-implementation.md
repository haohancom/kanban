# Kanban MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete but restrained Kanban MVP from the approved design: Java 8 backend, SQLite database, session authentication, role-based permissions, and React frontend.

**Architecture:** The backend is a Spring Boot 2.x REST API using Spring Security sessions and `JdbcTemplate` repositories over SQLite. The frontend is a React + Vite + TypeScript SPA that talks to the backend through a small typed API client and renders the authenticated kanban workspace.

**Tech Stack:** Java 8, Maven, Spring Boot 2.7.x, Spring Security, Spring JDBC, SQLite JDBC, JUnit 5, MockMvc, React, Vite, TypeScript, Vitest, React Testing Library.

---

## File Structure

Backend files live under `backend/`:

- `backend/pom.xml`: Maven build, Java 8 target, Spring Boot dependencies.
- `backend/src/main/java/com/example/kanban/KanbanApplication.java`: application entry point.
- `backend/src/main/resources/application.yml`: local SQLite datasource and session config.
- `backend/src/main/resources/schema.sql`: SQLite schema for users, teams, memberships, sprints, and tasks.
- `backend/src/main/java/com/example/kanban/common/*`: API errors, validation helpers, timestamp helpers.
- `backend/src/main/java/com/example/kanban/auth/*`: login/logout/me API, security config, current-user model.
- `backend/src/main/java/com/example/kanban/users/*`: user repository, service, DTOs, API.
- `backend/src/main/java/com/example/kanban/teams/*`: team tree, memberships, authorization rules, API.
- `backend/src/main/java/com/example/kanban/sprints/*`: sprint repository, service, DTOs, API.
- `backend/src/main/java/com/example/kanban/tasks/*`: board task repository, service, DTOs, API, recycle bin.
- `backend/src/test/java/com/example/kanban/*`: integration tests with MockMvc and isolated SQLite test databases.

Frontend files live under `frontend/`:

- `frontend/package.json`: Vite, React, TypeScript, Vitest scripts and dependencies.
- `frontend/src/api/*`: typed fetch client and endpoint modules.
- `frontend/src/auth/*`: auth context, login state, route guards.
- `frontend/src/components/*`: reusable shell, form, table, filter, dialog, and status components.
- `frontend/src/pages/*`: login, board, team administration, sprint management, user administration, recycle bin.
- `frontend/src/types.ts`: shared frontend types.
- `frontend/src/test/*`: test setup and API mocking helpers.

Root files:

- `README.md`: updated local development instructions and default admin credentials.
- `.gitignore`: add frontend build artifacts and local tool caches if needed.

## Task 1: Backend Scaffold and Database Schema

**Files:**

- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/example/kanban/KanbanApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/schema.sql`
- Create: `backend/src/test/java/com/example/kanban/ApplicationSmokeTest.java`

- [ ] **Step 1: Write the failing application smoke test**

```java
package com.example.kanban;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationSmokeTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void startsAndCreatesCoreTables() {
        Integer userTables = jdbcTemplate.queryForObject(
                "select count(*) from sqlite_master where type = 'table' and name = 'users'",
                Integer.class);
        Integer taskTables = jdbcTemplate.queryForObject(
                "select count(*) from sqlite_master where type = 'table' and name = 'tasks'",
                Integer.class);

        assertThat(userTables).isEqualTo(1);
        assertThat(taskTables).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run the test and verify it fails because the backend does not exist yet**

Run: `mvn -f backend/pom.xml test -Dtest=ApplicationSmokeTest`

Expected: FAIL with a missing `backend/pom.xml` or missing application class error.

- [ ] **Step 3: Add the Maven project and Spring Boot entry point**

Create `backend/pom.xml` with Spring Boot 2.7.x, Java 8, Web, Security, JDBC,
Validation, SQLite JDBC, and Spring Boot Test dependencies. Create
`KanbanApplication` with `SpringApplication.run(KanbanApplication.class, args)`.

Create `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:sqlite:kanban.sqlite3
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always
  session:
    timeout: 8h
server:
  port: 8080
kanban:
  seed-admin:
    username: admin
    password: admin123
    display-name: 超级管理员
```

Create `backend/src/main/resources/schema.sql` with idempotent table creation
for `users`, `teams`, `team_memberships`, `sprints`, and `tasks`. Include unique
indexes for `users.username` and `(team_id, user_id)` memberships.

- [ ] **Step 4: Run the test and verify it passes**

Run: `mvn -f backend/pom.xml test -Dtest=ApplicationSmokeTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/pom.xml backend/src/main/java backend/src/main/resources backend/src/test/java
git commit -m "feat: scaffold spring backend"
```

## Task 2: Authentication, Seed Admin, and Current User API

**Files:**

- Create: `backend/src/main/java/com/example/kanban/auth/AuthController.java`
- Create: `backend/src/main/java/com/example/kanban/auth/SecurityConfig.java`
- Create: `backend/src/main/java/com/example/kanban/auth/CurrentUser.java`
- Create: `backend/src/main/java/com/example/kanban/auth/LoginRequest.java`
- Create: `backend/src/main/java/com/example/kanban/users/UserRepository.java`
- Create: `backend/src/main/java/com/example/kanban/users/UserService.java`
- Create: `backend/src/test/java/com/example/kanban/auth/AuthControllerTest.java`
- Create: `backend/src/test/java/com/example/kanban/support/IntegrationTestSupport.java`

- [ ] **Step 1: Write failing auth tests**

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void seedsSuperAdministratorWhenUsersTableIsEmpty() {
        Integer count = jdbc.queryForObject(
                "select count(*) from users where username = 'admin' and super_admin = 1",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void loginReturnsCurrentUserAndSessionCookie() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.superAdmin").value(true))
                .andExpect(cookie().exists("JSESSIONID"));
    }

    @Test
    void invalidLoginReturnsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run auth tests and verify they fail for missing endpoints/services**

Run: `mvn -f backend/pom.xml test -Dtest=AuthControllerTest`

Expected: FAIL with missing `/api/auth/login` behavior.

- [ ] **Step 3: Implement minimal auth support**

Implement:

- `UserRepository`: create, find by username, find by id, list users.
- `UserService`: seed admin on startup with BCrypt password.
- `SecurityConfig`: disable CSRF for the JSON MVP API, allow `/api/auth/login`,
  require authentication for `/api/**`, and use HTTP session.
- `AuthController`: login by checking BCrypt hash, store current user id in the session,
  return `CurrentUser`, support logout and `/api/auth/me`.
- `IntegrationTestSupport`: test helper with `loginAsAdmin()`, `json(String)`,
  and `readId(String)` methods used by later MockMvc integration tests.
  Define a reusable `Fixture` class with public fields for `teamId`,
  `rootTeamId`, `childTeamId`, `sprintId`, `memberUserId`, `otherUserId`,
  `taskId`, `adminSession`, and `memberSession`; later tests only populate the
  fields they need.

Use this session attribute name consistently:

```java
public static final String SESSION_USER_ID = "KANBAN_USER_ID";
```

- [ ] **Step 4: Run auth tests and verify they pass**

Run: `mvn -f backend/pom.xml test -Dtest=AuthControllerTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/example/kanban/auth backend/src/main/java/com/example/kanban/users backend/src/test/java/com/example/kanban/auth backend/src/test/java/com/example/kanban/support
git commit -m "feat: add session authentication"
```

## Task 3: User Administration API

**Files:**

- Create: `backend/src/main/java/com/example/kanban/users/UserController.java`
- Create: `backend/src/main/java/com/example/kanban/users/UserDtos.java`
- Modify: `backend/src/main/java/com/example/kanban/users/UserRepository.java`
- Modify: `backend/src/main/java/com/example/kanban/users/UserService.java`
- Create: `backend/src/test/java/com/example/kanban/users/UserControllerTest.java`

- [ ] **Step 1: Write failing user administration tests**

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest extends IntegrationTestSupport {
    @Autowired MockMvc mvc;

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
    void nonSuperAdministratorCannotCreateUsers() throws Exception {
        MockHttpSession member = createPlainMemberSession();

        mvc.perform(post("/api/users").session(member)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"li\",\"displayName\":\"小李\",\"password\":\"secret123\",\"superAdmin\":false}"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run user API tests and verify they fail**

Run: `mvn -f backend/pom.xml test -Dtest=UserControllerTest`

Expected: FAIL with missing `/api/users` endpoints.

- [ ] **Step 3: Implement user administration**

Implement:

- `GET /api/users`: list users.
- `POST /api/users`: create user with BCrypt password.
- `PATCH /api/users/{id}`: update display name or super administrator flag.
- `PATCH /api/users/{id}/password`: reset password.
- Super administrator guard for every user administration endpoint.
- `createPlainMemberSession()` in `IntegrationTestSupport` for permission tests.

- [ ] **Step 4: Run user API tests and backend suite**

Run:

```bash
mvn -f backend/pom.xml test -Dtest=UserControllerTest
mvn -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/example/kanban/users backend/src/test/java/com/example/kanban/users backend/src/test/java/com/example/kanban/support
git commit -m "feat: add user administration api"
```

## Task 4: Teams, Memberships, and Authorization

**Files:**

- Create: `backend/src/main/java/com/example/kanban/teams/TeamRole.java`
- Create: `backend/src/main/java/com/example/kanban/teams/TeamRepository.java`
- Create: `backend/src/main/java/com/example/kanban/teams/MembershipRepository.java`
- Create: `backend/src/main/java/com/example/kanban/teams/AuthorizationService.java`
- Create: `backend/src/main/java/com/example/kanban/teams/TeamController.java`
- Create: `backend/src/main/java/com/example/kanban/teams/MembershipController.java`
- Create: `backend/src/test/java/com/example/kanban/teams/TeamAuthorizationTest.java`

- [ ] **Step 1: Write failing team and permission tests**

```java
@SpringBootTest
@AutoConfigureMockMvc
class TeamAuthorizationTest extends IntegrationTestSupport {
    @Autowired MockMvc mvc;

    @Test
    void creatorCanCreateSubTeamAndSeeTree() throws Exception {
        MockHttpSession session = loginAsAdmin();

        String root = mvc.perform(post("/api/teams").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"研发部\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long rootId = JsonPath.read(root, "$.id");

        mvc.perform(post("/api/teams").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"平台组\",\"parentId\":" + rootId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(rootId));

        mvc.perform(get("/api/teams").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("研发部"))
                .andExpect(jsonPath("$[0].children[0].name").value("平台组"));
    }

    @Test
    void memberCannotManageMemberships() throws Exception {
        Fixture fixture = createTeamWithMember();

        mvc.perform(post("/api/teams/" + fixture.teamId + "/members").session(fixture.memberSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + fixture.otherUserId + ",\"role\":\"TEAM_MEMBER\"}"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run team tests and verify they fail for missing APIs**

Run: `mvn -f backend/pom.xml test -Dtest=TeamAuthorizationTest`

Expected: FAIL with missing team endpoints.

- [ ] **Step 3: Implement teams, memberships, and permission checks**

Implement:

- `TeamRole` enum values: `TEAM_CREATOR`, `TEAM_ADMIN`, `TEAM_MEMBER`.
- Team CRUD for create, list visible tree, rename, and delete empty team.
- Membership CRUD with one membership per team/user.
- `AuthorizationService` methods:
  - `canViewTeamTree(userId, teamId)`
  - `canManageTeam(userId, teamId)`
  - `canManageMembers(userId, teamId)`
  - `descendantTeamIds(teamId)`
- Super administrator bypass for all team actions.
- Team creator membership inserted automatically when creating a team.
- `createTeamWithMember()` in `IntegrationTestSupport`; it creates a team,
  a member user, another user, a `TEAM_MEMBER` membership, and returns a
  `Fixture` with `teamId`, `memberSession`, and `otherUserId`.

- [ ] **Step 4: Run team tests and all backend tests**

Run:

```bash
mvn -f backend/pom.xml test -Dtest=TeamAuthorizationTest
mvn -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/example/kanban/teams backend/src/test/java/com/example/kanban/teams
git commit -m "feat: add teams and authorization"
```

## Task 5: Sprints API

**Files:**

- Create: `backend/src/main/java/com/example/kanban/sprints/SprintRepository.java`
- Create: `backend/src/main/java/com/example/kanban/sprints/SprintController.java`
- Create: `backend/src/main/java/com/example/kanban/sprints/SprintDtos.java`
- Create: `backend/src/test/java/com/example/kanban/sprints/SprintControllerTest.java`

- [ ] **Step 1: Write failing sprint tests**

```java
@SpringBootTest
@AutoConfigureMockMvc
class SprintControllerTest extends IntegrationTestSupport {
    @Autowired MockMvc mvc;

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

        long sprintId = JsonPath.read(created, "$.id");

        mvc.perform(patch("/api/sprints/" + sprintId).session(fixture.adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"2026 Q3 Sprint A\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("2026 Q3 Sprint A"))
                .andExpect(jsonPath("$.active").value(false));
    }
}
```

- [ ] **Step 2: Run sprint tests and verify they fail**

Run: `mvn -f backend/pom.xml test -Dtest=SprintControllerTest`

Expected: FAIL with missing sprint endpoints.

- [ ] **Step 3: Implement sprint repository and controller**

Implement sprint creation, listing by team, rename, and active flag updates.
Authorize all writes through `AuthorizationService.canManageTeam`.
Add `createManagedTeam()` in `IntegrationTestSupport`; it returns a `Fixture`
with `teamId` and `adminSession`.

- [ ] **Step 4: Run sprint tests and backend suite**

Run:

```bash
mvn -f backend/pom.xml test -Dtest=SprintControllerTest
mvn -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/example/kanban/sprints backend/src/test/java/com/example/kanban/sprints
git commit -m "feat: add sprint management api"
```

## Task 6: Board Tasks, Filters, and Task Editing

**Files:**

- Create: `backend/src/main/java/com/example/kanban/tasks/TaskStatus.java`
- Create: `backend/src/main/java/com/example/kanban/tasks/TaskRepository.java`
- Create: `backend/src/main/java/com/example/kanban/tasks/TaskController.java`
- Create: `backend/src/main/java/com/example/kanban/tasks/TaskDtos.java`
- Create: `backend/src/test/java/com/example/kanban/tasks/BoardTaskControllerTest.java`

- [ ] **Step 1: Write failing board tests**

```java
@SpringBootTest
@AutoConfigureMockMvc
class BoardTaskControllerTest extends IntegrationTestSupport {
    @Autowired MockMvc mvc;

    @Test
    void parentBoardIncludesDescendantTasksAndAppliesFilters() throws Exception {
        Fixture fixture = createTeamTreeWithSprintAndAssignees();

        mvc.perform(post("/api/teams/" + fixture.childTeamId + "/tasks")
                .session(fixture.adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"接入登录\",\"status\":\"TODO\",\"sprintId\":" + fixture.sprintId
                        + ",\"assigneeId\":" + fixture.memberUserId + ",\"remarks\":\"先做 session\",\"risks\":\"权限遗漏\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/teams/" + fixture.rootTeamId + "/board/tasks")
                .session(fixture.adminSession)
                .param("subTeamId", String.valueOf(fixture.childTeamId))
                .param("memberId", String.valueOf(fixture.memberUserId))
                .param("status", "TODO")
                .param("sprintId", String.valueOf(fixture.sprintId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("接入登录"))
                .andExpect(jsonPath("$[0].teamId").value((int) fixture.childTeamId))
                .andExpect(jsonPath("$[0].remarks").value("先做 session"))
                .andExpect(jsonPath("$[0].risks").value("权限遗漏"));
    }
}
```

- [ ] **Step 2: Run board tests and verify they fail**

Run: `mvn -f backend/pom.xml test -Dtest=BoardTaskControllerTest`

Expected: FAIL with missing task endpoints.

- [ ] **Step 3: Implement task status, repository, and controller**

Implement:

- `TaskStatus` enum values: `TODO`, `IN_PROGRESS`, `DONE`.
- Task creation for visible/manageable teams.
- Task detail lookup.
- Task patch updates for title, description, remarks, risks, status, sprint, assignee.
- Board query for a team and all descendants, excluding deleted tasks.
- Optional filters: `subTeamId`, `memberId`, `status`, `sprintId`.
- Member edit rule: members may update tasks assigned to themselves; administrators may update team-tree tasks.
- `createTeamTreeWithSprintAndAssignees()` in `IntegrationTestSupport`; it
  returns a `Fixture` with `rootTeamId`, `childTeamId`, `sprintId`,
  `memberUserId`, and `adminSession`.

- [ ] **Step 4: Run board tests and backend suite**

Run:

```bash
mvn -f backend/pom.xml test -Dtest=BoardTaskControllerTest
mvn -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/example/kanban/tasks backend/src/test/java/com/example/kanban/tasks
git commit -m "feat: add board task api"
```

## Task 7: Recycle Bin API

**Files:**

- Modify: `backend/src/main/java/com/example/kanban/tasks/TaskRepository.java`
- Modify: `backend/src/main/java/com/example/kanban/tasks/TaskController.java`
- Create: `backend/src/main/java/com/example/kanban/tasks/RecycleBinController.java`
- Create: `backend/src/test/java/com/example/kanban/tasks/RecycleBinControllerTest.java`

- [ ] **Step 1: Write failing recycle-bin tests**

```java
@SpringBootTest
@AutoConfigureMockMvc
class RecycleBinControllerTest extends IntegrationTestSupport {
    @Autowired MockMvc mvc;

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
                .andExpect(jsonPath("$[0].id").value((int) fixture.taskId));

        mvc.perform(post("/api/recycle-bin/tasks/" + fixture.taskId + "/restore").session(fixture.adminSession))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/tasks/" + fixture.taskId).session(fixture.adminSession))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/recycle-bin/tasks/" + fixture.taskId).session(fixture.adminSession))
                .andExpect(status().isOk());

        mvc.perform(get("/api/teams/" + fixture.teamId + "/recycle-bin/tasks").session(fixture.adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
```

- [ ] **Step 2: Run recycle-bin tests and verify they fail**

Run: `mvn -f backend/pom.xml test -Dtest=RecycleBinControllerTest`

Expected: FAIL with missing recycle-bin behavior.

- [ ] **Step 3: Implement recycle-bin behavior**

Implement soft deletion by setting `tasks.deleted_at`. Implement restore,
permanent delete by id, bulk permanent delete by selected ids, and delete all
for a team tree. Authorize permanent deletes through team management permission.
Add `createTaskFixture()` in `IntegrationTestSupport`; it returns a `Fixture`
with `teamId`, `taskId`, and `adminSession`.

- [ ] **Step 4: Run recycle-bin tests and backend suite**

Run:

```bash
mvn -f backend/pom.xml test -Dtest=RecycleBinControllerTest
mvn -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/example/kanban/tasks backend/src/test/java/com/example/kanban/tasks
git commit -m "feat: add task recycle bin"
```

## Task 8: Frontend Scaffold, API Client, and Auth Flow

**Files:**

- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/App.tsx`
- Create: `frontend/src/api/client.ts`
- Create: `frontend/src/auth/AuthContext.tsx`
- Create: `frontend/src/pages/LoginPage.tsx`
- Create: `frontend/src/test/setup.ts`
- Create: `frontend/src/auth/AuthContext.test.tsx`

- [ ] **Step 1: Write failing auth UI tests**

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import App from "../App";

describe("auth flow", () => {
  it("logs in and shows the authenticated shell", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo) => {
      const url = String(input);
      if (url.endsWith("/api/auth/me")) {
        return new Response("null", { status: 401 });
      }
      if (url.endsWith("/api/auth/login")) {
        return Response.json({ id: 1, username: "admin", displayName: "超级管理员", superAdmin: true, memberships: [] });
      }
      if (url.endsWith("/api/teams")) {
        return Response.json([]);
      }
      return new Response("{}", { status: 404 });
    }));

    render(<App />);
    await userEvent.type(await screen.findByLabelText("用户名"), "admin");
    await userEvent.type(screen.getByLabelText("密码"), "admin123");
    await userEvent.click(screen.getByRole("button", { name: "登录" }));

    await waitFor(() => expect(screen.getByText("超级管理员")).toBeInTheDocument());
  });
});
```

- [ ] **Step 2: Run frontend tests and verify they fail because the frontend is missing**

Run: `npm --prefix frontend test -- --run`

Expected: FAIL with missing `frontend/package.json` or missing app files.

- [ ] **Step 3: Add Vite React app, API client, auth context, and login page**

Create Vite configuration, TypeScript configuration, `main.tsx`, `App.tsx`,
`api/client.ts`, `AuthContext.tsx`, and `LoginPage.tsx`. The API client must
send `credentials: "include"` with every request.

Login form labels must be exactly `用户名` and `密码`, and the submit button label
must be `登录`.

- [ ] **Step 4: Run frontend auth tests**

Run: `npm --prefix frontend test -- --run`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat: scaffold react frontend"
```

## Task 9: App Shell, Team Tree, and Role-Gated Navigation

**Files:**

- Create: `frontend/src/types.ts`
- Create: `frontend/src/api/teams.ts`
- Create: `frontend/src/components/AppShell.tsx`
- Create: `frontend/src/components/TeamTree.tsx`
- Create: `frontend/src/components/RoleGate.tsx`
- Create: `frontend/src/pages/BoardPage.tsx`
- Create: `frontend/src/components/TeamTree.test.tsx`

- [ ] **Step 1: Write failing team tree test**

```tsx
describe("TeamTree", () => {
  it("renders nested teams and selects a child", async () => {
    const onSelect = vi.fn();
    render(<TeamTree teams={[{ id: 1, name: "研发部", children: [{ id: 2, name: "平台组", children: [] }] }]} selectedTeamId={1} onSelect={onSelect} />);

    expect(screen.getByText("研发部")).toBeInTheDocument();
    await userEvent.click(screen.getByText("平台组"));

    expect(onSelect).toHaveBeenCalledWith(2);
  });
});
```

- [ ] **Step 2: Run the team tree test and verify it fails**

Run: `npm --prefix frontend test -- --run TeamTree`

Expected: FAIL with missing `TeamTree`.

- [ ] **Step 3: Implement shell and team tree**

Implement a compact left sidebar tree, top user bar, logout action, and selected
team state. Show board content as the default authenticated page. Add role-gated
navigation entries for team admin, sprints, user admin, and recycle bin.

- [ ] **Step 4: Run frontend tests**

Run: `npm --prefix frontend test -- --run`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src
git commit -m "feat: add frontend app shell"
```

## Task 10: Board Page, Filters, Columns, and Task Modal

**Files:**

- Create: `frontend/src/api/tasks.ts`
- Create: `frontend/src/components/BoardFilters.tsx`
- Create: `frontend/src/components/KanbanBoard.tsx`
- Create: `frontend/src/components/TaskModal.tsx`
- Modify: `frontend/src/pages/BoardPage.tsx`
- Create: `frontend/src/components/KanbanBoard.test.tsx`
- Create: `frontend/src/components/BoardFilters.test.tsx`

- [ ] **Step 1: Write failing board grouping and filter tests**

```tsx
describe("KanbanBoard", () => {
  it("groups task cards by status columns", () => {
    render(<KanbanBoard tasks={[
      { id: 1, title: "接入登录", status: "TODO", teamName: "平台组" },
      { id: 2, title: "实现回收站", status: "DONE", teamName: "平台组" }
    ]} onEdit={vi.fn()} />);

    expect(within(screen.getByLabelText("待开始")).getByText("接入登录")).toBeInTheDocument();
    expect(within(screen.getByLabelText("已完成")).getByText("实现回收站")).toBeInTheDocument();
  });
});

describe("BoardFilters", () => {
  it("emits selected sub-team, member, status, and sprint filters", async () => {
    const onChange = vi.fn();
    render(<BoardFilters teams={[{ id: 2, name: "平台组" }]} members={[{ id: 3, displayName: "小王" }]} sprints={[{ id: 4, name: "Sprint A" }]} value={{}} onChange={onChange} />);

    await userEvent.selectOptions(screen.getByLabelText("状态"), "TODO");

    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ status: "TODO" }));
  });
});
```

- [ ] **Step 2: Run board UI tests and verify they fail**

Run: `npm --prefix frontend test -- --run KanbanBoard BoardFilters`

Expected: FAIL with missing board components.

- [ ] **Step 3: Implement board UI and task modal**

Implement:

- Status columns labeled `待开始`, `进行中`, `已完成`.
- Task cards with team, assignee, sprint, remarks indicator, and risks indicator.
- Filters for sub-team, member, task status, and sprint.
- Task create/edit modal with title, description, remarks, risks, status, sprint, and assignee.
- Explicit status select or buttons for moving tasks between columns.

- [ ] **Step 4: Run frontend tests**

Run: `npm --prefix frontend test -- --run`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src
git commit -m "feat: add kanban board ui"
```

## Task 11: Administration Pages and Recycle Bin UI

**Files:**

- Create: `frontend/src/api/users.ts`
- Create: `frontend/src/api/sprints.ts`
- Create: `frontend/src/api/recycleBin.ts`
- Create: `frontend/src/pages/TeamAdminPage.tsx`
- Create: `frontend/src/pages/SprintPage.tsx`
- Create: `frontend/src/pages/UserAdminPage.tsx`
- Create: `frontend/src/pages/RecycleBinPage.tsx`
- Create: `frontend/src/pages/RecycleBinPage.test.tsx`

- [ ] **Step 1: Write failing recycle-bin UI test**

```tsx
describe("RecycleBinPage", () => {
  it("selects deleted tasks and bulk deletes them", async () => {
    const api = {
      listDeletedTasks: vi.fn(async () => [{ id: 9, title: "旧任务", teamName: "平台组", deletedAt: "2026-06-23T00:00:00Z" }]),
      bulkDeleteTasks: vi.fn(async () => undefined),
      restoreTask: vi.fn(async () => undefined),
      deleteTaskForever: vi.fn(async () => undefined)
    };

    render(<RecycleBinPage teamId={1} api={api} />);

    await userEvent.click(await screen.findByRole("checkbox", { name: "选择 旧任务" }));
    await userEvent.click(screen.getByRole("button", { name: "永久删除所选" }));

    expect(api.bulkDeleteTasks).toHaveBeenCalledWith([9]);
  });
});
```

- [ ] **Step 2: Run admin UI tests and verify they fail**

Run: `npm --prefix frontend test -- --run RecycleBinPage`

Expected: FAIL with missing recycle-bin page.

- [ ] **Step 3: Implement administration pages**

Implement:

- Team administration: create root/sub-team, rename, member list, add/remove member, role select.
- Sprint management: list, create, rename, activate/deactivate.
- User administration: create user, reset password, grant/remove super administrator.
- Recycle bin: list deleted tasks, restore one, permanently delete one, select multiple, delete selected, delete all.

Use compact tables and dialogs. Hide user administration from non-super-admin users.

- [ ] **Step 4: Run frontend tests**

Run: `npm --prefix frontend test -- --run`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src
git commit -m "feat: add admin and recycle bin ui"
```

## Task 12: README, Integration Wiring, and Local Smoke Test

**Files:**

- Modify: `README.md`
- Modify: `frontend/vite.config.ts`
- Modify: `.gitignore`

- [ ] **Step 1: Write a failing smoke checklist**

Create a temporary manual checklist in the working notes and execute it after
the app runs:

```text
1. Start backend on http://localhost:8080.
2. Start frontend on http://localhost:5173.
3. Log in with admin / admin123.
4. Create a user, a root team, a sub-team, one member, one sprint, and one task.
5. Open the root board and verify the sub-team task is visible.
6. Filter by sub-team, member, status, and sprint.
7. Delete the task, restore it, delete it again, and permanently delete it.
```

Before implementation wiring is complete, this checklist fails at startup or login.

- [ ] **Step 2: Add frontend proxy and README instructions**

Configure Vite dev proxy from `/api` to `http://localhost:8080`. Update README
with:

- Backend prerequisites and startup command.
- Frontend prerequisites and startup command.
- Default seeded super administrator credentials.
- SQLite database file location.
- Test commands for backend and frontend.

- [ ] **Step 3: Run full automated verification**

Run:

```bash
mvn -f backend/pom.xml test
npm --prefix frontend install
npm --prefix frontend test -- --run
npm --prefix frontend build
```

Expected: all commands pass.

- [ ] **Step 4: Start local dev servers**

Run backend:

```bash
mvn -f backend/pom.xml spring-boot:run
```

Run frontend in a second session:

```bash
npm --prefix frontend run dev -- --host 127.0.0.1
```

Expected:

- Backend listens on `http://localhost:8080`.
- Frontend listens on `http://127.0.0.1:5173`.
- Login works with `admin / admin123`.

- [ ] **Step 5: Execute smoke checklist and fix any failure with a failing test first**

For each smoke failure, add or adjust a backend or frontend test that reproduces
the failure, verify it fails, implement the fix, and rerun the relevant suite.

- [ ] **Step 6: Commit**

```bash
git add README.md .gitignore frontend/vite.config.ts backend frontend
git commit -m "docs: add local development instructions"
```

## Final Verification

- [ ] Run `mvn -f backend/pom.xml test`.
- [ ] Run `npm --prefix frontend test -- --run`.
- [ ] Run `npm --prefix frontend build`.
- [ ] Start backend and frontend.
- [ ] Verify the MVP acceptance path manually in the browser.
- [ ] Run `git status --short` and confirm only intentional files are changed.
