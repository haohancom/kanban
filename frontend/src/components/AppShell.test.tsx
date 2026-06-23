import { render, screen } from "@testing-library/react";
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
});
