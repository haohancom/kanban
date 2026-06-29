import { ReactNode } from "react";
import { CurrentUser, Team } from "../types";
import RoleGate from "./RoleGate";
import TeamTree from "./TeamTree";
import UserAvatar from "./UserAvatar";

export type WorkspaceView =
  | "board"
  | "team-admin"
  | "sprints"
  | "recycle-bin"
  | "users"
  | "snapshots"
  | "profile";

interface AppShellProps {
  activeView: WorkspaceView;
  children: ReactNode;
  onLogout: () => Promise<void>;
  onSelectTeam: (teamId: number) => void;
  onSelectView: (view: WorkspaceView) => void;
  selectedTeam: Team | null;
  selectedTeamId: number | null;
  teamError: string | null;
  teams: Team[];
  teamsLoading: boolean;
  user: CurrentUser;
}

export default function AppShell({
  activeView,
  children,
  onLogout,
  onSelectTeam,
  onSelectView,
  selectedTeam,
  selectedTeamId,
  teamError,
  teams,
  teamsLoading,
  user
}: AppShellProps) {
  const canManageSelectedTeam = canManageTeam(teams, selectedTeamId, user.superAdmin);
  const canOpenTeamAdmin = user.superAdmin || canManageSelectedTeam;

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <h1>工作台</h1>
        </div>

        <section className="sidebar-section" aria-labelledby="team-tree-title">
          <div className="section-heading">
            <h2 id="team-tree-title">团队</h2>
            {teamsLoading && <span>加载中</span>}
          </div>
          {teamError && <p className="inline-error">{teamError}</p>}
          <TeamTree teams={teams} selectedTeamId={selectedTeamId} onSelect={onSelectTeam} />
        </section>

        <nav className="workspace-nav" aria-label="工作区导航">
          <NavButton activeView={activeView} view="board" onSelect={onSelectView}>
            看板
          </NavButton>
          <NavButton activeView={activeView} view="profile" onSelect={onSelectView}>
            个人资料
          </NavButton>
          {canOpenTeamAdmin && (
            <NavButton activeView={activeView} view="team-admin" onSelect={onSelectView}>
              团队管理
            </NavButton>
          )}
          <RoleGate
            canManageTeam={canManageSelectedTeam}
            user={user}
            team={selectedTeam}
            requirement="team-manager"
          >
            <NavButton activeView={activeView} view="sprints" onSelect={onSelectView}>
              Sprint 管理
            </NavButton>
            <NavButton activeView={activeView} view="recycle-bin" onSelect={onSelectView}>
              回收站
            </NavButton>
          </RoleGate>
          <RoleGate user={user} requirement="super-admin">
            <NavButton activeView={activeView} view="users" onSelect={onSelectView}>
              用户管理
            </NavButton>
            <NavButton activeView={activeView} view="snapshots" onSelect={onSelectView}>
              快照设置
            </NavButton>
          </RoleGate>
        </nav>
      </aside>

      <section className="workspace-panel">
        <header className="workspace-header">
          <div>
            <p className="workspace-kicker">{selectedTeam?.name || "未选择团队"}</p>
            <h2>{viewTitle(activeView)}</h2>
          </div>
          <div className="current-user">
            <UserAvatar avatarUrl={user.avatarUrl} displayName={user.displayName} username={user.username} />
            <div className="current-user-details">
              <span className="current-user-name">{user.displayName}</span>
            </div>
            <button type="button" onClick={() => void onLogout()}>
              退出
            </button>
          </div>
        </header>
        {children}
      </section>
    </main>
  );
}

function NavButton({
  activeView,
  children,
  onSelect,
  view
}: {
  activeView: WorkspaceView;
  children: ReactNode;
  onSelect: (view: WorkspaceView) => void;
  view: WorkspaceView;
}) {
  return (
    <button
      type="button"
      className={activeView === view ? "nav-item active" : "nav-item"}
      onClick={() => onSelect(view)}
    >
      {children}
    </button>
  );
}

function viewTitle(view: WorkspaceView) {
  if (view === "team-admin") {
    return "团队管理";
  }
  if (view === "sprints") {
    return "Sprint 管理";
  }
  if (view === "recycle-bin") {
    return "回收站";
  }
  if (view === "users") {
    return "用户管理";
  }
  if (view === "profile") {
    return "个人资料";
  }
  if (view === "snapshots") {
    return "快照设置";
  }
  return "看板";
}

function canManageTeam(teams: Team[], selectedTeamId: number | null, superAdmin: boolean) {
  if (selectedTeamId === null) {
    return false;
  }
  if (superAdmin) {
    return hasTeam(teams, selectedTeamId);
  }
  return hasInheritedManagementRole(teams, selectedTeamId, false);
}

function hasInheritedManagementRole(
  teams: Team[],
  selectedTeamId: number,
  inheritedManagement: boolean
): boolean {
  for (const team of teams) {
    const managesHere =
      inheritedManagement || team.role === "TEAM_CREATOR" || team.role === "TEAM_ADMIN";
    if (team.id === selectedTeamId) {
      return managesHere;
    }
    if (hasInheritedManagementRole(team.children, selectedTeamId, managesHere)) {
      return true;
    }
  }
  return false;
}

function hasTeam(teams: Team[], selectedTeamId: number): boolean {
  for (const team of teams) {
    if (team.id === selectedTeamId || hasTeam(team.children, selectedTeamId)) {
      return true;
    }
  }
  return false;
}
