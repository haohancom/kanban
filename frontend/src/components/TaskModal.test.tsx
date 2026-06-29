import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import TaskModal from "./TaskModal";

describe("TaskModal", () => {
  it("focuses the title field and closes on Escape", async () => {
    const onClose = vi.fn();

    render(
      <TaskModal
        task={null}
        members={[]}
        sprints={[]}
        submitting={false}
        deleting={false}
        error={null}
        onClose={onClose}
        onSubmit={vi.fn()}
      />
    );

    expect(screen.getByLabelText("标题")).toHaveFocus();

    await userEvent.keyboard("{Escape}");

    expect(onClose).toHaveBeenCalled();
  });

  it("shows delete button for edit mode and triggers delete callback", async () => {
    const onDelete = vi.fn();

    render(
      <TaskModal
        task={{
          id: 10,
          teamId: 1,
          teamName: "平台组",
          title: "待删任务",
          status: "TODO"
        }}
        members={[]}
        sprints={[]}
        submitting={false}
        deleting={false}
        error={null}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
        onDelete={onDelete}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: "删除" }));

    expect(onDelete).toHaveBeenCalled();
  });

  it("disables delete button while a delete request is processing", () => {
    render(
      <TaskModal
        task={{
          id: 10,
          teamId: 1,
          teamName: "平台组",
          title: "待删任务",
          status: "TODO"
        }}
        members={[]}
        sprints={[]}
        submitting={false}
        deleting={true}
        error={null}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
        onDelete={vi.fn()}
      />
    );

    expect(screen.getByRole("button", { name: "删除" })).toBeDisabled();
  });

  it("hides delete button for create mode", () => {
    render(
      <TaskModal
        task={null}
        members={[]}
        sprints={[]}
        submitting={false}
        deleting={false}
        error={null}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />
    );

    expect(screen.queryByRole("button", { name: "删除" })).not.toBeInTheDocument();
  });

  it("keeps keyboard focus stable across parent rerenders", async () => {
    const firstOnClose = vi.fn();

    const { rerender } = render(
      <TaskModal
        task={null}
        members={[]}
        sprints={[]}
        submitting={false}
        deleting={false}
        error={null}
        onClose={firstOnClose}
        onSubmit={vi.fn()}
      />
    );

    await userEvent.click(screen.getByLabelText("描述"));

    rerender(
      <TaskModal
        task={null}
        members={[]}
        sprints={[]}
        submitting={false}
        deleting={false}
        error="任务保存失败"
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />
    );

    expect(screen.getByLabelText("描述")).toHaveFocus();
  });
});
