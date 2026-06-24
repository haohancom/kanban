# Kanban MVP Remaining Todo

## Current State

- Branch: `feature-kanban-mvp`
- Worktree: `/Users/aaron/kanban/.worktrees/kanban-mvp`
- Implementation plan: `docs/superpowers/plans/2026-06-23-kanban-mvp-implementation.md`
- Required execution method: implement directly; use subagents only for spec compliance and code quality review.
- Last completed task: Task 13, README, Integration Wiring, and Local Smoke Test
- Last completed commit: `4a75404 docs: add local development instructions`

## Completed

- [x] Set up isolated worktree for implementation.
- [x] Task 1: Backend scaffold and database schema.
- [x] Task 2: Authentication, seed admin, and current user API.
- [x] Task 3: User administration API.
- [x] Task 4: Snapshot Settings, Scheduler, and Backup Generation.
- [x] Task 5: Teams, Memberships, and Authorization.
- [x] Task 6: Sprints API.
- [x] Task 7: Board Tasks, Filters, and Task Editing.
- [x] Task 8: Recycle Bin API.
- [x] Task 9: Frontend Scaffold, API Client, and Auth Flow.
- [x] Task 10: App Shell, Team Tree, and Role-Gated Navigation.
- [x] Task 11: Board Page, Filters, Columns, and Task Modal.
- [x] Task 12: Administration Pages, Snapshot Settings, and Recycle Bin UI.

## Resume Checklist

- [x] Confirm worktree is clean: `git status --short --branch`.
- [x] Run backend baseline: `mvn -f backend/pom.xml test`.
- [x] Resume from Task 8 in the implementation plan; implement coding work directly.
- [x] For each remaining task, run spec compliance review and code quality review with subagents after direct implementation.
- [x] Do not start the next task until both reviews pass and any Important/Critical findings are fixed.

## Remaining Tasks

- [x] Task 6: Sprints API.
  - Add sprint repository, DTOs, and controller.
  - Authorize writes through team management permission.
  - Verify sprint tests and full backend suite.

- [x] Task 7: Board Tasks, Filters, and Task Editing.
  - Add task status, repository, DTOs, and controller.
  - Implement descendant board query and filters.
  - Implement task create/detail/update behavior and member edit rule.
  - Verify board task tests and full backend suite.

- [x] Task 8: Recycle Bin API.
  - Add soft delete, restore, permanent delete, bulk delete, and delete-all behavior.
  - Authorize permanent deletion through team management permission.
  - Verify recycle-bin tests and full backend suite.

- [x] Task 9: Frontend Scaffold, API Client, and Auth Flow.
  - Add Vite React TypeScript scaffold.
  - Add typed API client, auth context, app entry, and login page.
  - Verify frontend auth tests.

- [x] Task 10: App Shell, Team Tree, and Role-Gated Navigation.
  - Add shared frontend types, team API module, app shell, team tree, and role gate.
  - Add default board page shell.
  - Verify team tree and full frontend tests.

- [x] Task 11: Board Page, Filters, Columns, and Task Modal.
  - Add task API, board filters, kanban board, and task modal.
  - Implement status columns, filter query behavior, and task create/edit controls.
  - Verify board UI tests and full frontend tests.

- [x] Task 12: Administration Pages, Snapshot Settings, and Recycle Bin UI.
  - Add frontend APIs for users, sprints, snapshots, and recycle bin.
  - Add team, sprint, user admin, snapshot settings, and recycle-bin pages.
  - Verify snapshot settings and recycle-bin UI tests plus full frontend tests.

- [x] Task 13: README, Integration Wiring, and Local Smoke Test.
  - Configure Vite `/api` proxy.
  - Update README with backend/frontend startup, default credentials, SQLite data, snapshot defaults, and test commands.
  - Run backend tests, frontend install/tests/build, start local servers, and execute the manual smoke checklist.

- [ ] Final Verification.
  - Run `mvn -f backend/pom.xml test`.
  - Run `npm --prefix frontend test -- --run`.
  - Run `npm --prefix frontend run build`.
  - Start backend and frontend.
  - Manually verify the MVP acceptance path in the browser.
  - Run `git status --short` and confirm only intentional files are changed.
