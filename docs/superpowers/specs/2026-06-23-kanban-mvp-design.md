# Kanban MVP Design

## Goal

Build a complete but restrained MVP for the Kanban project described in `README.md`.
The application will be a web-based team board system with a Java 8 backend,
SQLite persistence, and a React frontend.

## Scope

The MVP must cover the README's core capabilities:

- Create teams and nested sub-teams.
- Create user accounts and manage team membership.
- Assign team administrators.
- Let a parent team board show tasks from its own board and all descendant sub-team boards.
- Filter board tasks by sub-team, member, task status, and sprint.
- Support custom sprint names.
- Soft-delete tasks into a recycle bin before permanent deletion.
- Include basic login and role-based permissions.

The MVP will not include invite emails, avatars, real-time collaboration,
notifications, comments, file attachments, audit logs, complex reporting, or
external identity providers.

## Recommended Architecture

Use a split full-stack structure:

- Backend: Java 8, Spring Boot 2.x, Spring Security, SQLite, Maven.
- Frontend: React, Vite, TypeScript, REST API client.
- Database: SQLite file created locally by the backend.

The backend owns authentication, authorization, persistence, team hierarchy,
soft deletion, and board query behavior. The frontend renders a practical SPA
for the daily workflows: login, selecting teams, board work, team administration,
sprint management, and recycle-bin cleanup.

During development, the backend and frontend run separately. For production-like
local use, the frontend build can be copied into the backend static resources or
served independently.

## User Roles

Supported roles:

- `SUPER_ADMIN`: can manage all users, teams, memberships, sprints, tasks, and recycle bins.
- `TEAM_CREATOR`: owns teams they created and can manage those teams and descendants.
- `TEAM_ADMIN`: can manage members, sprints, and tasks for their team and descendants.
- `TEAM_MEMBER`: can view team boards and update tasks assigned to them.

Role names shown in the UI should use the README labels:

- 超级管理员
- 团队创建者
- 团队管理员
- 团队成员

## Permission Rules

- Anonymous users can only access the login API and frontend login page.
- Super administrators can access every team and administrative action.
- Team creators can manage teams they created, descendant teams, memberships,
  sprints, tasks, and recycle bins inside that tree.
- Team administrators can manage memberships, sprints, tasks, and recycle bins
  inside teams where they are administrators and descendant teams.
- Team members can view tasks in their team tree.
- Team members can create tasks in teams where they are members.
- Team members can edit or move tasks assigned to them.
- Only super administrators, team creators, and team administrators can permanently
  delete tasks from the recycle bin.

When a user has more than one membership, the highest applicable permission wins
for a given team tree.

## Data Model

### users

- `id`: primary key.
- `username`: unique login name.
- `display_name`: visible name.
- `password_hash`: BCrypt password hash.
- `super_admin`: boolean flag.
- `created_at`: timestamp.
- `updated_at`: timestamp.

### teams

- `id`: primary key.
- `name`: team name.
- `parent_id`: nullable reference to `teams.id`.
- `created_by`: reference to `users.id`.
- `created_at`: timestamp.
- `updated_at`: timestamp.

`parent_id` represents the team hierarchy. Board queries for a parent team must
include the parent team and every descendant team.

### team_memberships

- `id`: primary key.
- `team_id`: reference to `teams.id`.
- `user_id`: reference to `users.id`.
- `role`: `TEAM_CREATOR`, `TEAM_ADMIN`, or `TEAM_MEMBER`.
- `created_at`: timestamp.

A user can have one membership per team. The creator of a team receives a
`TEAM_CREATOR` membership for that team.

### sprints

- `id`: primary key.
- `team_id`: reference to `teams.id`.
- `name`: custom sprint name.
- `active`: boolean flag.
- `created_at`: timestamp.
- `updated_at`: timestamp.

Sprint names are user-defined. A task may be assigned to a sprint from its own
team.

### tasks

- `id`: primary key.
- `team_id`: reference to `teams.id`.
- `title`: task title.
- `description`: task description.
- `remarks`: task remarks.
- `risks`: task risks.
- `status`: `TODO`, `IN_PROGRESS`, or `DONE`.
- `sprint_id`: nullable reference to `sprints.id`.
- `assignee_id`: nullable reference to `users.id`.
- `created_by`: reference to `users.id`.
- `deleted_at`: nullable timestamp for recycle-bin state.
- `created_at`: timestamp.
- `updated_at`: timestamp.

Status labels shown in the UI should use:

- 待开始
- 进行中
- 已完成

## Backend Modules

### Authentication

- `POST /api/auth/login`: accepts username and password, returns authenticated user context.
- `POST /api/auth/logout`: clears the session.
- `GET /api/auth/me`: returns current user, global role, and team memberships.

Use session-based authentication with Spring Security. Seed a first super
administrator account on startup when no users exist.

### Users

- `GET /api/users`: list users.
- `POST /api/users`: create user.
- `PATCH /api/users/{id}`: update display name or super administrator flag.
- `PATCH /api/users/{id}/password`: reset password.

Only super administrators can create users and grant global super administrator
access in the MVP.

### Teams

- `GET /api/teams`: list teams visible to the current user as a tree.
- `POST /api/teams`: create root team or sub-team.
- `PATCH /api/teams/{id}`: rename team or change parent.
- `DELETE /api/teams/{id}`: delete empty team.

Deleting teams with tasks or child teams should be blocked in the MVP.

### Memberships

- `GET /api/teams/{teamId}/members`: list team members.
- `POST /api/teams/{teamId}/members`: add a user to a team.
- `PATCH /api/teams/{teamId}/members/{membershipId}`: change role.
- `DELETE /api/teams/{teamId}/members/{membershipId}`: remove member.

### Sprints

- `GET /api/teams/{teamId}/sprints`: list sprints.
- `POST /api/teams/{teamId}/sprints`: create sprint.
- `PATCH /api/sprints/{id}`: rename or activate/deactivate sprint.

### Board Tasks

- `GET /api/teams/{teamId}/board/tasks`: list non-deleted tasks from the team
  and descendants, with filters for sub-team, member, status, and sprint.
- `POST /api/teams/{teamId}/tasks`: create task.
- `GET /api/tasks/{id}`: get task detail.
- `PATCH /api/tasks/{id}`: update title, description, remarks, risks, status,
  sprint, and assignee.
- `DELETE /api/tasks/{id}`: soft-delete task into recycle bin.

### Recycle Bin

- `GET /api/teams/{teamId}/recycle-bin/tasks`: list deleted tasks from the team
  and descendants.
- `POST /api/recycle-bin/tasks/{id}/restore`: restore a deleted task.
- `DELETE /api/recycle-bin/tasks/{id}`: permanently delete one task.
- `POST /api/recycle-bin/tasks/bulk-delete`: permanently delete selected tasks.
- `DELETE /api/teams/{teamId}/recycle-bin/tasks`: permanently delete all deleted
  tasks visible in that team tree.

## Frontend Pages

### Login

- Username and password form.
- Error message for invalid credentials.
- Redirect authenticated users to the board.

### App Shell

- Left-side team tree.
- Top bar with current user and logout action.
- Main content area for board and administration views.

### Board

- Columns for 待开始, 进行中, 已完成.
- Task cards showing title, team, assignee, sprint, remarks/risk indicator.
- Filters for sub-team, member, status, and sprint.
- Task creation action.
- Task detail/edit modal.
- Status changes through explicit controls; drag and drop is optional for MVP.

### Team Administration

- Create root teams and sub-teams.
- Rename teams.
- Manage team members and roles.
- Block destructive team deletion when child teams or tasks exist.

### Sprint Management

- List team sprints.
- Create custom sprint names.
- Rename and activate/deactivate sprints.

### User Administration

- Visible to super administrators.
- Create users.
- Reset passwords.
- Grant or remove super administrator access.

### Recycle Bin

- List soft-deleted tasks.
- Select multiple tasks for permanent deletion.
- Delete all visible recycled tasks.
- Restore individual tasks.

## UI Direction

The UI should be quiet, operational, and optimized for repeated team work:

- No marketing landing page.
- The first authenticated screen is the board.
- Use compact tables, forms, filters, tabs, dialogs, and icon buttons.
- Keep cards restrained with small border radii.
- Use clear Chinese labels for roles and statuses.

## Error Handling

- API validation errors should return `400` with a field-level error map where useful.
- Authentication failures should return `401`.
- Authorization failures should return `403`.
- Missing resources should return `404`.
- Business-rule conflicts, such as deleting a non-empty team, should return `409`.
- The frontend should display inline form errors for validation failures and toast
  or banner errors for failed actions.

## Testing Strategy

Backend tests:

- Authentication succeeds and fails correctly.
- Super administrator seed user exists when the database starts empty.
- Team hierarchy queries include descendants.
- Board filters combine sub-team, member, status, and sprint.
- Role checks allow and deny expected actions.
- Task deletion sets `deleted_at` instead of removing rows.
- Recycle-bin permanent deletion removes rows.

Frontend tests:

- Login form calls the auth API and handles errors.
- Board groups tasks by status.
- Filters produce the expected query parameters.
- Role-gated controls appear or hide for representative roles.
- Recycle-bin multi-select actions call the expected API client methods.

Manual verification:

- Start backend and frontend locally.
- Log in as seeded super administrator.
- Create users, teams, sub-teams, memberships, sprints, and tasks.
- Verify a parent board shows sub-team tasks.
- Verify filters narrow tasks correctly.
- Soft-delete a task, restore it, soft-delete again, then permanently delete it.

## Implementation TODO

- [ ] Scaffold Maven Spring Boot backend targeting Java 8.
- [ ] Add SQLite JDBC, Spring Web, Spring Security, validation, and test dependencies.
- [ ] Configure SQLite datasource and schema initialization.
- [ ] Implement domain enums for roles and task statuses.
- [ ] Implement database schema for users, teams, memberships, sprints, and tasks.
- [ ] Add repository layer for each core table.
- [ ] Add authentication service, password hashing, session security, and seed super administrator.
- [ ] Add authorization service for team-tree permission checks.
- [ ] Implement auth API.
- [ ] Implement user administration API.
- [ ] Implement team and team-tree API.
- [ ] Implement membership API.
- [ ] Implement sprint API.
- [ ] Implement board task API with descendant-team aggregation and filters.
- [ ] Implement recycle-bin API.
- [ ] Add backend tests for authentication, authorization, board queries, filters, and recycle-bin behavior.
- [ ] Scaffold React + Vite + TypeScript frontend.
- [ ] Add routing, API client, auth context, and app shell.
- [ ] Implement login page.
- [ ] Implement team tree navigation.
- [ ] Implement board page with status columns and filters.
- [ ] Implement task create/edit modal.
- [ ] Implement team administration page.
- [ ] Implement membership management UI.
- [ ] Implement sprint management UI.
- [ ] Implement super administrator user management UI.
- [ ] Implement recycle-bin UI with restore, selected delete, and delete all.
- [ ] Add frontend tests for auth, board grouping, filters, role-gated controls, and recycle-bin actions.
- [ ] Add root README instructions for local backend/frontend startup.
- [ ] Run backend tests, frontend tests, and local smoke verification.

## Acceptance Criteria

- A new developer can run the backend and frontend locally from README instructions.
- The seeded super administrator can log in and create users.
- Users can create teams and sub-teams according to their permissions.
- Team administrators can manage memberships, sprints, and tasks.
- Parent team boards show tasks from descendant teams.
- Board filters work for sub-team, member, status, and sprint.
- Tasks support remarks, risks, sprint assignment, member assignment, and status changes.
- Deleting a task moves it into the recycle bin.
- Recycle-bin tasks can be restored, individually deleted forever, bulk deleted,
  and deleted all at once.
- Unauthorized users cannot perform restricted actions.
