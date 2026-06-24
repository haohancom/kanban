import { Dispatch, FormEvent, SetStateAction, useEffect, useState } from "react";
import * as defaultApi from "../api/users";
import { ApiError } from "../api/client";
import { UserAccount } from "../types";

export interface UserAdminApi {
  listUsers: () => Promise<UserAccount[]>;
  createUser: (values: {
    username: string;
    displayName: string;
    password: string;
    superAdmin: boolean;
  }) => Promise<UserAccount>;
  updateUser: (userId: number, values: { displayName?: string; superAdmin?: boolean }) => Promise<UserAccount>;
  resetUserPassword: (userId: number, password: string) => Promise<void>;
}

interface UserAdminPageProps {
  api?: UserAdminApi;
  currentUserId?: number;
}

export default function UserAdminPage({ api = defaultApi, currentUserId }: UserAdminPageProps) {
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [username, setUsername] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [superAdmin, setSuperAdmin] = useState(false);
  const [passwordsByUser, setPasswordsByUser] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    api
      .listUsers()
      .then((nextUsers) => {
        if (active) {
          setUsers(nextUsers);
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(errorMessage(cause, "无法加载用户"));
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
  }, [api, reloadKey]);

  async function createUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(async () => {
      await api.createUser({
        username: username.trim(),
        displayName: displayName.trim(),
        password,
        superAdmin
      });
      setUsername("");
      setDisplayName("");
      setPassword("");
      setSuperAdmin(false);
      setMessage("用户已创建");
    });
  }

  async function runAction(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await action();
      setReloadKey((current) => current + 1);
    } catch (cause: unknown) {
      setError(errorMessage(cause, "用户管理操作失败"));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="board-shell admin-page" aria-labelledby="users-title">
      <div className="board-toolbar">
        <div>
          <p className="workspace-kicker">系统管理</p>
          <h2 id="users-title">用户管理</h2>
        </div>
      </div>

      {loading && (
        <p className="board-loading" role="status">
          正在加载用户
        </p>
      )}
      {error && <p className="form-error">{error}</p>}
      {message && <p className="form-success">{message}</p>}

      <form className="admin-grid-form" onSubmit={createUser}>
        <label className="field">
          <span>用户名</span>
          <input value={username} onChange={(event) => setUsername(event.target.value)} required />
        </label>
        <label className="field">
          <span>显示名称</span>
          <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} required />
        </label>
        <label className="field">
          <span>初始密码</span>
          <input
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            type="password"
          />
        </label>
        <label className="checkbox-field form-checkbox">
          <input
            checked={superAdmin}
            onChange={(event) => setSuperAdmin(event.target.checked)}
            type="checkbox"
          />
          <span>创建为超管</span>
        </label>
        <button type="submit" disabled={busy || !username.trim() || !displayName.trim() || !password}>
          创建用户
        </button>
      </form>

      <div className="table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>用户名</th>
              <th>显示名称</th>
              <th>权限</th>
              <th>重置密码</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {users.map((account) => (
              <UserRow
                key={account.id}
                account={account}
                api={api}
                busy={busy}
                currentUserId={currentUserId}
                password={passwordsByUser[account.id] ?? ""}
                runAction={runAction}
                setMessage={setMessage}
                setPasswordsByUser={setPasswordsByUser}
              />
            ))}
            {!loading && users.length === 0 && (
              <tr>
                <td colSpan={5}>暂无用户</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function UserRow({
  account,
  api,
  busy,
  currentUserId,
  password,
  runAction,
  setMessage,
  setPasswordsByUser
}: {
  account: UserAccount;
  api: UserAdminApi;
  busy: boolean;
  currentUserId?: number;
  password: string;
  runAction: (action: () => Promise<void>) => Promise<void>;
  setMessage: Dispatch<SetStateAction<string | null>>;
  setPasswordsByUser: Dispatch<SetStateAction<Record<number, string>>>;
}) {
  const currentSuperAdmin = account.id === currentUserId && account.superAdmin;

  return (
    <tr>
      <td>{account.username}</td>
      <td>{account.displayName}</td>
      <td>{account.superAdmin ? "超级管理员" : "普通用户"}</td>
      <td>
        <input
          aria-label={`重置密码 ${account.username}`}
          type="password"
          value={password}
          onChange={(event) =>
            setPasswordsByUser((current) => ({ ...current, [account.id]: event.target.value }))
          }
        />
      </td>
      <td>
        <div className="row-actions">
          {currentSuperAdmin ? (
            <button
              type="button"
              className="secondary-button"
              disabled
              aria-label={`当前账号 ${account.username}`}
            >
              当前账号
            </button>
          ) : (
            <button
              type="button"
              className="secondary-button"
              disabled={busy}
              aria-label={`${account.superAdmin ? "撤销超管" : "授予超管"} ${account.username}`}
              onClick={() =>
                void runAction(() =>
                  api.updateUser(account.id, { superAdmin: !account.superAdmin }).then(() => undefined)
                )
              }
            >
              {account.superAdmin ? "撤销超管" : "授予超管"}
            </button>
          )}
          <button
            type="button"
            disabled={busy || !password.trim()}
            onClick={() =>
              void runAction(async () => {
                await api.resetUserPassword(account.id, password);
                setPasswordsByUser((current) => ({ ...current, [account.id]: "" }));
                setMessage("密码已重置");
              })
            }
          >
            重置密码 {account.username}
          </button>
        </div>
      </td>
    </tr>
  );
}

function errorMessage(cause: unknown, fallback: string) {
  if (cause instanceof ApiError) {
    return cause.message || fallback;
  }
  return fallback;
}
