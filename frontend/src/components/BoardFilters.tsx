import { BoardTaskFilters, TaskStatus } from "../types";

interface FilterTeam {
  id: number;
  name: string;
}

interface FilterMember {
  id: number;
  displayName: string;
}

interface FilterSprint {
  id: number;
  name: string;
}

interface BoardFiltersProps {
  teams: FilterTeam[];
  members: FilterMember[];
  sprints: FilterSprint[];
  value: BoardTaskFilters;
  onChange: (value: BoardTaskFilters) => void;
}

const statusLabels: Record<TaskStatus, string> = {
  TODO: "待开始",
  IN_PROGRESS: "进行中",
  DONE: "已完成"
};

export default function BoardFilters({ teams, members, sprints, value, onChange }: BoardFiltersProps) {
  function updateNumberFilter(key: "subTeamId" | "memberId" | "sprintId", nextValue: string) {
    onChange({
      ...value,
      [key]: nextValue === "" ? undefined : Number(nextValue)
    });
  }

  function updateStatus(nextValue: string) {
    onChange({
      ...value,
      status: nextValue === "" ? undefined : (nextValue as TaskStatus)
    });
  }

  return (
    <form className="board-filters" aria-label="看板筛选">
      <label className="field compact-field">
        <span>子团队</span>
        <select
          value={value.subTeamId ?? ""}
          onChange={(event) => updateNumberFilter("subTeamId", event.target.value)}
        >
          <option value="">全部团队</option>
          {teams.map((team) => (
            <option key={team.id} value={team.id}>
              {team.name}
            </option>
          ))}
        </select>
      </label>

      <label className="field compact-field">
        <span>成员</span>
        <select
          value={value.memberId ?? ""}
          onChange={(event) => updateNumberFilter("memberId", event.target.value)}
        >
          <option value="">全部成员</option>
          {members.map((member) => (
            <option key={member.id} value={member.id}>
              {member.displayName}
            </option>
          ))}
        </select>
      </label>

      <label className="field compact-field">
        <span>状态</span>
        <select value={value.status ?? ""} onChange={(event) => updateStatus(event.target.value)}>
          <option value="">全部状态</option>
          {Object.entries(statusLabels).map(([status, label]) => (
            <option key={status} value={status}>
              {label}
            </option>
          ))}
        </select>
      </label>

      <label className="field compact-field">
        <span>冲刺</span>
        <select
          value={value.sprintId ?? ""}
          onChange={(event) => updateNumberFilter("sprintId", event.target.value)}
        >
          <option value="">全部冲刺</option>
          {sprints.map((sprint) => (
            <option key={sprint.id} value={sprint.id}>
              {sprint.name}
            </option>
          ))}
        </select>
      </label>
    </form>
  );
}
