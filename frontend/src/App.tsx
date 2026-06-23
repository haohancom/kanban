import { useEffect, useMemo, useState } from "react";
import { listTeams } from "./api/teams";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import AppShell from "./components/AppShell";
import BoardPage from "./pages/BoardPage";
import LoginPage from "./pages/LoginPage";
import { Team } from "./types";

function AppContent() {
  const { user, loading, logout } = useAuth();
  const [teams, setTeams] = useState<Team[]>([]);
  const [teamsLoading, setTeamsLoading] = useState(false);
  const [teamError, setTeamError] = useState<string | null>(null);
  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null);

  useEffect(() => {
    if (!user) {
      setTeams([]);
      setSelectedTeamId(null);
      setTeamError(null);
      setTeamsLoading(false);
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
  }, [user]);

  const selectedTeam = useMemo(
    () => (selectedTeamId === null ? null : findTeam(teams, selectedTeamId)),
    [teams, selectedTeamId]
  );

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
      onLogout={logout}
      onSelectTeam={setSelectedTeamId}
      selectedTeam={selectedTeam}
      selectedTeamId={selectedTeamId}
      teamError={teamError}
      teams={teams}
      teamsLoading={teamsLoading}
      user={user}
    >
      <BoardPage selectedTeam={selectedTeam} teamsLoading={teamsLoading} />
    </AppShell>
  );
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

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
