# User Avatar Upload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add self-service avatar upload, display, replacement, and removal for the current logged-in user.

**Architecture:** Store avatar bytes and content type directly on `users` in SQLite. Backend exposes current-user avatar endpoints under `/api/users/me/avatar`; frontend shows a reusable avatar in the workspace header and calls dedicated user-avatar API helpers.

**Tech Stack:** Java 8, Spring Boot 2.7, Spring MVC multipart upload, SQLite, React 18, TypeScript, Vitest, Testing Library.

---

## File Map

- Modify `backend/src/main/resources/schema.sql`: add nullable avatar columns for new databases.
- Modify `backend/src/main/java/com/example/kanban/users/UserRepository.java`: run startup migration, expose avatar persistence methods, and include avatar metadata in `UserRecord`.
- Modify `backend/src/main/java/com/example/kanban/auth/CurrentUser.java`: serialize `avatarUrl` for the authenticated user.
- Modify `backend/src/main/java/com/example/kanban/auth/AuthController.java`: keep `/api/auth/me` behavior backed by refreshed session principal from the security filter.
- Modify `backend/src/main/java/com/example/kanban/users/UserDtos.java`: add nullable `avatarUrl` on user-admin responses.
- Modify `backend/src/main/java/com/example/kanban/users/UserController.java`: add current-user avatar upload, fetch, and delete endpoints.
- Modify `backend/src/test/java/com/example/kanban/ApplicationSmokeTest.java`: assert new schema and migration behavior.
- Modify `backend/src/test/java/com/example/kanban/auth/AuthControllerTest.java`: assert current-user avatar metadata.
- Modify `backend/src/test/java/com/example/kanban/users/UserControllerTest.java`: cover avatar endpoint behavior.
- Modify `frontend/src/types.ts`: add nullable `avatarUrl` to user types.
- Modify `frontend/src/api/client.ts`: allow non-JSON request bodies for `FormData`.
- Create `frontend/src/api/profile.ts`: current-user avatar upload/delete helpers.
- Create `frontend/src/components/UserAvatar.tsx`: default initial avatar and uploaded image rendering.
- Create `frontend/src/components/UserAvatar.test.tsx`: default avatar behavior.
- Modify `frontend/src/auth/AuthContext.tsx`: expose `refreshUser` for post-upload state refresh.
- Modify `frontend/src/components/AppShell.tsx`: render avatar controls and upload/remove actions.
- Modify `frontend/src/components/AppShell.test.tsx`: cover default avatar, upload, delete, and upload error behavior.
- Modify `frontend/src/styles.css`: add compact header avatar styles.

---

### Task 1: Backend Avatar Schema and Current-User Metadata

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/java/com/example/kanban/users/UserRepository.java`
- Modify: `backend/src/main/java/com/example/kanban/users/UserDtos.java`
- Modify: `backend/src/main/java/com/example/kanban/auth/CurrentUser.java`
- Modify: `backend/src/test/java/com/example/kanban/ApplicationSmokeTest.java`
- Modify: `backend/src/test/java/com/example/kanban/auth/AuthControllerTest.java`

- [x] **Step 1: Write failing schema and metadata tests**

Add schema assertions in `ApplicationSmokeTest`:

```java
List<String> userColumns = jdbcTemplate.queryForList(
        "select name from pragma_table_info('users')",
        String.class);
assertThat(userColumns).contains("avatar_data", "avatar_content_type", "avatar_updated_at");
```

Add current-user JSON assertions in `AuthControllerTest`:

```java
.andExpect(jsonPath("$.avatarUrl").doesNotExist())
```

Then add a second authenticated case:

```java
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
```

- [x] **Step 2: Run tests to verify red**

Run:

```bash
mvn -f backend/pom.xml -Dtest=ApplicationSmokeTest,AuthControllerTest test
```

Expected: FAIL because `avatar_data`, `avatar_content_type`, and `avatar_updated_at` do not exist, and `avatarUrl` is not serialized.

- [x] **Step 3: Implement minimal schema and metadata support**

Add columns to `schema.sql`:

```sql
avatar_data blob,
avatar_content_type text,
avatar_updated_at text,
```

In `UserRepository`, add startup migration methods called from the constructor:

```java
public UserRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
    ensureAvatarColumns();
}

private void ensureAvatarColumns() {
    addColumnIfMissing("avatar_data", "blob");
    addColumnIfMissing("avatar_content_type", "text");
    addColumnIfMissing("avatar_updated_at", "text");
}

private void addColumnIfMissing(String columnName, String columnDefinition) {
    Integer count = jdbc.queryForObject(
            "select count(*) from pragma_table_info('users') where name = ?",
            Integer.class,
            columnName);
    if (count == null || count == 0) {
        jdbc.execute("alter table users add column " + columnName + " " + columnDefinition);
    }
}
```

Extend user queries to select avatar metadata:

```java
"select id, username, display_name, password_hash, super_admin, avatar_content_type, avatar_updated_at from users ..."
```

Extend `UserRecord` with `avatarContentType`, `avatarUpdatedAt`, and:

```java
public boolean hasAvatar() {
    return avatarContentType != null && avatarUpdatedAt != null;
}
```

Extend `CurrentUser` with `avatarUrl` and compute:

```java
private static String avatarUrl(UserRepository.UserRecord user) {
    if (!user.hasAvatar()) {
        return null;
    }
    return "/api/users/me/avatar?v=" + user.getAvatarUpdatedAt().replaceAll("[^0-9]", "");
}
```

Add nullable `avatarUrl` to `UserDtos.UserResponse`, returning `null` for now.

- [x] **Step 4: Run tests to verify green**

Run:

```bash
mvn -f backend/pom.xml -Dtest=ApplicationSmokeTest,AuthControllerTest test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/java/com/example/kanban/users/UserRepository.java backend/src/main/java/com/example/kanban/users/UserDtos.java backend/src/main/java/com/example/kanban/auth/CurrentUser.java backend/src/test/java/com/example/kanban/ApplicationSmokeTest.java backend/src/test/java/com/example/kanban/auth/AuthControllerTest.java
git commit -m "feat: add avatar metadata to users"
```

---

### Task 2: Backend Current-User Avatar Endpoints

**Files:**
- Modify: `backend/src/main/java/com/example/kanban/users/UserRepository.java`
- Modify: `backend/src/main/java/com/example/kanban/users/UserService.java`
- Modify: `backend/src/main/java/com/example/kanban/users/UserController.java`
- Modify: `backend/src/test/java/com/example/kanban/users/UserControllerTest.java`

- [x] **Step 1: Write failing endpoint tests**

Add tests in `UserControllerTest`:

```java
@Test
void currentUserUploadsFetchesReplacesAndDeletesAvatar() throws Exception {
    MockHttpSession admin = loginAsAdmin();
    byte[] firstAvatar = new byte[] {1, 2, 3};
    byte[] secondAvatar = new byte[] {4, 5};

    mvc.perform(multipart("/api/users/me/avatar")
                    .file("file", firstAvatar)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .session(admin)
                    .with(request -> {
                        request.setMethod("PUT");
                        request.addHeader("Content-Type", "image/png");
                        return request;
                    }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.startsWith("/api/users/me/avatar?v=")));

    mvc.perform(get("/api/users/me/avatar").session(admin))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(content().bytes(firstAvatar));

    mvc.perform(multipart("/api/users/me/avatar")
                    .file("file", secondAvatar)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .session(admin)
                    .with(request -> {
                        request.setMethod("PUT");
                        request.addHeader("Content-Type", "image/jpeg");
                        return request;
                    }))
            .andExpect(status().isOk());

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
```

Also add invalid upload tests for empty file, unsupported type, oversized file, and unauthenticated upload.

- [x] **Step 2: Run tests to verify red**

Run:

```bash
mvn -f backend/pom.xml -Dtest=UserControllerTest test
```

Expected: FAIL with 404 or 405 for `/api/users/me/avatar`.

- [x] **Step 3: Implement minimal endpoint support**

Add `AvatarRecord`, `updateAvatar`, `removeAvatar`, and `findAvatarById` to `UserRepository`.

Add service methods in `UserService`:

```java
public UserRepository.UserRecord updateAvatar(long id, MultipartFile file) {
    validateAvatar(file);
    userRepository.updateAvatar(id, file.getBytes(), file.getContentType());
    return findUserOrThrow(id);
}

public UserRepository.AvatarRecord findAvatar(long id) {
    return userRepository.findAvatarById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
}

public UserRepository.UserRecord removeAvatar(long id) {
    findUserOrThrow(id);
    userRepository.removeAvatar(id);
    return findUserOrThrow(id);
}
```

Validation rules:

```java
private static final long MAX_AVATAR_BYTES = 2L * 1024L * 1024L;
private static final Set<String> ALLOWED_AVATAR_TYPES =
        new HashSet<>(Arrays.asList("image/png", "image/jpeg", "image/webp", "image/gif"));
```

Add endpoints in `UserController`:

```java
@PutMapping("/me/avatar")
public CurrentUser uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) { ... }

@GetMapping("/me/avatar")
public ResponseEntity<byte[]> avatar(Authentication authentication) { ... }

@DeleteMapping("/me/avatar")
public CurrentUser deleteAvatar(Authentication authentication) { ... }
```

- [x] **Step 4: Run tests to verify green**

Run:

```bash
mvn -f backend/pom.xml -Dtest=UserControllerTest test
```

Expected: PASS.

- [x] **Step 5: Run backend suite**

Run:

```bash
mvn -f backend/pom.xml test
```

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add backend/src/main/java/com/example/kanban/users/UserRepository.java backend/src/main/java/com/example/kanban/users/UserService.java backend/src/main/java/com/example/kanban/users/UserController.java backend/src/test/java/com/example/kanban/users/UserControllerTest.java
git commit -m "feat: add current user avatar api"
```

---

### Task 3: Frontend Avatar API, Rendering, and Auth Refresh

**Files:**
- Modify: `frontend/src/types.ts`
- Modify: `frontend/src/api/client.ts`
- Create: `frontend/src/api/profile.ts`
- Create: `frontend/src/components/UserAvatar.tsx`
- Create: `frontend/src/components/UserAvatar.test.tsx`
- Modify: `frontend/src/auth/AuthContext.tsx`
- Modify: `frontend/src/auth/AuthContext.test.tsx`

- [x] **Step 1: Write failing avatar component and auth tests**

Create `UserAvatar.test.tsx`:

```tsx
it("uses the last non-whitespace display-name character for the default avatar", () => {
  render(<UserAvatar displayName="管理员 " username="admin" />);
  expect(screen.getByLabelText("管理员 的默认头像")).toHaveTextContent("员");
});

it("falls back to username when display name is blank", () => {
  render(<UserAvatar displayName=" " username="admin" />);
  expect(screen.getByLabelText("admin 的默认头像")).toHaveTextContent("n");
});

it("renders an uploaded avatar image when avatarUrl exists", () => {
  render(<UserAvatar avatarUrl="/api/users/me/avatar?v=1" displayName="管理员" username="admin" />);
  expect(screen.getByRole("img", { name: "管理员 的头像" })).toHaveAttribute("src", "/api/users/me/avatar?v=1");
});
```

Add an `AuthContext` test that calls `refreshUser` and expects `/api/auth/me` to reload user state.

- [x] **Step 2: Run tests to verify red**

Run:

```bash
npm --prefix frontend test -- --run frontend/src/components/UserAvatar.test.tsx frontend/src/auth/AuthContext.test.tsx
```

Expected: FAIL because `UserAvatar`, `avatarUrl`, and `refreshUser` do not exist.

- [x] **Step 3: Implement minimal frontend primitives**

Add `avatarUrl?: string | null` to `CurrentUser` and `UserAccount`.

Update `apiRequest` to preserve `FormData`:

```ts
const body = hasBody && options.body instanceof FormData ? options.body : JSON.stringify(options.body);
if (hasBody && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
  headers.set("Content-Type", "application/json");
}
```

Create `profile.ts`:

```ts
export function uploadCurrentUserAvatar(file: File) {
  const body = new FormData();
  body.append("file", file);
  return apiRequest<CurrentUser>("/api/users/me/avatar", { method: "PUT", body });
}

export function deleteCurrentUserAvatar() {
  return apiRequest<CurrentUser>("/api/users/me/avatar", { method: "DELETE" });
}
```

Create `UserAvatar.tsx` with:

```tsx
export function defaultAvatarInitial(displayName: string, username: string) {
  const source = displayName.trim() || username.trim();
  return source ? Array.from(source).pop() ?? "?" : "?";
}
```

Extend `AuthContextValue` with:

```ts
refreshUser: () => Promise<CurrentUser>;
setCurrentUser: (user: CurrentUser) => void;
```

- [x] **Step 4: Run tests to verify green**

Run:

```bash
npm --prefix frontend test -- --run frontend/src/components/UserAvatar.test.tsx frontend/src/auth/AuthContext.test.tsx
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/types.ts frontend/src/api/client.ts frontend/src/api/profile.ts frontend/src/components/UserAvatar.tsx frontend/src/components/UserAvatar.test.tsx frontend/src/auth/AuthContext.tsx frontend/src/auth/AuthContext.test.tsx
git commit -m "feat: add avatar frontend primitives"
```

---

### Task 4: Workspace Header Avatar Upload UI

**Files:**
- Modify: `frontend/src/components/AppShell.tsx`
- Modify: `frontend/src/components/AppShell.test.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/styles.css`

- [x] **Step 1: Write failing header tests**

Extend `AppShell.test.tsx`:

```tsx
it("shows the default avatar in the workspace header", () => {
  render(<AppShell {...propsWithUser({ displayName: "管理员", avatarUrl: null })}>内容</AppShell>);
  expect(screen.getByLabelText("管理员 的默认头像")).toHaveTextContent("员");
});
```

Add upload and delete behavior tests using injected props:

```tsx
it("uploads and removes the current user's avatar", async () => {
  const uploadAvatar = vi.fn(async () => undefined);
  const deleteAvatar = vi.fn(async () => undefined);
  const file = new File(["avatar"], "avatar.png", { type: "image/png" });

  render(<AppShell {...propsWithUser({ displayName: "管理员", avatarUrl: "/api/users/me/avatar?v=1" })} onUploadAvatar={uploadAvatar} onDeleteAvatar={deleteAvatar}>内容</AppShell>);

  await userEvent.upload(screen.getByLabelText("上传头像"), file);
  await userEvent.click(screen.getByRole("button", { name: "移除头像" }));

  expect(uploadAvatar).toHaveBeenCalledWith(file);
  expect(deleteAvatar).toHaveBeenCalled();
});
```

Add failure test:

```tsx
it("shows an inline avatar upload error", async () => {
  const uploadAvatar = vi.fn(async () => {
    throw new Error("bad avatar");
  });
  const file = new File(["avatar"], "avatar.txt", { type: "text/plain" });

  render(<AppShell {...propsWithUser({ displayName: "管理员", avatarUrl: null })} onUploadAvatar={uploadAvatar}>内容</AppShell>);

  await userEvent.upload(screen.getByLabelText("上传头像"), file);

  expect(await screen.findByText("头像上传失败")).toBeInTheDocument();
});
```

- [x] **Step 2: Run tests to verify red**

Run:

```bash
npm --prefix frontend test -- --run frontend/src/components/AppShell.test.tsx
```

Expected: FAIL because header avatar controls do not exist.

- [x] **Step 3: Implement minimal header UI**

Add props to `AppShell`:

```ts
onUploadAvatar: (file: File) => Promise<void>;
onDeleteAvatar: () => Promise<void>;
```

Use `UserAvatar` in `.current-user`, add a hidden/native file input labeled `上传头像`, and show a remove button only when `user.avatarUrl` exists.

In `App.tsx`, import `uploadCurrentUserAvatar` and `deleteCurrentUserAvatar`; after each call, update auth state with the returned `CurrentUser`.

Add CSS for:

```css
.avatar-circle { width: 40px; height: 40px; border-radius: 50%; }
.avatar-default { background: #1d4ed8; color: #fff; }
.avatar-actions { display: flex; align-items: center; gap: 8px; }
```

- [x] **Step 4: Run tests to verify green**

Run:

```bash
npm --prefix frontend test -- --run frontend/src/components/AppShell.test.tsx
```

Expected: PASS.

- [x] **Step 5: Run frontend suite and build**

Run:

```bash
npm --prefix frontend test -- --run
npm --prefix frontend run build
```

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add frontend/src/components/AppShell.tsx frontend/src/components/AppShell.test.tsx frontend/src/App.tsx frontend/src/styles.css
git commit -m "feat: add avatar upload controls"
```

---

### Task 5: Final Verification

**Files:**
- Review all changed files.

- [x] **Step 1: Run full verification**

Run:

```bash
mvn -f backend/pom.xml test
npm --prefix frontend test -- --run
npm --prefix frontend run build
git status --short
```

Expected:

- Backend tests pass.
- Frontend tests pass.
- Frontend production build passes.
- Git status is clean after commits.

- [x] **Step 2: Summarize result**

Report the branch, commits, verification commands, and any known residual risks.

---

## Self-Review

- Spec coverage: The plan covers SQLite binary storage, current-user-only upload, default blue last-character avatar, upload replacement, deletion, validation, cache-busted avatar URL, startup migration, backend tests, and frontend tests.
- Placeholder scan: No unresolved placeholders are present.
- Type consistency: Backend uses `avatarUrl`, `avatarContentType`, and `avatarUpdatedAt`; frontend uses `avatarUrl` consistently on `CurrentUser` and `UserAccount`.
