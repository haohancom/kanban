import { FormEvent, useEffect, useState } from "react";
import {
  addTeamMember,
  createTeam,
  listAssignableUsers,
  listTeamMembers,
  removeTeamMember,
  updateTeam,
  updateTeamMember
} from "../api/teams";
import { ApiError } from "../api/client";
import { AssignableUser, Team, TeamMember, TeamRole } from "../types";

export interface TeamAdminApi {
  listMembers: (teamId: number) => Promise<TeamMember[]>;
  listAssignableUsers: (teamId: number) => Promise<AssignableUser[]>;
  createTeam: (values: { name: string; parentId?: number | null }) => Promise<Team>;
  updateTeam: (teamId: number, values: { name: string }) => Promise<Team>;
  addMember: (teamId: number, values: { userId: number; role: TeamRole }) => Promise<TeamMember>;
  updateMember: (teamId: number, membershipId: number, values: { role: TeamRole }) => Promise<TeamMember>;
  removeMember: (teamId: number, membershipId: number) => Promise<void>;
}

interface TeamAdminPageProps {
  api?: TeamAdminApi;
  onTeamsChanged: () => void;
  selectedTeam: Team | null;
}

const defaultTeamAdminApi: TeamAdminApi = {
  listMembers: listTeamMembers,
  listAssignableUsers,
  createTeam,
  updateTeam,
  addMember: addTeamMember,
  updateMember: updateTeamMember,
  removeMember: removeTeamMember
};

const roleOptions: Array<{ value: TeamRole; label: string }> = [
  { value: "TEAM_MEMBER", label: "成员" },
  { value: "TEAM_ADMIN", label: "管理员" },
  { value: "TEAM_CREATOR", label: "创建者" }
];

export default function TeamAdminPage({
  api = defaultTeamAdminApi,
  onTeamsChanged,
  selectedTeam
}: TeamAdminPageProps) {
  const [members, setMembers] = useState<TeamMember[]>([]);
  const [assignableUsers, setAssignableUsers] = useState<AssignableUser[]>([]);
  const [rootName, setRootName] = useState("");
  const [teamName, setTeamName] = useState(selectedTeam?.name ?? "");
  const [childName, setChildName] = useState("");
  const [memberUserId, setMemberUserId] = useState("");
  const [memberRole, setMemberRole] = useState<TeamRole>("TEAM_MEMBER");
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [membersReloadKey, setMembersReloadKey] = useState(0);
  const selectedTeamId = selectedTeam?.id ?? null;

  useEffect(() => {
    setTeamName(selectedTeam?.name ?? "");
    setMemberUserId("");
    setMemberRole("TEAM_MEMBER");
  }, [selectedTeam]);

  useEffect(() => {
    if (selectedTeamId === null) {
      setMembers([]);
      setAssignableUsers([]);
      setLoading(false);
      setError(null);
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);

    Promise.all([api.listMembers(selectedTeamId), api.listAssignableUsers(selectedTeamId)])
      .then(([nextMembers, nextAssignableUsers]) => {
        if (active) {
          setMembers(nextMembers);
          setAssignableUsers(nextAssignableUsers);
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(errorMessage(cause, "无法加载团队成员"));
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
  }, [api, selectedTeamId, membersReloadKey]);

  async function createRootTeam(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!rootName.trim()) {
      return;
    }
    await runTeamAction(async () => {
      await api.createTeam({ name: rootName.trim(), parentId: null });
      setRootName("");
      setMessage("根团队已创建");
    });
  }

  async function renameTeam(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (selectedTeamId === null || !teamName.trim()) {
      return;
    }
    await runTeamAction(async () => {
      await api.updateTeam(selectedTeamId, { name: teamName.trim() });
      setMessage("团队名称已保存");
    });
  }

  async function createChildTeam(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (selectedTeamId === null || !childName.trim()) {
      return;
    }
    await runTeamAction(async () => {
      await api.createTeam({ name: childName.trim(), parentId: selectedTeamId });
      setChildName("");
      setMessage("子团队已创建");
    });
  }

  async function addMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (selectedTeamId === null || !memberUserId.trim()) {
      return;
    }
    await runMemberAction(async () => {
      await api.addMember(selectedTeamId, { userId: Number(memberUserId), role: memberRole });
      setMemberUserId("");
      setMemberRole("TEAM_MEMBER");
      setMessage("成员已添加");
    });
  }

  async function runTeamAction(action: () => Promise<void>) {
    await runAction(async () => {
      await action();
      onTeamsChanged();
    });
  }

  async function runMemberAction(action: () => Promise<void>) {
    await runAction(async () => {
      await action();
      setMembersReloadKey((current) => current + 1);
    });
  }

  async function runAction(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await action();
    } catch (cause: unknown) {
      setError(errorMessage(cause, "团队管理操作失败"));
    } finally {
      setBusy(false);
    }
  }

  const existingMemberUserIds = new Set(members.map((member) => member.userId));
  const assignableMemberOptions = assignableUsers.filter((user) => !existingMemberUserIds.has(user.id));

  return (
    <section className="board-shell admin-page" aria-labelledby="team-admin-title">
      <div className="board-toolbar">
        <div>
          <p className="workspace-kicker">团队管理</p>
          <h2 id="team-admin-title">{selectedTeam?.name ?? "团队"}</h2>
        </div>
      </div>

      {error && <p className="form-error">{error}</p>}
      {message && <p className="form-success">{message}</p>}

      <form className="admin-inline-form" onSubmit={createRootTeam}>
        <label className="field">
          <span>根团队名称</span>
          <input value={rootName} onChange={(event) => setRootName(event.target.value)} />
        </label>
        <button type="submit" disabled={busy || !rootName.trim()}>
          创建根团队
        </button>
      </form>

      {selectedTeamId === null ? (
        <div className="workspace-empty inline-empty">
          <h2>请选择团队</h2>
          <p>选择团队后可以编辑名称、创建子团队并维护成员。</p>
        </div>
      ) : (
        <>
          <form className="admin-inline-form" onSubmit={renameTeam}>
            <label className="field">
              <span>团队名称</span>
              <input value={teamName} onChange={(event) => setTeamName(event.target.value)} required />
            </label>
            <button type="submit" disabled={busy || !teamName.trim()}>
              保存团队名称
            </button>
          </form>

          <form className="admin-inline-form" onSubmit={createChildTeam}>
            <label className="field">
              <span>子团队名称</span>
              <input value={childName} onChange={(event) => setChildName(event.target.value)} />
            </label>
            <button type="submit" disabled={busy || !childName.trim()}>
              创建子团队
            </button>
          </form>

          <form className="admin-inline-form" onSubmit={addMember}>
            <label className="field">
              <span>成员</span>
              <select value={memberUserId} onChange={(event) => setMemberUserId(event.target.value)}>
                <option value="">选择用户</option>
                {assignableMemberOptions.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.displayName} ({user.username})
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>成员角色</span>
              <select value={memberRole} onChange={(event) => setMemberRole(event.target.value as TeamRole)}>
                {roleOptions.map((role) => (
                  <option key={role.value} value={role.value}>
                    {role.label}
                  </option>
                ))}
              </select>
            </label>
            <button type="submit" disabled={busy || !memberUserId.trim()}>
              添加成员
            </button>
          </form>

          {loading && (
            <p className="board-loading" role="status">
              正在加载团队成员
            </p>
          )}

          <div className="table-wrap">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>显示名称</th>
                  <th>角色</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {members.map((member) => (
                  <tr key={member.id}>
                    <td>{member.username}</td>
                    <td>{member.displayName}</td>
                    <td>
                      <select
                        aria-label={`角色 ${member.username}`}
                        value={member.role}
                        onChange={(event) =>
                          void runMemberAction(() =>
                            api
                              .updateMember(selectedTeamId, member.id, { role: event.target.value as TeamRole })
                              .then(() => undefined)
                          )
                        }
                      >
                        {roleOptions.map((role) => (
                          <option key={role.value} value={role.value}>
                            {role.label}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="secondary-button"
                        disabled={busy}
                        onClick={() => void runMemberAction(() => api.removeMember(selectedTeamId, member.id))}
                      >
                        移除
                      </button>
                    </td>
                  </tr>
                ))}
                {!loading && members.length === 0 && (
                  <tr>
                    <td colSpan={4}>暂无成员</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}

function errorMessage(cause: unknown, fallback: string) {
  if (cause instanceof ApiError) {
    return cause.message || fallback;
  }
  return fallback;
}
