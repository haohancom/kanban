import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import SprintPage, { type SprintPageApi } from "./SprintPage";

describe("SprintPage", () => {
  it("creates and deactivates sprints", async () => {
    const user = userEvent.setup();
    const api: SprintPageApi = {
      listSprints: vi.fn(async () => [{ id: 7, teamId: 1, name: "Sprint 1", active: true }]),
      createSprint: vi.fn(async (teamId, values) => ({
        id: 8,
        teamId,
        name: values.name,
        active: true
      })),
      updateSprint: vi.fn(async (sprintId, values) => ({
        id: sprintId,
        teamId: 1,
        name: "Sprint 1",
        active: Boolean(values.active)
      })),
      deleteSprint: vi.fn(async () => {})
    };

    render(<SprintPage teamId={1} api={api} />);

    expect(await screen.findByText("Sprint 1")).toBeInTheDocument();

    await user.type(screen.getByLabelText("Sprint 名称"), "Sprint 2");
    await user.click(screen.getByRole("button", { name: "创建 Sprint" }));
    await user.click(screen.getByRole("button", { name: "停用 Sprint 1" }));
    await user.click(screen.getByRole("button", { name: "删除 Sprint 1" }));
    expect(screen.getByRole("dialog", { name: "确认删除 Sprint" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "确认删除" }));

    expect(api.createSprint).toHaveBeenCalledWith(1, { name: "Sprint 2" });
    expect(api.updateSprint).toHaveBeenCalledWith(7, { active: false }, 1);
    expect(api.deleteSprint).toHaveBeenCalledWith(7, 1);
  });

  it("cannot modify sprints that belong to ancestor teams", async () => {
    const api: SprintPageApi = {
      listSprints: vi.fn(async () => [
        { id: 7, teamId: 1, name: "父团队 Sprint", active: true },
        { id: 8, teamId: 2, name: "子团队 Sprint", active: true }
      ]),
      createSprint: vi.fn(async (teamId, values) => ({
        id: 9,
        teamId,
        name: values.name,
        active: true
      })),
      updateSprint: vi.fn(async (sprintId, values) => ({
        id: sprintId,
        teamId: 1,
        name: "父团队 Sprint",
        active: Boolean(values.active)
      })),
      deleteSprint: vi.fn(async () => {})
    };

    render(<SprintPage teamId={2} api={api} />);

    expect(await screen.findByText("父团队 Sprint")).toBeInTheDocument();
    const parentRow = screen.getByText("父团队 Sprint").closest("tr") as HTMLTableRowElement;
    expect(within(parentRow).getByText("仅本团队可操作")).toBeInTheDocument();
    expect(within(parentRow).queryByRole("button", { name: "删除 父团队 Sprint" })).not.toBeInTheDocument();
    expect(within(parentRow).queryByRole("button", { name: "保存名称" })).not.toBeInTheDocument();
    expect(within(parentRow).queryByRole("button", { name: "停用 父团队 Sprint" })).not.toBeInTheDocument();
    expect(within(parentRow).queryByRole("button", { name: "启用 父团队 Sprint" })).not.toBeInTheDocument();
  });
});
