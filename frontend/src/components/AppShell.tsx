import { ReactNode } from "react";
import { CurrentUser, Team } from "../types";
import RoleGate from "./RoleGate";
import TeamTree from "./TeamTree";

interface AppShellProps {
  children: ReactNode;
  onLogout: () => Promise<void>;
  onSelectTeam: (teamId: number) => void;
  selectedTeam: Team | null;
  selectedTeamId: number | null;
  teamError: string | null;
  teams: Team[];
  teamsLoading: boolean;
  user: CurrentUser;
}

export default function AppShell({
  children,
  onLogout,
  onSelectTeam,
  selectedTeam,
  selectedTeamId,
  teamError,
  teams,
  teamsLoading,
  user
}: AppShellProps) {
  const canManageSelectedTeam = canManageTeam(teams, selectedTeamId, user.superAdmin);

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <p className="workspace-kicker">Kanban MVP</p>
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
          <button type="button" className="nav-item active">
            看板
          </button>
          <RoleGate
            canManageTeam={canManageSelectedTeam}
            user={user}
            team={selectedTeam}
            requirement="team-manager"
          >
            <button type="button" className="nav-item">
              团队管理
            </button>
            <button type="button" className="nav-item">
              冲刺管理
            </button>
            <button type="button" className="nav-item">
              回收站
            </button>
          </RoleGate>
          <RoleGate user={user} requirement="super-admin">
            <button type="button" className="nav-item">
              用户管理
            </button>
          </RoleGate>
        </nav>
      </aside>

      <section className="workspace-panel">
        <header className="workspace-header">
          <div>
            <p className="workspace-kicker">{selectedTeam?.name || "未选择团队"}</p>
            <h2>看板</h2>
          </div>
          <div className="current-user">
            <span>{user.displayName}</span>
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
