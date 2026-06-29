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

## Local Development

### Prerequisites

- Java 8
- Maven
- Node.js 20.19 or newer

### Backend

Start the Spring Boot API from the repository root:

```bash
mvn -f backend/pom.xml spring-boot:run
```

The backend listens on `http://localhost:8080`. On first startup it creates the
SQLite database file `kanban.sqlite3` in the current working directory and seeds
the default super administrator:

- Username: `admin`
- Password: `admin123`

### Frontend

Install dependencies and start the Vite development server:

```bash
npm --prefix frontend install
npm --prefix frontend run dev -- --host 127.0.0.1
```

Open `http://127.0.0.1:5173`. The Vite server proxies `/api` requests to
`http://localhost:8080`, so the browser uses the frontend origin while API
traffic reaches the backend session endpoints. If `5173` is already in use, add
`--port <port>` to the dev command and open that port instead.

### Static Build + Executable JAR

The project can be built into a single executable Spring Boot JAR by compiling
the frontend into static assets and packaging them into `backend/target` with
`mvn clean package`.

```bash
mvn -f backend/pom.xml -DskipTests clean package
```

After packaging, run the JAR directly:

```bash
java -jar backend/target/kanban-0.0.1-SNAPSHOT.jar
```

Because frontend assets are placed under `static`, opening `http://localhost:8080`
will load both API and UI from the same process.

### Windows One-Click Start / Stop

Double-click or run these scripts from the repository root:

```bash
scripts\start-kanban.bat
scripts\stop-kanban.bat
```

`start-kanban.bat` will:

1. run `mvn -f backend/pom.xml -DskipTests clean package`
   (this triggers frontend install/build and injects static assets into the JAR),
2. start `backend/target/kanban-0.0.1-SNAPSHOT.jar` in background and save PID.

`stop-kanban.bat` stops the process recorded by PID (and falls back to matching
`kanban-*.jar` Java processes).

### Linux One-Command Start / Stop

On Linux, this repository provides JDK-only start/stop scripts for the packaged JAR:

```bash
chmod +x scripts/start-kanban-jar.sh scripts/stop-kanban-jar.sh
./scripts/start-kanban-jar.sh backend/target/kanban-0.0.1-SNAPSHOT.jar 0.0.0.0

# stop
./scripts/stop-kanban-jar.sh backend/target/kanban-0.0.1-SNAPSHOT.jar
```

These scripts start/stop only the JAR process and redirect logs to:
`backend/target/logs/kanban-runtime.log` and `backend/target/logs/kanban-runtime-error.log`.

### Server Deployment (JDK-only runtime)

If the server has only JDK (no npm, no Node), deploy this way:

1. On a machine with Node + JDK, run:

```bash
mvn -f backend/pom.xml -DskipTests clean package
```

2. Copy only the JAR (and optionally `application.yml` if you use custom DB path) to the server:

```bash
backend/target/kanban-0.0.1-SNAPSHOT.jar
```

3. Start the JAR on the server with:

```bash
./scripts/start-kanban-jar.sh backend/target/kanban-0.0.1-SNAPSHOT.jar 0.0.0.0
```

Or directly:

```bash
java -jar backend/target/kanban-0.0.1-SNAPSHOT.jar
```

The app serves both API and frontend from the same process, so external users can
open `http://<server_ip>:8080`.

Stop on the server with:

```bash
./scripts/stop-kanban-jar.sh backend/target/kanban-0.0.1-SNAPSHOT.jar
```

Notes for external access:

1. Ensure port `8080` is open in the server firewall/security group.
2. Use the server's public IP, e.g. `http://203.0.113.10:8080`.
3. For production, consider running with a reverse proxy (Nginx/Caddy) on port 80/443.

### Database And Snapshots

The local SQLite database is `kanban.sqlite3` relative to the directory where
the backend process starts. With the Maven command above, that file is
`backend/kanban.sqlite3`. Local database and snapshot files are ignored by git.

Scheduled snapshots are disabled by default. The default schedule is daily at
`00:00`, represented by the cron expression `0 0 0 * * *`; snapshots are kept for
3 days and written to the `backup` directory next to the running application.
Super administrators can open snapshot settings to enable or disable scheduled
snapshots, change the cron expression, retention days, and output path, or run a
snapshot immediately.

## Test Commands

Run the backend suite:

```bash
mvn -f backend/pom.xml test
```

Run the frontend tests and production build:

```bash
npm --prefix frontend test -- --run
npm --prefix frontend run build
```

## Local Smoke Checklist

1. Start the backend on `http://localhost:8080`.
2. Start the frontend on `http://127.0.0.1:5173`.
3. Log in with `admin` / `admin123`.
4. Create a user, a root team, a sub-team, one member, one sprint, and one task.
5. Open the root board and verify the sub-team task is visible.
6. Filter by sub-team, member, status, and sprint.
7. Delete the task, restore it, delete it again, and permanently delete it.
8. Open snapshot settings as the super administrator, enable snapshots, change
   retention days and output path, trigger a manual snapshot, and verify the
   backup file is created in the configured directory.
