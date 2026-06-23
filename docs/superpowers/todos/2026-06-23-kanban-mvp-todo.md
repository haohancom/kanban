# Kanban MVP Remaining Todo

## Current State

- Branch: `feature-kanban-mvp`
- Worktree: `/Users/aaron/kanban/.worktrees/kanban-mvp`
- Implementation plan: `docs/superpowers/plans/2026-06-23-kanban-mvp-implementation.md`
- Required execution method: `superpowers:subagent-driven-development`
- Last completed task: Task 3, User Administration API
- Last completed commit: `1bc5bca fix: make super admin demotion atomic`

## Completed

- [x] Set up isolated worktree for implementation.
- [x] Task 1: Backend scaffold and database schema.
- [x] Task 2: Authentication, seed admin, and current user API.
- [x] Task 3: User administration API.

## Resume Checklist

- [ ] Confirm worktree is clean: `git status --short --branch`.
- [ ] Run backend baseline: `mvn -f backend/pom.xml test`.
- [ ] Resume from Task 4 in the implementation plan using Subagent-Driven Development.
- [ ] For each remaining task, dispatch an implementer subagent, then run spec compliance review, then code quality review.
- [ ] Do not start the next task until both reviews pass and any Important/Critical findings are fixed.

## Remaining Tasks

- [ ] Task 4: Snapshot Settings, Scheduler, and Backup Generation.
  - Add snapshot settings persistence and API.
  - Add manual snapshot generation and cleanup.
  - Add scheduler guarded by enabled settings.
  - Verify snapshot tests and full backend suite.

- [ ] Task 5: Teams, Memberships, and Authorization.
  - Add team roles, repositories, controllers, and authorization service.
  - Implement team tree and membership management.
  - Add helper fixtures for team/member tests.
  - Verify team authorization tests and full backend suite.

- [ ] Task 6: Sprints API.
  - Add sprint repository, DTOs, and controller.
  - Authorize writes through team management permission.
  - Verify sprint tests and full backend suite.

- [ ] Task 7: Board Tasks, Filters, and Task Editing.
  - Add task status, repository, DTOs, and controller.
  - Implement descendant board query and filters.
  - Implement task create/detail/update behavior and member edit rule.
  - Verify board task tests and full backend suite.

- [ ] Task 8: Recycle Bin API.
  - Add soft delete, restore, permanent delete, bulk delete, and delete-all behavior.
  - Authorize permanent deletion through team management permission.
  - Verify recycle-bin tests and full backend suite.

- [ ] Task 9: Frontend Scaffold, API Client, and Auth Flow.
  - Add Vite React TypeScript scaffold.
  - Add typed API client, auth context, app entry, and login page.
  - Verify frontend auth tests.

- [ ] Task 10: App Shell, Team Tree, and Role-Gated Navigation.
  - Add shared frontend types, team API module, app shell, team tree, and role gate.
  - Add default board page shell.
  - Verify team tree and full frontend tests.

- [ ] Task 11: Board Page, Filters, Columns, and Task Modal.
  - Add task API, board filters, kanban board, and task modal.
  - Implement status columns, filter query behavior, and task create/edit controls.
  - Verify board UI tests and full frontend tests.

- [ ] Task 12: Administration Pages, Snapshot Settings, and Recycle Bin UI.
  - Add frontend APIs for users, sprints, snapshots, and recycle bin.
  - Add team, sprint, user admin, snapshot settings, and recycle-bin pages.
  - Verify snapshot settings and recycle-bin UI tests plus full frontend tests.

- [ ] Task 13: README, Integration Wiring, and Local Smoke Test.
  - Configure Vite `/api` proxy.
  - Update README with backend/frontend startup, default credentials, SQLite data, snapshot defaults, and test commands.
  - Run backend tests, frontend install/tests/build, start local servers, and execute the manual smoke checklist.

- [ ] Final Verification.
  - Run `mvn -f backend/pom.xml test`.
  - Run `npm --prefix frontend test -- --run`.
  - Run `npm --prefix frontend build`.
  - Start backend and frontend.
  - Manually verify the MVP acceptance path in the browser.
  - Run `git status --short` and confirm only intentional files are changed.
