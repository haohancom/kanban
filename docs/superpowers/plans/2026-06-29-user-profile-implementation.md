# User Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a dedicated profile workflow where all users can edit their display name and password and manage their own avatar, while removing avatar controls from the top-right header.

**Architecture:** The backend adds `/api/users/me` and `/api/users/me/password` endpoints that operate only on the authenticated principal. The frontend adds a new `ProfilePage` route/view and reuses existing `CurrentUser` payload refreshing patterns for immediate UI state updates. Avatar actions are moved from `AppShell` to `ProfilePage`.

**Tech Stack:** Java 8, Spring Boot 2.7, Spring MVC multipart upload, SQLite, React 18, TypeScript, Vitest.

---

### Task 1: Add backend current-user profile endpoints

**Files:**
- Modify: `backend/src/main/java/com/example/kanban/users/UserDtos.java`
- Modify: `backend/src/main/java/com/example/kanban/users/UserService.java`
- Modify: `backend/src/main/java/com/example/kanban/users/UserController.java`
- Modify: `backend/src/test/java/com/example/kanban/users/UserControllerTest.java`

- [ ] **Step 1: Write the failing tests for current-user profile name update and password policy**

```java
@Test
void currentUserCanUpdateDisplayName() throws Exception {
    MockHttpSession member = createPlainMemberSession();

    mvc.perform(patch("/api/users/me")
                    .session(member)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"displayName\":\"新名字\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("新名字"));
}

@Test
void regularUserMustProvideCurrentPasswordToChangeOwnPassword() throws Exception {
    MockHttpSession member = createPlainMemberSession();

    mvc.perform(patch("/api/users/me/password")
                    .session(member)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newPassword\":\"newpwd\"}"))
            .andExpect(status().isBadRequest());

    mvc.perform(patch("/api/users/me/password")
                    .session(member)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"newpwd\"}"))
            .andExpect(status().isForbidden());
}
```

- [ ] **Step 2: Run the new tests and verify they fail for missing behavior**

Run:
`mvn -f backend/pom.xml -Dtest=UserControllerTest test`

Expected: tests fail because `/api/users/me` and `/api/users/me/password` do not exist yet.

- [ ] **Step 3: Implement minimal backend behavior for display name and own password changes**

```java
public UserRepository.UserRecord updateDisplayName(long id, String displayName) {
    UserRepository.UserRecord current = findUserOrThrow(id);
    if (!StringUtils.hasText(displayName)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
    userRepository.update(id, displayName, current.isSuperAdmin());
    return findUserOrThrow(id);
}

public UserRepository.UserRecord changeOwnPassword(long id, boolean isSuperAdmin, String currentPassword, String newPassword) {
    if (!StringUtils.hasText(newPassword)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
    UserRepository.UserRecord current = findUserOrThrow(id);
    if (!isSuperAdmin) {
        if (!StringUtils.hasText(currentPassword) || !passwordEncoder.matches(currentPassword, current.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
    userRepository.updatePasswordHash(id, passwordEncoder.encode(newPassword));
    return findUserOrThrow(id);
}
```

- [ ] **Step 4: Add controller endpoints and request DTOs**

- `PATCH /api/users/me` -> `UserDtos.CurrentUserUpdateRequest` with required `displayName`, return `CurrentUser`.
- `PATCH /api/users/me/password` -> `UserDtos.CurrentUserPasswordRequest` with required `newPassword` and optional `currentPassword`, return `CurrentUser`.

- [ ] **Step 5: Run user controller tests for this file and commit**

Run:
`mvn -f backend/pom.xml -Dtest=UserControllerTest test`

Then:
`git add backend/src/main/java/com/example/kanban/users/UserController.java backend/src/main/java/com/example/kanban/users/UserDtos.java backend/src/main/java/com/example/kanban/users/UserService.java backend/src/test/java/com/example/kanban/users/UserControllerTest.java`

`git commit -m "feat: add current-user profile endpoints"`

### Task 2: Add profile page and wire app navigation

**Files:**
- Modify: `frontend/src/components/AppShell.tsx`
- Modify: `frontend/src/App.tsx`
- Add: `frontend/src/pages/ProfilePage.tsx`
- Modify: `frontend/src/types.ts`
- Add: `frontend/src/api/profile.ts`
- Modify: `frontend/src/styles.css`
- Add: `frontend/src/pages/ProfilePage.test.tsx`

- [ ] **Step 1: Write failing tests for new navigation and profile page behavior**

```ts
it("shows profile in sidebar and renders profile view", async () => {
  render(
    <AppShell ... user={{ ...user, superAdmin: false }}>
      <div>看板内容</div>
    </AppShell>
  );
  await userEvent.click(screen.getByRole("button", { name: "个人资料" }));
  expect(selectView).toHaveBeenCalledWith("profile");
});

it("shows profile page fields and calls APIs", async () => {
  render(<ProfilePage ... />);
  await user.type(screen.getByLabelText("显示名称"), "新昵称");
  await user.click(screen.getByRole("button", { name: "保存显示名称" }));
  expect(updateCurrentUser).toHaveBeenCalledWith({ displayName: "新昵称" });
});
```

- [ ] **Step 2: Run the new frontend tests and verify they fail**

Run:
`npm --prefix frontend test -- --run`

Expected: failure because files do not exist and existing header upload assertions still reference removed controls.

- [ ] **Step 3: Implement ProfilePage with three sections**

- Profile header card with avatar preview and username.
- Avatar upload and remove actions calling current-user avatar helpers.
- Display-name edit with PATCH submit.
- Password form with current password for non-super-admin users.

- [ ] **Step 4: Move avatar actions out of `AppShell` header, add profile nav/view and profile page route**

- Remove upload/remove controls from header.
- Add persistent `个人资料` nav item.
- Add `WorkspaceView = "profile"` and switch rendering in `App`.

- [ ] **Step 5: Update existing tests, add `ProfilePage.test.tsx`, and run tests**

Run:
`npm --prefix frontend test -- --run`

Commit:
`git add frontend/src/components/AppShell.tsx frontend/src/App.tsx frontend/src/pages/ProfilePage.tsx frontend/src/pages/ProfilePage.test.tsx frontend/src/api/profile.ts frontend/src/types.ts frontend/src/styles.css`

`git commit -m "feat: add profile page and move avatar controls"`

### Task 3: Full pass for affected tests and polish

**Files:**
- Run all tests in both projects

- [ ] **Step 1: Run focused backend and frontend tests**

Run:
`mvn -f backend/pom.xml test`

`npm --prefix frontend test -- --run`

Expected: all pass.

- [ ] **Step 2: Commit final glue updates (if any)**

`git add ...`

`git commit -m "feat: user profile edit flow for self-service"`
