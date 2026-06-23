import { Team } from "../types";

interface BoardPageProps {
  selectedTeam: Team | null;
  teamsLoading: boolean;
}

export default function BoardPage({ selectedTeam, teamsLoading }: BoardPageProps) {
  if (teamsLoading) {
    return (
      <section className="workspace-empty" role="status">
        <h2>正在加载团队</h2>
      </section>
    );
  }

  if (!selectedTeam) {
    return (
      <section className="workspace-empty">
        <h2>请选择团队</h2>
        <p>团队创建后，这里会显示对应的任务看板。</p>
      </section>
    );
  }

  return (
    <section className="board-shell" aria-labelledby="board-title">
      <div>
        <p className="workspace-kicker">当前团队</p>
        <h2 id="board-title">{selectedTeam.name}</h2>
      </div>
      <div className="board-placeholder">
        <span>待办</span>
        <span>进行中</span>
        <span>已完成</span>
      </div>
    </section>
  );
}
