import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import KanbanBoard from "./KanbanBoard";
import userEvent from "@testing-library/user-event";

describe("KanbanBoard", () => {
  it("groups task cards by status columns", () => {
    render(
      <KanbanBoard
        tasks={[
          { id: 1, title: "接入登录", status: "TODO", teamName: "平台组" },
          { id: 2, title: "实现回收站", status: "DONE", teamName: "平台组" }
        ]}
        onEdit={vi.fn()}
      />
    );

    expect(within(screen.getByLabelText("待开始")).getByText("接入登录")).toBeInTheDocument();
    expect(within(screen.getByLabelText("已完成")).getByText("实现回收站")).toBeInTheDocument();
  });

  it("shows task metadata indicators", () => {
    render(
      <KanbanBoard
        tasks={[
          {
            id: 1,
            title: "接入登录",
            status: "TODO",
            teamName: "平台组",
            assigneeDisplayName: "小王",
            sprintName: "Sprint A",
            remarks: "先做 session",
            risks: "权限遗漏"
          }
        ]}
        onEdit={vi.fn()}
      />
    );

    const todoColumn = screen.getByLabelText("待开始");
    expect(within(todoColumn).getByText("小王")).toBeInTheDocument();
    expect(within(todoColumn).getByText("Sprint A")).toBeInTheDocument();
    expect(within(todoColumn).getByText("先做 session")).toBeInTheDocument();
    const card = screen.getByText("接入登录").closest("article");
    expect(card).toHaveClass("has-risk");
    expect(within(todoColumn).queryByText("有备注")).not.toBeInTheDocument();
    expect(within(todoColumn).queryByText("有风险")).not.toBeInTheDocument();
  });

  it("does not render a delete button on cards", () => {
    render(
      <KanbanBoard
        tasks={[
          { id: 1, title: "接入登录", status: "TODO", teamName: "平台组", assigneeDisplayName: "小王" }
        ]}
        onEdit={vi.fn()}
      />
    );

    expect(screen.queryByRole("button", { name: "删除 接入登录" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /删除/ })).not.toBeInTheDocument();
  });

  it("opens edit modal on double click", async () => {
    const onEdit = vi.fn();
    render(
      <KanbanBoard
        tasks={[
          {
            id: 1,
            title: "接入登录",
            status: "TODO",
            teamName: "平台组",
            assigneeDisplayName: "小王"
          }
        ]}
        onEdit={onEdit}
      />
    );

    await userEvent.dblClick(screen.getByText("接入登录"));

    expect(onEdit).toHaveBeenCalledWith(expect.objectContaining({ id: 1 }));
  });

  it("emits status moves when a permitted task is dropped into another column", () => {
    const onMove = vi.fn();

    render(
      <KanbanBoard
        tasks={[{ id: 1, title: "接入登录", status: "TODO", teamName: "平台组" }]}
        onEdit={vi.fn()}
        onMove={onMove}
        canMoveTask={() => true}
      />
    );

    expect(screen.queryByLabelText("移动 接入登录 状态")).not.toBeInTheDocument();

    const card = screen.getByText("接入登录").closest("article");
    expect(card).toHaveAttribute("draggable", "true");

    const dataTransfer = createDataTransfer();
    fireEvent.dragStart(card as HTMLElement, { dataTransfer });
    fireEvent.dragOver(screen.getByLabelText("进行中"), { dataTransfer });
    fireEvent.drop(screen.getByLabelText("进行中"), { dataTransfer });

    expect(onMove).toHaveBeenCalledWith(expect.objectContaining({ id: 1 }), "IN_PROGRESS");
  });

  it("does not emit status moves for tasks the user cannot move", () => {
    const onMove = vi.fn();

    render(
      <KanbanBoard
        tasks={[{ id: 1, title: "他人任务", status: "TODO", teamName: "平台组" }]}
        onEdit={vi.fn()}
        onMove={onMove}
        canMoveTask={() => false}
      />
    );

    const card = screen.getByText("他人任务").closest("article");
    expect(card).not.toHaveAttribute("draggable", "true");

    const dataTransfer = createDataTransfer();
    fireEvent.dragStart(card as HTMLElement, { dataTransfer });
    fireEvent.dragOver(screen.getByLabelText("进行中"), { dataTransfer });
    fireEvent.drop(screen.getByLabelText("进行中"), { dataTransfer });

    expect(onMove).not.toHaveBeenCalled();
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
