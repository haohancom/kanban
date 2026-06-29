import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import BoardPage from "./BoardPage";
import { Team } from "../types";

const selectedTeam: Team = {
  id: 1,
  name: "研发部",
  children: [{ id: 2, name: "平台组", children: [] }]
};

const otherTeam: Team = {
  id: 3,
  name: "设计部",
  children: []
};

describe("BoardPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("uses the task team metadata when editing a descendant team task", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);

      if (url === "/api/teams/1/board/tasks") {
        return Response.json([
          {
            id: 10,
            teamId: 2,
            teamName: "平台组",
            title: "子团队任务",
            status: "TODO",
            sprintId: 22,
            sprintName: "Child Sprint",
            assigneeId: 33,
            assigneeDisplayName: "小王"
          }
        ]);
      }

      if (url === "/api/teams/1/members") {
        return Response.json([{ id: 11, teamId: 1, userId: 11, displayName: "父团队成员", role: "TEAM_MEMBER" }]);
      }
      if (url === "/api/teams/2/members") {
        return Response.json([{ id: 33, teamId: 2, userId: 33, displayName: "小王", role: "TEAM_MEMBER" }]);
      }
      if (url === "/api/teams/1/sprints") {
        return Response.json([{ id: 21, teamId: 1, name: "Parent Sprint", active: true }]);
      }
      if (url === "/api/teams/2/sprints") {
        return Response.json([{ id: 22, teamId: 2, name: "Child Sprint", active: true }]);
      }

      return new Response("{}", { status: 404 });
    });

    vi.stubGlobal("fetch", fetchMock);

    render(
      <BoardPage
        canManageSelectedTeam={true}
        currentUserId={99}
        selectedTeam={selectedTeam}
        teamsLoading={false}
      />
    );

    await screen.findByText("子团队任务");
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/teams/2/members",
        expect.objectContaining({ credentials: "include" })
      )
    );

    const card = screen.getByText("子团队任务").closest("article");
    expect(card).not.toBeNull();
    await userEvent.dblClick(card as HTMLElement);

    const dialog = screen.getByRole("dialog", { name: "子团队任务" });
    expect(within(dialog).getByRole("option", { name: "Child Sprint" })).toBeInTheDocument();
    expect(within(dialog).getByRole("option", { name: "小王" })).toBeInTheDocument();
    expect(within(dialog).queryByRole("option", { name: "Parent Sprint" })).not.toBeInTheDocument();
  });

  it("hides previous team tasks immediately when switching teams", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);

      if (url === "/api/teams/1/board/tasks") {
        return Response.json([{ id: 10, teamId: 1, teamName: "研发部", title: "旧团队任务", status: "TODO" }]);
      }
      if (url === "/api/teams/3/board/tasks") {
        return new Promise<Response>(() => undefined);
      }
      if (url.endsWith("/members") || url.endsWith("/sprints")) {
        return Response.json([]);
      }

      return new Response("{}", { status: 404 });
    });

    vi.stubGlobal("fetch", fetchMock);

    const { rerender } = render(
      <BoardPage
        canManageSelectedTeam={true}
        currentUserId={99}
        selectedTeam={selectedTeam}
        teamsLoading={false}
      />
    );

    await screen.findByText("旧团队任务");

    rerender(
      <BoardPage
        canManageSelectedTeam={true}
        currentUserId={99}
        selectedTeam={otherTeam}
        teamsLoading={false}
      />
    );

    expect(screen.queryByText("旧团队任务")).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("正在加载任务");
  });

  it("does not show delete controls directly on task cards", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);

      if (url === "/api/teams/1/board/tasks") {
        return Response.json([{ id: 10, teamId: 1, teamName: "研发部", title: "卡片任务", status: "TODO" }]);
      }
      if (url === "/api/tasks/10" && init?.method === "DELETE") return new Response(null, { status: 204 });
      if (url.endsWith("/members") || url.endsWith("/sprints")) {
        return Response.json([]);
      }

      return new Response("{}", { status: 404 });
    });

    vi.stubGlobal("fetch", fetchMock);

    render(
      <BoardPage
        canManageSelectedTeam={true}
        currentUserId={99}
        selectedTeam={selectedTeam}
        teamsLoading={false}
      />
    );

    await screen.findByText("卡片任务");

    expect(screen.queryByRole("button", { name: /删除/ })).not.toBeInTheDocument();
  });

  it("hides task deletion controls in modal for users who cannot manage the selected team", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);

      if (url === "/api/teams/1/board/tasks") {
        return Response.json([{ id: 10, teamId: 1, teamName: "研发部", title: "成员可见任务", status: "TODO" }]);
      }
      if (url.endsWith("/members") || url.endsWith("/sprints")) {
        return Response.json([]);
      }

      return new Response("{}", { status: 404 });
    });

    vi.stubGlobal("fetch", fetchMock);

    render(
      <BoardPage
        canManageSelectedTeam={false}
        currentUserId={99}
        selectedTeam={selectedTeam}
        teamsLoading={false}
      />
    );

    await screen.findByText("成员可见任务");
    const card = screen.getByText("成员可见任务").closest("article");
    expect(card).not.toBeNull();
    await userEvent.dblClick(card as HTMLElement);

    const dialog = screen.getByRole("dialog", { name: "成员可见任务" });
    expect(within(dialog).queryByRole("button", { name: "删除" })).not.toBeInTheDocument();
  });

  it("keeps task deletion in the modal and deletes task from there", async () => {
    let tasks = [{ id: 10, teamId: 1, teamName: "研发部", title: "可删除任务", status: "TODO" }];
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);

      if (url === "/api/teams/1/board/tasks") {
        return Response.json(tasks);
      }
      if (url.endsWith("/members") || url.endsWith("/sprints")) {
        return Response.json([]);
      }
      if (url === "/api/tasks/10" && init?.method === "DELETE") {
        tasks = [];
        return new Response(null, { status: 204 });
      }
      return new Response("{}", { status: 404 });
    });

    vi.stubGlobal("fetch", fetchMock);

    render(
      <BoardPage
        canManageSelectedTeam={true}
        currentUserId={99}
        selectedTeam={selectedTeam}
        teamsLoading={false}
      />
    );

    const card = (await screen.findByText("可删除任务")).closest("article");
    expect(card).not.toBeNull();
    await userEvent.dblClick(card as HTMLElement);
    await userEvent.click(screen.getByRole("button", { name: "删除" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/tasks/10",
        expect.objectContaining({ credentials: "include", method: "DELETE" })
      )
    );
    await waitFor(() => expect(screen.queryByText("可删除任务")).not.toBeInTheDocument());
  });

  it("updates a member's own task status by dragging it into another column", async () => {
    let tasks = [
      {
        id: 10,
        teamId: 1,
        teamName: "研发部",
        title: "我的任务",
        status: "TODO",
        assigneeId: 99
      }
    ];
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);

      if (url === "/api/teams/1/board/tasks") {
        return Response.json(tasks);
      }
      if (url === "/api/tasks/10" && init?.method === "PATCH") {
        tasks = [{ ...tasks[0], status: "IN_PROGRESS" }];
        return Response.json(tasks[0]);
      }
      if (url.endsWith("/members") || url.endsWith("/sprints")) {
        return Response.json([]);
      }

      return new Response("{}", { status: 404 });
    });

    vi.stubGlobal("fetch", fetchMock);

    render(
      <BoardPage
        canManageSelectedTeam={false}
        currentUserId={99}
        selectedTeam={selectedTeam}
        teamsLoading={false}
      />
    );

    const card = (await screen.findByText("我的任务")).closest("article") as HTMLElement;
    const dataTransfer = createDataTransfer();
    fireEvent.dragStart(card, { dataTransfer });
    fireEvent.dragOver(screen.getByLabelText("进行中"), { dataTransfer });
    fireEvent.drop(screen.getByLabelText("进行中"), { dataTransfer });

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/tasks/10",
        expect.objectContaining({
          body: JSON.stringify({ status: "IN_PROGRESS" }),
          credentials: "include",
          method: "PATCH"
        })
      )
    );
  });
});

function createDataTransfer() {
  const data = new Map<string, string>();
  return {
    clearData: vi.fn(() => data.clear()),
    getData: vi.fn((format: string) => data.get(format) ?? ""),
    setData: vi.fn((format: string, value: string) => data.set(format, value))
  };
}
