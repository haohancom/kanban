import { DragEvent, useState } from "react";
import { BoardTask, TaskStatus } from "../types";
import UserAvatar from "./UserAvatar";

interface KanbanBoardProps {
  tasks: BoardTask[];
  canMoveTask?: (task: BoardTask) => boolean;
  onEdit: (task: BoardTask) => void;
  onMove?: (task: BoardTask, status: TaskStatus) => void;
}

const columns: Array<{ status: TaskStatus; label: string }> = [
  { status: "TODO", label: "待开始" },
  { status: "IN_PROGRESS", label: "进行中" },
  { status: "DONE", label: "已完成" }
];

const taskDragType = "application/x-kanban-task-id";

export default function KanbanBoard({ tasks, canMoveTask, onEdit, onMove }: KanbanBoardProps) {
  const [draggingTaskId, setDraggingTaskId] = useState<number | null>(null);
  const draggingTask = draggingTaskId === null ? null : tasks.find((task) => task.id === draggingTaskId) ?? null;

  function canMove(task: BoardTask) {
    return Boolean(onMove && (canMoveTask ? canMoveTask(task) : true));
  }

  function handleDragStart(event: DragEvent<HTMLElement>, task: BoardTask) {
    if (!canMove(task)) {
      event.preventDefault();
      return;
    }

    setDraggingTaskId(task.id);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData(taskDragType, String(task.id));
    }
  }

  function handleDragOver(event: DragEvent<HTMLElement>, status: TaskStatus) {
    if (!draggingTask || draggingTask.status === status || !canMove(draggingTask)) {
      return;
    }

    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = "move";
    }
  }

  function handleDrop(event: DragEvent<HTMLElement>, status: TaskStatus) {
    event.preventDefault();
    const transferredTaskId = event.dataTransfer ? Number(event.dataTransfer.getData(taskDragType)) : NaN;
    const taskId = draggingTaskId ?? transferredTaskId;
    const task = tasks.find((candidate) => candidate.id === taskId);
    setDraggingTaskId(null);

    if (!task || task.status === status || !canMove(task) || !onMove) {
      return;
    }

    onMove(task, status);
  }

  return (
    <div className="kanban-board">
      {columns.map((column) => {
        const columnTasks = tasks.filter((task) => task.status === column.status);
        const canDropHere = Boolean(
          draggingTask && draggingTask.status !== column.status && canMove(draggingTask)
        );

        return (
          <section
            key={column.status}
            className={`kanban-column${canDropHere ? " drop-target" : ""}`}
            aria-label={column.label}
            onDragOver={(event) => handleDragOver(event, column.status)}
            onDrop={(event) => handleDrop(event, column.status)}
          >
            <div className="kanban-column-heading">
              <h3>{column.label}</h3>
              <span>{columnTasks.length}</span>
            </div>

            <div className="task-card-list">
              {columnTasks.length === 0 ? (
                <p className="empty-column">暂无任务</p>
              ) : (
                columnTasks.map((task) => {
                  const movable = canMove(task);
                  const dragging = draggingTaskId === task.id;

                  return (
                      <article
                      key={task.id}
                      className={`task-card${movable ? " task-card-draggable" : ""}${
                        dragging ? " is-dragging" : ""
                      }`}
                      draggable={movable}
                      onDoubleClick={() => onEdit(task)}
                      onDragEnd={() => setDraggingTaskId(null)}
                      onDragStart={(event) => handleDragStart(event, task)}
                    >
                      <div>
                        <p className="task-team">{task.teamName}</p>
                        <h4>{task.title}</h4>
                      </div>

                      <div className="task-meta">
                        {task.sprintName && <span>{task.sprintName}</span>}
                        {hasText(task.remarks) && <span>有备注</span>}
                        {hasText(task.risks) && <span className="risk-chip">有风险</span>}
                      </div>

                      <div className="task-card-footer">
                        <div className="task-card-assignee">
                          <UserAvatar
                            avatarUrl={undefined}
                            displayName={task.assigneeDisplayName || task.createdByDisplayName || "未分配"}
                            username={task.assigneeUsername || task.assigneeDisplayName || "未分配"}
                          />
                          <span className="task-card-assignee-name">
                            {task.assigneeDisplayName || task.createdByDisplayName || "未分配"}
                          </span>
                        </div>
                      </div>
                    </article>
                  );
                })
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
