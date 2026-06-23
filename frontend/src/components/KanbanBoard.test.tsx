import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import KanbanBoard from "./KanbanBoard";

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

  it("shows task metadata indicators and emits status moves", async () => {
    const onMove = vi.fn();

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
        onMove={onMove}
      />
    );

    const todoColumn = screen.getByLabelText("待开始");
    expect(within(todoColumn).getByText("小王")).toBeInTheDocument();
    expect(within(todoColumn).getByText("Sprint A")).toBeInTheDocument();
    expect(within(todoColumn).getByText("有备注")).toBeInTheDocument();
    expect(within(todoColumn).getByText("有风险")).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("移动 接入登录 状态"), "IN_PROGRESS");

    expect(onMove).toHaveBeenCalledWith(expect.objectContaining({ id: 1 }), "IN_PROGRESS");
  });
});
