import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import AppShell from "./AppShell";
import { CurrentUser, Team } from "../types";

const user: CurrentUser = {
  id: 1,
  username: "admin",
  displayName: "管理员",
  superAdmin: false
};

describe("AppShell", () => {
  it("shows team management navigation for child teams managed through an ancestor", () => {
    const teams: Team[] = [
      {
        id: 1,
        name: "研发部",
        role: "TEAM_ADMIN",
        children: [{ id: 2, name: "平台组", role: null, children: [] }]
      }
    ];

    render(
      <AppShell
        onLogout={vi.fn()}
        onSelectTeam={vi.fn()}
        onSelectView={vi.fn()}
        activeView="board"
        selectedTeam={teams[0].children[0]}
        selectedTeamId={2}
        teamError={null}
        teams={teams}
        teamsLoading={false}
        user={user}
      >
        <div>看板内容</div>
      </AppShell>
    );

    expect(screen.getByRole("button", { name: "团队管理" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "冲刺管理" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "回收站" })).toBeInTheDocument();
  });

  it("changes workspace view from navigation and shows snapshot settings only to super administrators", async () => {
    const selectView = vi.fn();
    const teams: Team[] = [{ id: 1, name: "研发部", role: "TEAM_CREATOR", children: [] }];

    render(
      <AppShell
        onLogout={vi.fn()}
        onSelectTeam={vi.fn()}
        onSelectView={selectView}
        activeView="board"
        selectedTeam={teams[0]}
        selectedTeamId={1}
        teamError={null}
        teams={teams}
        teamsLoading={false}
        user={{ ...user, superAdmin: true }}
      >
        <div>看板内容</div>
      </AppShell>
    );

    await userEvent.click(screen.getByRole("button", { name: "团队管理" }));
    await userEvent.click(screen.getByRole("button", { name: "快照设置" }));

    expect(selectView).toHaveBeenCalledWith("team-admin");
    expect(selectView).toHaveBeenCalledWith("snapshots");
  });

  it("lets super administrators open team administration before any team exists", async () => {
    const selectView = vi.fn();

    render(
      <AppShell
        onLogout={vi.fn()}
        onSelectTeam={vi.fn()}
        onSelectView={selectView}
        activeView="board"
        selectedTeam={null}
        selectedTeamId={null}
        teamError={null}
        teams={[]}
        teamsLoading={false}
        user={{ ...user, superAdmin: true }}
      >
        <div>看板内容</div>
      </AppShell>
    );

    await userEvent.click(screen.getByRole("button", { name: "团队管理" }));

    expect(screen.queryByRole("button", { name: "冲刺管理" })).not.toBeInTheDocument();
    expect(selectView).toHaveBeenCalledWith("team-admin");
  });
});
