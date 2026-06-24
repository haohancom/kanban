import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import UserAdminPage, { type UserAdminApi } from "./UserAdminPage";

describe("UserAdminPage", () => {
  it("creates users, updates super administrator status, and resets passwords", async () => {
    const user = userEvent.setup();
    const api: UserAdminApi = {
      listUsers: vi.fn(async () => [
        { id: 1, username: "admin", displayName: "管理员", superAdmin: false }
      ]),
      createUser: vi.fn(async (values) => ({
        id: 2,
        username: values.username,
        displayName: values.displayName,
        superAdmin: values.superAdmin
      })),
      updateUser: vi.fn(async (userId, values) => ({
        id: userId,
        username: "admin",
        displayName: "管理员",
        superAdmin: Boolean(values.superAdmin)
      })),
      resetUserPassword: vi.fn(async () => undefined)
    };

    render(<UserAdminPage api={api} />);

    expect(await screen.findByText("admin")).toBeInTheDocument();

    await user.type(screen.getByLabelText("用户名"), "wang");
    await user.type(screen.getByLabelText("显示名称"), "小王");
    await user.type(screen.getByLabelText("初始密码"), "secret123");
    await user.click(screen.getByRole("button", { name: "创建用户" }));
    await user.click(screen.getByRole("button", { name: "授予超管 admin" }));
    await user.type(screen.getByLabelText("重置密码 admin"), "changed123");
    await user.click(screen.getByRole("button", { name: "重置密码 admin" }));

    expect(api.createUser).toHaveBeenCalledWith({
      username: "wang",
      displayName: "小王",
      password: "secret123",
      superAdmin: false
    });
    expect(api.updateUser).toHaveBeenCalledWith(1, { superAdmin: true });
    expect(api.resetUserPassword).toHaveBeenCalledWith(1, "changed123");
  });

  it("does not allow the current user to revoke their own super administrator access", async () => {
    const api: UserAdminApi = {
      listUsers: vi.fn(async () => [
        { id: 1, username: "admin", displayName: "管理员", superAdmin: true },
        { id: 2, username: "other-admin", displayName: "其他管理员", superAdmin: true }
      ]),
      createUser: vi.fn(async (values) => ({
        id: 3,
        username: values.username,
        displayName: values.displayName,
        superAdmin: values.superAdmin
      })),
      updateUser: vi.fn(async (userId, values) => ({
        id: userId,
        username: "other-admin",
        displayName: "其他管理员",
        superAdmin: Boolean(values.superAdmin)
      })),
      resetUserPassword: vi.fn(async () => undefined)
    };

    render(<UserAdminPage api={api} currentUserId={1} />);

    expect(await screen.findByRole("button", { name: "当前账号 admin" })).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: "撤销超管 other-admin" }));

    expect(api.updateUser).toHaveBeenCalledWith(2, { superAdmin: false });
    expect(api.updateUser).not.toHaveBeenCalledWith(1, expect.anything());
  });
});
