import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import {
  deleteCurrentUserAvatar,
  updateCurrentUser,
  updateCurrentUserPassword,
  uploadCurrentUserAvatar,
  UpdateCurrentUserPasswordPayload,
  UpdateCurrentUserPayload
} from "../api/profile";
import UserAvatar from "../components/UserAvatar";
import { CurrentUser } from "../types";
import { ApiError } from "../api/client";

interface ProfileApi {
  deleteCurrentUserAvatar: () => Promise<CurrentUser>;
  uploadCurrentUserAvatar: (file: File) => Promise<CurrentUser>;
  updateCurrentUser: (payload: UpdateCurrentUserPayload) => Promise<CurrentUser>;
  updateCurrentUserPassword: (payload: UpdateCurrentUserPasswordPayload) => Promise<CurrentUser>;
}

interface ProfilePageProps {
  currentUser: CurrentUser;
  onUserUpdated: (user: CurrentUser) => void;
  api?: ProfileApi;
}

export default function ProfilePage({
  currentUser,
  onUserUpdated,
  api = {
    deleteCurrentUserAvatar,
    uploadCurrentUserAvatar,
    updateCurrentUser,
    updateCurrentUserPassword
  }
}: ProfilePageProps) {
  const [displayName, setDisplayName] = useState(currentUser.displayName);
  const [displayNameError, setDisplayNameError] = useState<string | null>(null);
  const [profileMessage, setProfileMessage] = useState<string | null>(null);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordMessage, setPasswordMessage] = useState<string | null>(null);

  const [avatarMessage, setAvatarMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setDisplayName(currentUser.displayName);
  }, [currentUser.displayName]);

  async function saveDisplayName(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setDisplayNameError(null);
    setProfileMessage(null);
    if (!displayName.trim()) {
      setDisplayNameError("显示名称不能为空");
      return;
    }
    if (displayName === currentUser.displayName) {
      return;
    }

    setBusy(true);
    try {
      onUserUpdated(await api.updateCurrentUser({ displayName: displayName.trim() }));
      setProfileMessage("资料已更新");
    } catch {
      setDisplayNameError("无法更新资料");
    } finally {
      setBusy(false);
    }
  }

  async function changePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPasswordError(null);
    setPasswordMessage(null);

    if (!newPassword.trim() || newPassword.trim() !== confirmPassword.trim()) {
      setPasswordError("两次输入的新密码不一致");
      return;
    }

    const payload: UpdateCurrentUserPasswordPayload = { newPassword };
    if (!currentUser.superAdmin) {
      payload.currentPassword = currentPassword.trim();
      if (!payload.currentPassword) {
        setPasswordError("当前密码不能为空");
        return;
      }
    }

    setBusy(true);
    try {
      await api.updateCurrentUserPassword(payload);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setPasswordMessage("密码已更新");
    } catch (cause: unknown) {
      if (cause instanceof ApiError && cause.status === 403) {
        setPasswordError("当前密码不正确");
      } else {
        setPasswordError("无法更新密码");
      }
    } finally {
      setBusy(false);
    }
  }

  async function uploadAvatar(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    setBusy(true);
    setAvatarMessage(null);
    try {
      onUserUpdated(await api.uploadCurrentUserAvatar(file));
    } catch {
      setAvatarMessage("头像上传失败");
    } finally {
      setBusy(false);
      event.target.value = "";
    }
  }

  async function deleteAvatar() {
    setBusy(true);
    setAvatarMessage(null);
    try {
      onUserUpdated(await api.deleteCurrentUserAvatar());
    } catch {
      setAvatarMessage("头像删除失败");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="board-shell profile-page" aria-labelledby="profile-title">
      <div className="board-toolbar">
        <div>
          <p className="workspace-kicker">个人资料</p>
          <h2 id="profile-title">个人资料</h2>
        </div>
      </div>

      <div className="table-wrap profile-section">
        <h3 className="workspace-kicker">当前身份</h3>
        <div className="current-user">
          <UserAvatar avatarUrl={currentUser.avatarUrl} displayName={currentUser.displayName} username={currentUser.username} />
          <div className="current-user-details">
            <span className="current-user-name">{currentUser.displayName}</span>
            <span>{currentUser.username}</span>
          </div>
          <div className="profile-buttons">
            <span className="table-label">{currentUser.superAdmin ? "超级管理员" : "普通用户"}</span>
            {avatarMessage && <p className="avatar-error">{avatarMessage}</p>}
          </div>
        </div>
      </div>

      <div className="admin-grid-form profile-section">
        <h3 className="workspace-kicker">头像</h3>
        <div className="avatar-actions">
          <label className={busy ? "avatar-upload-button disabled" : "avatar-upload-button"}>
            上传头像
            <input
              accept="image/png,image/jpeg,image/webp,image/gif"
              disabled={busy}
              onChange={(event) => void uploadAvatar(event)}
              type="file"
            />
          </label>
          {currentUser.avatarUrl && (
            <button
              className="secondary-button avatar-remove-button"
              type="button"
              disabled={busy}
              onClick={() => void deleteAvatar()}
            >
              移除头像
            </button>
          )}
        </div>
      </div>

      <form className="admin-grid-form profile-section" onSubmit={saveDisplayName}>
        <h3 className="workspace-kicker">显示名称</h3>
        <label className="field">
          <span>显示名称</span>
          <input
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            disabled={busy}
          />
        </label>
        {displayNameError && <p className="form-error">{displayNameError}</p>}
        {profileMessage && <p className="form-success">{profileMessage}</p>}
        <button disabled={busy} type="submit">
          保存显示名称
        </button>
      </form>

      <form className="admin-grid-form profile-section" onSubmit={changePassword}>
        <h3 className="workspace-kicker">修改密码</h3>
        {!currentUser.superAdmin && (
          <label className="field">
            <span>当前密码</span>
            <input
              type="password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              disabled={busy}
            />
          </label>
        )}
        <label className="field">
          <span>新密码</span>
          <input
            type="password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            disabled={busy}
          />
        </label>
        <label className="field">
          <span>确认新密码</span>
          <input
            type="password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            disabled={busy}
          />
        </label>
        {passwordError && <p className="form-error">{passwordError}</p>}
        {passwordMessage && <p className="form-success">{passwordMessage}</p>}
        <button disabled={busy} type="submit">
          修改密码
        </button>
      </form>
    </section>
  );
}
