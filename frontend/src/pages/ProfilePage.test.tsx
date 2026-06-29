import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import ProfilePage from "./ProfilePage";
import { CurrentUser } from "../types";
import { ApiError } from "../api/client";

const currentUser: CurrentUser = {
  id: 1,
  username: "member",
  displayName: "团队成员",
  superAdmin: false,
  avatarUrl: "/api/users/me/avatar?v=1"
};

describe("ProfilePage", () => {
  it("updates display name", async () => {
    const actor = userEvent.setup();
    const onUserUpdated = vi.fn();
    const api = {
      deleteCurrentUserAvatar: vi.fn(async () => currentUser),
      uploadCurrentUserAvatar: vi.fn(async () => currentUser),
      updateCurrentUser: vi.fn(async () => ({ ...currentUser, displayName: "新名字" })),
      updateCurrentUserPassword: vi.fn(async () => currentUser)
    };

    render(<ProfilePage currentUser={currentUser} onUserUpdated={onUserUpdated} api={api} />);

    await actor.clear(screen.getByLabelText("显示名称"));
    await actor.type(screen.getByLabelText("显示名称"), "新名字");
    await actor.click(screen.getByRole("button", { name: "保存显示名称" }));

    expect(api.updateCurrentUser).toHaveBeenCalledWith({ displayName: "新名字" });
    expect(onUserUpdated).toHaveBeenCalledWith({ ...currentUser, displayName: "新名字" });
    expect(screen.getByText("资料已更新")).toBeInTheDocument();
  });

  it("requires current password for normal users when changing password", async () => {
    const onUserUpdated = vi.fn();
    const api = {
      deleteCurrentUserAvatar: vi.fn(async () => currentUser),
      uploadCurrentUserAvatar: vi.fn(async () => currentUser),
      updateCurrentUser: vi.fn(async () => currentUser),
      updateCurrentUserPassword: vi.fn(async () => ({ ...currentUser, displayName: "团队成员" }))
    };

    render(<ProfilePage currentUser={currentUser} onUserUpdated={onUserUpdated} api={api} />);

    const actor = userEvent.setup();
    const currentPasswordInput = screen.getByLabelText("当前密码");
    const newPasswordInput = screen.getByLabelText("新密码");
    const confirmPasswordInput = screen.getByLabelText("确认新密码");

    fireEvent.change(currentPasswordInput, { target: { value: "wrong" } });
    fireEvent.change(newPasswordInput, { target: { value: "newpass" } });
    fireEvent.change(confirmPasswordInput, { target: { value: "newpass" } });
    await actor.click(screen.getByRole("button", { name: "修改密码" }));

    expect(api.updateCurrentUserPassword).toHaveBeenCalledWith({
      currentPassword: "wrong",
      newPassword: "newpass"
    });

    api.updateCurrentUserPassword = vi.fn(async () => {
      throw new ApiError(403, "forbidden");
    });

    fireEvent.change(currentPasswordInput, { target: { value: "wrong2" } });
    fireEvent.change(newPasswordInput, { target: { value: "another" } });
    fireEvent.change(confirmPasswordInput, { target: { value: "another" } });
    await actor.click(screen.getByRole("button", { name: "修改密码" }));

    expect(await screen.findByText("当前密码不正确")).toBeInTheDocument();
  });

  it("allows super administrators to change password without current password", async () => {
    const actor = userEvent.setup();
    const onUserUpdated = vi.fn();
    const superAdminUser = { ...currentUser, superAdmin: true };
    const api = {
      deleteCurrentUserAvatar: vi.fn(async () => currentUser),
      uploadCurrentUserAvatar: vi.fn(async () => currentUser),
      updateCurrentUser: vi.fn(async () => currentUser),
      updateCurrentUserPassword: vi.fn(async () => superAdminUser)
    };

    render(<ProfilePage currentUser={superAdminUser} onUserUpdated={onUserUpdated} api={api} />);

    expect(screen.queryByLabelText("当前密码")).not.toBeInTheDocument();
    await actor.type(screen.getByLabelText("新密码"), "newadminpass");
    await actor.type(screen.getByLabelText("确认新密码"), "newadminpass");
    await actor.click(screen.getByRole("button", { name: "修改密码" }));

    expect(api.updateCurrentUserPassword).toHaveBeenCalledWith({ newPassword: "newadminpass" });
  });

  it("uploads and removes avatar", async () => {
    const actor = userEvent.setup();
    const onUserUpdated = vi.fn();
    const api = {
      deleteCurrentUserAvatar: vi.fn(async () => ({ ...currentUser, avatarUrl: null })),
      uploadCurrentUserAvatar: vi.fn(async () => ({ ...currentUser, avatarUrl: "/api/users/me/avatar?v=2" })),
      updateCurrentUser: vi.fn(async () => currentUser),
      updateCurrentUserPassword: vi.fn(async () => currentUser)
    };
    const file = new File(["avatar"], "avatar.png", { type: "image/png" });

    render(<ProfilePage currentUser={currentUser} onUserUpdated={onUserUpdated} api={api} />);

    await actor.upload(screen.getByLabelText("上传头像"), file);
    await actor.click(screen.getByRole("button", { name: "移除头像" }));

    expect(api.uploadCurrentUserAvatar).toHaveBeenCalledWith(file);
    expect(api.deleteCurrentUserAvatar).toHaveBeenCalled();
    expect(onUserUpdated).toHaveBeenCalledTimes(2);
  });
});
