import { useEffect, useMemo, useState } from "react";
import { listTeams } from "./api/teams";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import AppShell, { WorkspaceView } from "./components/AppShell";
import BoardPage from "./pages/BoardPage";
import LoginPage from "./pages/LoginPage";
import RecycleBinPage from "./pages/RecycleBinPage";
import SnapshotSettingsPage from "./pages/SnapshotSettingsPage";
import SprintPage from "./pages/SprintPage";
import TeamAdminPage from "./pages/TeamAdminPage";
import UserAdminPage from "./pages/UserAdminPage";
import ProfilePage from "./pages/ProfilePage";
import { CurrentUser, Team } from "./types";

function AppContent() {
  const { user, loading, logout, setCurrentUser } = useAuth();
  const [teams, setTeams] = useState<Team[]>([]);
  const [teamsLoading, setTeamsLoading] = useState(false);
  const [teamError, setTeamError] = useState<string | null>(null);
  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null);
  const [activeView, setActiveView] = useState<WorkspaceView>("board");
  const [teamsReloadKey, setTeamsReloadKey] = useState(0);

  useEffect(() => {
    if (!user) {
      setTeams([]);
      setSelectedTeamId(null);
      setTeamError(null);
      setTeamsLoading(false);
      setActiveView("board");
      return;
    }

    let active = true;
    setTeamsLoading(true);
    setTeamError(null);

    listTeams()
      .then((nextTeams) => {
        if (!active) {
          return;
        }
        setTeams(nextTeams);
        setSelectedTeamId((currentTeamId) => {
          if (currentTeamId && findTeam(nextTeams, currentTeamId)) {
            return currentTeamId;
          }
          return firstTeamId(nextTeams);
        });
      })
      .catch(() => {
        if (active) {
          setTeamError("无法加载团队列表");
        }
      })
      .finally(() => {
        if (active) {
          setTeamsLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [user, teamsReloadKey]);

  const selectedTeam = useMemo(
    () => (selectedTeamId === null ? null : findTeam(teams, selectedTeamId)),
    [teams, selectedTeamId]
  );
  const canManageSelectedTeam = useMemo(
    () => canManageTeam(teams, selectedTeamId, Boolean(user?.superAdmin)),
    [teams, selectedTeamId, user?.superAdmin]
  );
  const visibleView = resolveView(activeView, user?.superAdmin ?? false, canManageSelectedTeam);

  if (loading) {
    return (
      <main className="loading-screen" role="status">
        正在加载
      </main>
    );
  }

  if (!user) {
    return <LoginPage />;
  }

  return (
    <AppShell
      activeView={visibleView}
      onLogout={logout}
      onSelectTeam={setSelectedTeamId}
      onSelectView={setActiveView}
      selectedTeam={selectedTeam}
      selectedTeamId={selectedTeamId}
      teamError={teamError}
      teams={teams}
      teamsLoading={teamsLoading}
      user={user}
    >
      {renderWorkspace(visibleView, {
        canManageSelectedTeam,
        currentUserId: user.id,
        currentUser: user,
        onCurrentUserUpdated: setCurrentUser,
        onTeamsChanged: () => setTeamsReloadKey((current) => current + 1),
        selectedTeam,
        superAdmin: user.superAdmin,
        canDeleteTeam: canDeleteSelectedTeam(user.superAdmin, selectedTeam),
        teamsLoading
      })}
    </AppShell>
  );
}

function renderWorkspace(
  view: WorkspaceView,
  context: {
    canManageSelectedTeam: boolean;
    currentUser: CurrentUser;
    currentUserId: number;
    onCurrentUserUpdated: (user: CurrentUser) => void;
    onTeamsChanged: () => void;
    selectedTeam: Team | null;
    superAdmin: boolean;
    canDeleteTeam: boolean;
    teamsLoading: boolean;
  }
) {
  if (view === "team-admin" && (context.superAdmin || context.canManageSelectedTeam)) {
    return (
      <TeamAdminPage
        selectedTeam={context.selectedTeam}
        onTeamsChanged={context.onTeamsChanged}
        canDeleteTeam={context.canDeleteTeam}
      />
    );
  }
  if (view === "sprints" && context.canManageSelectedTeam) {
    return <SprintPage teamId={context.selectedTeam?.id ?? null} />;
  }
  if (view === "recycle-bin" && context.canManageSelectedTeam) {
    return <RecycleBinPage teamId={context.selectedTeam?.id ?? null} />;
  }
  if (view === "users") {
    return <UserAdminPage currentUserId={context.currentUserId} />;
  }
  if (view === "profile") {
    return <ProfilePage currentUser={context.currentUser} onUserUpdated={context.onCurrentUserUpdated} />;
  }
  if (view === "snapshots") {
    return <SnapshotSettingsPage />;
  }
  return (
    <BoardPage
      canManageSelectedTeam={context.canManageSelectedTeam}
      currentUserId={context.currentUserId}
      selectedTeam={context.selectedTeam}
      teamsLoading={context.teamsLoading}
    />
  );
}

function resolveView(view: WorkspaceView, superAdmin: boolean, canManageSelectedTeam: boolean) {
  if ((view === "users" || view === "snapshots") && !superAdmin) {
    return "board";
  }
  if (view === "team-admin" && !superAdmin && !canManageSelectedTeam) {
    return "board";
  }
  if ((view === "sprints" || view === "recycle-bin") && !canManageSelectedTeam) {
    return "board";
  }
  return view;
}

function canDeleteSelectedTeam(superAdmin: boolean, selectedTeam: Team | null) {
  if (selectedTeam === null) {
    return false;
  }
  if (superAdmin) {
    return true;
  }
  return selectedTeam.role === "TEAM_CREATOR";
}

function firstTeamId(teams: Team[]): number | null {
  if (teams.length === 0) {
    return null;
  }
  return teams[0].id;
}

function findTeam(teams: Team[], teamId: number): Team | null {
  for (const team of teams) {
    if (team.id === teamId) {
      return team;
    }
    const childMatch = findTeam(team.children, teamId);
    if (childMatch) {
      return childMatch;
    }
  }
  return null;
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

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
