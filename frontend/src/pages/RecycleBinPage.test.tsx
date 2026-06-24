import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import RecycleBinPage, { type RecycleBinApi } from "./RecycleBinPage";

describe("RecycleBinPage", () => {
  it("selects deleted tasks and bulk deletes them", async () => {
    const user = userEvent.setup();
    const api: RecycleBinApi = {
      listDeletedTasks: vi.fn(async () => [
        { id: 9, title: "旧任务", teamName: "平台组", deletedAt: "2026-06-23T00:00:00Z" }
      ]),
      bulkDeleteTasks: vi.fn(async () => undefined),
      restoreTask: vi.fn(async () => undefined),
      deleteTaskForever: vi.fn(async () => undefined)
    };

    render(<RecycleBinPage teamId={1} api={api} />);

    await user.click(await screen.findByRole("checkbox", { name: "选择 旧任务" }));
    await user.click(screen.getByRole("button", { name: "永久删除所选" }));
    expect(api.bulkDeleteTasks).not.toHaveBeenCalled();
    expect(screen.getByRole("dialog", { name: "确认永久删除" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "确认删除" }));

    expect(api.bulkDeleteTasks).toHaveBeenCalledWith([9]);
  });
});
