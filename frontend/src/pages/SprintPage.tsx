import { FormEvent, useEffect, useState } from "react";
import * as defaultApi from "../api/sprints";
import { ApiError } from "../api/client";
import { Sprint } from "../types";

export interface SprintPageApi {
  listSprints: (teamId: number) => Promise<Sprint[]>;
  createSprint: (teamId: number, values: { name: string }) => Promise<Sprint>;
  updateSprint: (sprintId: number, values: { name?: string; active?: boolean }, teamId?: number) => Promise<Sprint>;
  deleteSprint: (sprintId: number, teamId?: number) => Promise<void>;
}

interface SprintPageProps {
  api?: SprintPageApi;
  teamId: number | null;
}

export default function SprintPage({ api = defaultApi, teamId }: SprintPageProps) {
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [newSprintName, setNewSprintName] = useState("");
  const [renaming, setRenaming] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [confirmingSprint, setConfirmingSprint] = useState<{ id: number; name: string } | null>(null);

  useEffect(() => {
    if (teamId === null) {
      setSprints([]);
      setLoading(false);
      setError(null);
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);

    api
      .listSprints(teamId)
      .then((nextSprints) => {
        if (active) {
          setSprints(nextSprints);
          setRenaming(namesById(nextSprints));
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(errorMessage(cause, "无法加载 Sprint"));
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
        <p>选择团队后可以管理该团队的 Sprint。</p>
      </section>
    );
  }

  const currentTeamId = teamId;

  async function createSprint(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!newSprintName.trim()) {
      return;
    }
    await runAction(async () => {
      await api.createSprint(currentTeamId, { name: newSprintName.trim() });
      setNewSprintName("");
    });
  }

  async function updateSprintName(sprint: Sprint) {
    if (sprint.teamId !== currentTeamId) {
      return;
    }
    const name = renaming[sprint.id]?.trim();
    if (!name || name === sprint.name) {
      return;
    }
    await runAction(() => api.updateSprint(sprint.id, { name }, currentTeamId));
  }

  function requestDeleteSprint(sprint: Sprint) {
    if (sprint.teamId !== currentTeamId) {
      return;
    }
    setConfirmingSprint({ id: sprint.id, name: sprint.name });
  }

  async function confirmDeleteSprint() {
    if (!confirmingSprint) {
      return;
    }
    const request = confirmingSprint;
    setConfirmingSprint(null);
    await runAction(() => api.deleteSprint(request.id, currentTeamId));
  }

  async function runAction(action: () => Promise<void | Sprint>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      setReloadKey((current) => current + 1);
    } catch (cause: unknown) {
      setError(errorMessage(cause, "Sprint 操作失败"));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="board-shell admin-page" aria-labelledby="sprints-title">
      <div className="board-toolbar">
        <div>
          <p className="workspace-kicker">团队管理</p>
          <h2 id="sprints-title">Sprint 管理</h2>
        </div>
      </div>

      {loading && (
        <p className="board-loading" role="status">
          正在加载 Sprint
        </p>
      )}
      {error && <p className="form-error">{error}</p>}

      <form className="admin-inline-form" onSubmit={createSprint}>
        <label className="field">
          <span>Sprint 名称</span>
          <input value={newSprintName} onChange={(event) => setNewSprintName(event.target.value)} />
        </label>
        <button type="submit" disabled={busy || !newSprintName.trim()}>
          创建 Sprint
        </button>
      </form>

      <div className="table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {sprints.map((sprint) => (
              <tr key={sprint.id}>
                <td>
                  <span className="table-primary">{sprint.name}</span>
                  <input
                    aria-label={`Sprint 名称 ${sprint.name}`}
                    value={renaming[sprint.id] ?? sprint.name}
                    onChange={(event) =>
                      setRenaming((current) => ({ ...current, [sprint.id]: event.target.value }))
                    }
                  />
                </td>
                <td>{sprint.active ? "启用" : "停用"}</td>
                <td>
                  <div className="row-actions">
                    {sprint.teamId === currentTeamId ? (
                      <>
                        <button type="button" className="secondary-button" disabled={busy} onClick={() => void updateSprintName(sprint)}>
                          保存名称
                        </button>
                        <button
                          type="button"
                          disabled={busy}
                          aria-label={`${sprint.active ? "停用" : "启用"} ${sprint.name}`}
                          onClick={() => void runAction(() => api.updateSprint(sprint.id, { active: !sprint.active }, currentTeamId))}
                        >
                          {sprint.active ? "停用" : "启用"}
                        </button>
                        <button
                          type="button"
                          className="delete-button"
                          disabled={busy}
                          aria-label={`删除 ${sprint.name}`}
                          onClick={() => requestDeleteSprint(sprint)}
                        >
                          删除
                        </button>
                      </>
                    ) : (
                      <span>仅本团队可操作</span>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {!loading && sprints.length === 0 && (
              <tr>
                <td colSpan={3}>暂无 Sprint</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {confirmingSprint && (
        <div className="modal-backdrop">
          <section
            className="task-modal confirm-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-sprint-delete-title"
          >
            <div className="modal-heading">
              <div>
            <p className="workspace-kicker">不可撤销</p>
                <h3 id="confirm-sprint-delete-title">确认删除 Sprint</h3>
              </div>
            </div>
            <p>确认删除 Sprint“{confirmingSprint.name}”吗？删除后不可恢复。</p>
            <div className="modal-actions">
              <button type="button" className="secondary-button" onClick={() => setConfirmingSprint(null)}>
                取消
              </button>
              <button type="button" disabled={busy} onClick={() => void confirmDeleteSprint()}>
                确认删除
              </button>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}

function namesById(sprints: Sprint[]) {
  return sprints.reduce<Record<number, string>>((record, sprint) => {
    record[sprint.id] = sprint.name;
    return record;
  }, {});
}

function errorMessage(cause: unknown, fallback: string) {
  if (cause instanceof ApiError) {
    return cause.message || fallback;
  }
  return fallback;
}
