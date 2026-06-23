# Kanban

Kanban is a web-based team board system.

The backend target is Java 8, with SQLite as the database. The product is built
around teams, members, tasks, and sprints, with hierarchical team boards and
role-based administration.

## Goals

- Create teams and sub-teams.
- Create accounts and manage team membership.
- Assign team administrators.
- Let a parent team board view all task content from its sub-team boards.
- Filter board tasks by sub-team, member, task status, and sprint.
- Support custom sprint names.
- Move deleted tasks into a recycle bin before permanent deletion.
- Support optional scheduled database snapshots managed by super administrators.

## Core Concepts

### Teams

Teams can contain sub-teams. A team board includes the content of its own board
and the boards of its sub-teams.

### Members

Supported member roles:

- 超级管理员
- 团队创建者
- 团队管理员
- 团队成员

### Tasks

Supported task statuses:

- 待开始
- 进行中
- 已完成

Tasks must support:

- Remarks
- Risks
- Sprint assignment
- Member assignment
- Recycle-bin based deletion

### Sprints

Sprints support custom names and can be used as a board filter.

### Database Snapshots

The backend must support scheduled database snapshots. This feature is disabled
by default.

When enabled, the service exports all data in the SQLite database into snapshot
files. By default, snapshots run every day at `00:00`, are written to a `backup`
directory next to the application jar, and are retained for 3 days.

If the backup directory does not exist, the backend must create it before writing
the snapshot. Each time a snapshot is generated, the backend must delete backup
files older than the configured retention period.

Super administrators can manage snapshot settings from the administration page:

- Enable or disable scheduled snapshots.
- Change the scheduled execution time.
- Change how many days of snapshots are retained.
- Change the snapshot output path.

## Recycle Bin

Task deletion is soft deletion by default. Deleted tasks enter the recycle bin.
The recycle bin supports:

- Multi-select permanent deletion
- Delete all
- Hard deletion after recycle-bin deletion

## Planned Technical Stack

- Backend: Java 8
- Frontend: React
- Database: SQLite
- Application type: web kanban
