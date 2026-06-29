import { render, screen } from "@testing-library/react";
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
      }))
    };

    render(<SprintPage teamId={1} api={api} />);

    expect(await screen.findByText("Sprint 1")).toBeInTheDocument();

    await user.type(screen.getByLabelText("Sprint 名称"), "Sprint 2");
    await user.click(screen.getByRole("button", { name: "创建 Sprint" }));
    await user.click(screen.getByRole("button", { name: "停用 Sprint 1" }));

    expect(api.createSprint).toHaveBeenCalledWith(1, { name: "Sprint 2" });
    expect(api.updateSprint).toHaveBeenCalledWith(7, { active: false });
  });
});
