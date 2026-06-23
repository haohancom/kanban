import { Team } from "../types";

interface TeamTreeProps {
  teams: Team[];
  selectedTeamId: number | null;
  onSelect: (teamId: number) => void;
}

export default function TeamTree({ teams, selectedTeamId, onSelect }: TeamTreeProps) {
  if (teams.length === 0) {
    return <p className="team-tree-empty">暂无团队</p>;
  }

  return (
    <nav aria-label="团队列表" className="team-tree">
      <TeamList teams={teams} selectedTeamId={selectedTeamId} onSelect={onSelect} />
    </nav>
  );
}

function TeamList({ teams, selectedTeamId, onSelect }: TeamTreeProps) {
  return (
    <ul>
      {teams.map((team) => (
        <li key={team.id}>
          <button
            aria-current={team.id === selectedTeamId ? "page" : undefined}
            className={team.id === selectedTeamId ? "team-tree-item selected" : "team-tree-item"}
            type="button"
            onClick={() => onSelect(team.id)}
          >
            <span>{team.name}</span>
            {team.role && <small>{roleLabel(team.role)}</small>}
          </button>
          {team.children.length > 0 && (
            <TeamList teams={team.children} selectedTeamId={selectedTeamId} onSelect={onSelect} />
          )}
        </li>
      ))}
    </ul>
  );
}

function roleLabel(role: Team["role"]) {
  if (role === "TEAM_CREATOR") {
    return "创建者";
  }
  if (role === "TEAM_ADMIN") {
    return "管理员";
  }
  return "成员";
}
