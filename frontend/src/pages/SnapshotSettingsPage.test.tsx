import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import SnapshotSettingsPage, { type SnapshotSettingsApi } from "./SnapshotSettingsPage";

describe("SnapshotSettingsPage", () => {
  it("updates snapshot settings and triggers a manual snapshot", async () => {
    const user = userEvent.setup();
    const api: SnapshotSettingsApi = {
      getSettings: vi.fn(async () => ({
        enabled: false,
        cron: "0 0 0 * * *",
        retentionDays: 3,
        outputPath: "backup"
      })),
      updateSettings: vi.fn(async (settings) => settings),
      runSnapshot: vi.fn(async () => ({ fileName: "kanban-snapshot-20260623-000000.sqlite3" }))
    };

    render(<SnapshotSettingsPage api={api} />);

    await user.click(await screen.findByRole("checkbox", { name: "启用自动快照" }));
    await user.clear(screen.getByLabelText("保留天数"));
    await user.type(screen.getByLabelText("保留天数"), "5");
    await user.clear(screen.getByLabelText("输出路径"));
    await user.type(screen.getByLabelText("输出路径"), "/data/backup");
    await user.click(screen.getByRole("button", { name: "保存设置" }));
    await user.click(screen.getByRole("button", { name: "立即生成快照" }));

    expect(api.updateSettings).toHaveBeenCalledWith(
      expect.objectContaining({
        enabled: true,
        retentionDays: 5,
        outputPath: "/data/backup"
      })
    );
    expect(api.runSnapshot).toHaveBeenCalled();
    expect(await screen.findByText("kanban-snapshot-20260623-000000.sqlite3")).toBeInTheDocument();
  });
});
