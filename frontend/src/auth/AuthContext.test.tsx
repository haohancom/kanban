import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "../App";
import { AuthProvider, useAuth } from "./AuthContext";

describe("auth flow", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("logs in and shows the authenticated shell", async () => {
    let meCalls = 0;
    const currentUser = {
      id: 1,
      username: "admin",
      displayName: "超级管理员",
      superAdmin: true,
      memberships: []
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/api/auth/me")) {
        meCalls += 1;
        return meCalls === 1 ? new Response("null", { status: 401 }) : Response.json(currentUser);
      }
      if (url.endsWith("/api/auth/login")) {
        return Response.json(currentUser);
      }
      if (url.endsWith("/api/teams")) {
        return Response.json([]);
      }
      return new Response("{}", { status: 404 });
    });

    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await userEvent.type(await screen.findByLabelText("用户名"), "admin");
    await userEvent.type(screen.getByLabelText("密码"), "admin123");
    await userEvent.click(screen.getByRole("button", { name: "登录" }));

    await waitFor(() => expect(screen.getByText("超级管理员")).toBeInTheDocument());
    await userEvent.click(screen.getByRole("button", { name: "团队管理" }));

    expect(screen.getByLabelText("根团队名称")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/auth/me",
      expect.objectContaining({ credentials: "include" })
    );
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/auth/login",
      expect.objectContaining({
        body: JSON.stringify({ username: "admin", password: "admin123" }),
        credentials: "include",
        method: "POST"
      })
    );
  });

  it("confirms the session before loading the workspace after login", async () => {
    const calls: string[] = [];
    let meCalls = 0;
    const currentUser = {
      id: 1,
      username: "admin",
      displayName: "超级管理员",
      superAdmin: true,
      memberships: []
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      calls.push(url);

      if (url.endsWith("/api/auth/me")) {
        meCalls += 1;
        return meCalls === 1 ? new Response("null", { status: 401 }) : Response.json(currentUser);
      }
      if (url.endsWith("/api/auth/login")) {
        return Response.json(currentUser);
      }
      if (url.endsWith("/api/teams")) {
        return Response.json([]);
      }
      return new Response("{}", { status: 404 });
    });

    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await userEvent.type(await screen.findByLabelText("用户名"), "admin");
    await userEvent.type(screen.getByLabelText("密码"), "admin123");
    await userEvent.click(screen.getByRole("button", { name: "登录" }));

    await waitFor(() => expect(screen.getByText("超级管理员")).toBeInTheDocument());

    const loginIndex = calls.findIndex((url) => url.endsWith("/api/auth/login"));
    const postLoginMeIndex = calls.findIndex(
      (url, index) => index > loginIndex && url.endsWith("/api/auth/me")
    );
    const teamsIndex = calls.findIndex((url) => url.endsWith("/api/teams"));

    expect(postLoginMeIndex).toBeGreaterThan(loginIndex);
    expect(postLoginMeIndex).toBeLessThan(teamsIndex);
  });

  it("clears the local user when logout returns unauthorized", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/api/auth/me")) {
        return Response.json({
          id: 1,
          username: "admin",
          displayName: "超级管理员",
          superAdmin: true,
          memberships: []
        });
      }
      if (url.endsWith("/api/auth/logout")) {
        return new Response("null", { status: 401 });
      }
      return new Response("{}", { status: 404 });
    });

    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await screen.findByText("超级管理员");
    await userEvent.click(screen.getByRole("button", { name: "退出" }));

    await waitFor(() => expect(screen.getByLabelText("用户名")).toBeInTheDocument());
    expect(screen.queryByText("超级管理员")).not.toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/auth/logout",
      expect.objectContaining({ credentials: "include", method: "POST" })
    );
  });

  it("refreshes the current user on demand", async () => {
    let meCalls = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/api/auth/me")) {
        meCalls += 1;
        return Response.json({
          id: 1,
          username: "admin",
          displayName: meCalls === 1 ? "旧名称" : "新名称",
          superAdmin: true,
          memberships: []
        });
      }
      return new Response("{}", { status: 404 });
    });

    vi.stubGlobal("fetch", fetchMock);

    render(
      <AuthProvider>
        <RefreshHarness />
      </AuthProvider>
    );

    expect(await screen.findByText("旧名称")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "刷新用户" }));

    await waitFor(() => expect(screen.getByText("新名称")).toBeInTheDocument());
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});

function RefreshHarness() {
  const { refreshUser, user } = useAuth();

  return (
    <div>
      <span>{user?.displayName ?? "未登录"}</span>
      <button type="button" onClick={() => void refreshUser()}>
        刷新用户
      </button>
    </div>
  );
}
