import { useEffect, useMemo, useState } from "react";
import * as defaultApi from "../api/recycleBin";
import { ApiError } from "../api/client";

export interface DeletedTask {
  id: number;
  title: string;
  teamName: string;
  deletedAt?: string | null;
}

export interface RecycleBinApi {
  listDeletedTasks: (teamId: number) => Promise<DeletedTask[]>;
  bulkDeleteTasks: (taskIds: number[]) => Promise<void>;
  restoreTask: (taskId: number) => Promise<void>;
  deleteTaskForever: (taskId: number) => Promise<void>;
  deleteAllTasks?: (teamId: number) => Promise<void>;
}

interface RecycleBinPageProps {
  api?: RecycleBinApi;
  teamId: number | null;
}

type ConfirmRequest =
  | { kind: "selected"; taskIds: number[] }
  | { kind: "single"; taskId: number; title: string }
  | { kind: "all"; teamId: number; count: number };

export default function RecycleBinPage({ api = defaultApi, teamId }: RecycleBinPageProps) {
  const [tasks, setTasks] = useState<DeletedTask[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmRequest, setConfirmRequest] = useState<ConfirmRequest | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const selectedTaskIds = useMemo(() => Array.from(selectedIds), [selectedIds]);

  useEffect(() => {
    if (teamId === null) {
      setTasks([]);
      setSelectedIds(new Set());
      setError(null);
      setLoading(false);
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);

    api
      .listDeletedTasks(teamId)
      .then((nextTasks) => {
        if (active) {
          setTasks(nextTasks);
          setSelectedIds(new Set());
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(errorMessage(cause, "无法加载回收站"));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [api, teamId, reloadKey]);

  if (teamId === null) {
    return (
      <section className="workspace-empty">
        <h2>请选择团队</h2>
        <p>选择团队后可以查看该团队树下已删除的任务。</p>
      </section>
    );
  }

  async function runAction(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      setReloadKey((current) => current + 1);
    } catch (cause: unknown) {
      setError(errorMessage(cause, "回收站操作失败"));
    } finally {
      setBusy(false);
    }
  }

  function toggleTask(taskId: number, selected: boolean) {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (selected) {
        next.add(taskId);
      } else {
        next.delete(taskId);
      }
      return next;
    });
  }

  async function confirmDelete() {
    if (!confirmRequest) {
      return;
    }

    const request = confirmRequest;
    setConfirmRequest(null);

    if (request.kind === "selected") {
      await runAction(() => api.bulkDeleteTasks(request.taskIds));
      return;
    }

    if (request.kind === "single") {
      await runAction(() => api.deleteTaskForever(request.taskId));
      return;
    }

    if (api.deleteAllTasks) {
      await runAction(() => api.deleteAllTasks ? api.deleteAllTasks(request.teamId) : Promise.resolve());
    }
  }

  return (
    <section className="board-shell admin-page" aria-labelledby="recycle-bin-title">
      <div className="board-toolbar">
        <div>
          <p className="workspace-kicker">团队工具</p>
          <h2 id="recycle-bin-title">回收站</h2>
        </div>
        <div className="toolbar-actions">
          <button
            type="button"
            className="secondary-button"
            disabled={busy || selectedTaskIds.length === 0}
            onClick={() => setConfirmRequest({ kind: "selected", taskIds: selectedTaskIds })}
          >
            永久删除所选
          </button>
          <button
            type="button"
            disabled={busy || tasks.length === 0 || !api.deleteAllTasks}
            onClick={() => setConfirmRequest({ kind: "all", teamId, count: tasks.length })}
          >
            清空回收站
          </button>
        </div>
      </div>

      {loading && (
        <p className="board-loading" role="status">
          正在加载回收站
        </p>
      )}
      {error && <p className="form-error">{error}</p>}

      <div className="table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>选择</th>
              <th>任务</th>
              <th>团队</th>
              <th>删除时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {tasks.map((task) => (
              <tr key={task.id}>
                <td>
                  <input
                    aria-label={`选择 ${task.title}`}
                    checked={selectedIds.has(task.id)}
                    type="checkbox"
                    onChange={(event) => toggleTask(task.id, event.target.checked)}
                  />
                </td>
                <td>{task.title}</td>
                <td>{task.teamName}</td>
                <td>{formatDate(task.deletedAt)}</td>
                <td>
                  <div className="row-actions">
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={busy}
                      onClick={() => void runAction(() => api.restoreTask(task.id))}
                    >
                      恢复
                    </button>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => setConfirmRequest({ kind: "single", taskId: task.id, title: task.title })}
                    >
                      永久删除
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {!loading && tasks.length === 0 && (
              <tr>
                <td colSpan={5}>暂无已删除任务</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {confirmRequest && (
        <div className="modal-backdrop">
          <section
            className="task-modal confirm-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-delete-title"
          >
            <div className="modal-heading">
              <div>
                <p className="workspace-kicker">不可撤销</p>
                <h3 id="confirm-delete-title">确认永久删除</h3>
              </div>
            </div>
            <p>{confirmMessage(confirmRequest)}</p>
            <div className="modal-actions">
              <button type="button" className="secondary-button" onClick={() => setConfirmRequest(null)}>
                取消
              </button>
              <button type="button" disabled={busy} onClick={() => void confirmDelete()}>
                确认删除
              </button>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}

function confirmMessage(request: ConfirmRequest) {
  if (request.kind === "selected") {
    return `将永久删除 ${request.taskIds.length} 个已选任务。`;
  }
  if (request.kind === "all") {
    return `将永久删除当前团队树回收站中的 ${request.count} 个任务。`;
  }
  return `将永久删除任务“${request.title}”。`;
}

function formatDate(value?: string | null) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString();
}

function errorMessage(cause: unknown, fallback: string) {
  if (cause instanceof ApiError) {
    return cause.message || fallback;
  }
  return fallback;
}
