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
        error={null}
        onClose={onClose}
        onSubmit={vi.fn()}
      />
    );

    expect(screen.getByLabelText("标题")).toHaveFocus();

    await userEvent.keyboard("{Escape}");

    expect(onClose).toHaveBeenCalled();
  });

  it("keeps keyboard focus stable across parent rerenders", async () => {
    const firstOnClose = vi.fn();

    const { rerender } = render(
      <TaskModal
        task={null}
        members={[]}
        sprints={[]}
        submitting={false}
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
        error="任务保存失败"
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />
    );

    expect(screen.getByLabelText("描述")).toHaveFocus();
  });
});
