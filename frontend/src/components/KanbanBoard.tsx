import { BoardTask, TaskStatus } from "../types";

interface KanbanBoardProps {
  tasks: BoardTask[];
  onDelete?: (task: BoardTask) => void;
  onEdit: (task: BoardTask) => void;
  onMove?: (task: BoardTask, status: TaskStatus) => void;
}

const columns: Array<{ status: TaskStatus; label: string }> = [
  { status: "TODO", label: "待开始" },
  { status: "IN_PROGRESS", label: "进行中" },
  { status: "DONE", label: "已完成" }
];

export default function KanbanBoard({ tasks, onDelete, onEdit, onMove }: KanbanBoardProps) {
  return (
    <div className="kanban-board">
      {columns.map((column) => {
        const columnTasks = tasks.filter((task) => task.status === column.status);

        return (
          <section key={column.status} className="kanban-column" aria-label={column.label}>
            <div className="kanban-column-heading">
              <h3>{column.label}</h3>
              <span>{columnTasks.length}</span>
            </div>

            <div className="task-card-list">
              {columnTasks.length === 0 ? (
                <p className="empty-column">暂无任务</p>
              ) : (
                columnTasks.map((task) => (
                  <article key={task.id} className="task-card">
                    <div>
                      <p className="task-team">{task.teamName}</p>
                      <h4>{task.title}</h4>
                    </div>

                    <div className="task-meta">
                      {task.assigneeDisplayName && <span>{task.assigneeDisplayName}</span>}
                      {task.sprintName && <span>{task.sprintName}</span>}
                      {hasText(task.remarks) && <span>有备注</span>}
                      {hasText(task.risks) && <span className="risk-chip">有风险</span>}
                    </div>

                    <div className="task-card-actions">
                      <button type="button" className="secondary-button" onClick={() => onEdit(task)}>
                        编辑
                      </button>
                      {onDelete && (
                        <button
                          type="button"
                          className="secondary-button"
                          aria-label={`删除 ${task.title}`}
                          onClick={() => onDelete(task)}
                        >
                          删除
                        </button>
                      )}
                      {onMove && (
                        <label className="status-move">
                          <span>移动</span>
                          <select
                            aria-label={`移动 ${task.title} 状态`}
                            value={task.status}
                            onChange={(event) => onMove(task, event.target.value as TaskStatus)}
                          >
                            {columns.map((statusColumn) => (
                              <option key={statusColumn.status} value={statusColumn.status}>
                                {statusColumn.label}
                              </option>
                            ))}
                          </select>
                        </label>
                      )}
                    </div>
                  </article>
                ))
              )}
            </div>
          </section>
        );
      })}
    </div>
  );
}

function hasText(value: string | undefined) {
  return Boolean(value && value.trim());
}
