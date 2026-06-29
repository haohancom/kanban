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
    expect(screen.getByRole("button", { name: "Sprint 管理" })).toBeInTheDocument();
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

    expect(screen.queryByRole("button", { name: "Sprint 管理" })).not.toBeInTheDocument();
    expect(selectView).toHaveBeenCalledWith("team-admin");
  });

  it("shows the default avatar in the workspace header", () => {
    const teams: Team[] = [{ id: 1, name: "研发部", role: "TEAM_CREATOR", children: [] }];

    render(
      <AppShell
        onLogout={vi.fn()}
        onSelectTeam={vi.fn()}
        onSelectView={vi.fn()}
        activeView="board"
        selectedTeam={teams[0]}
        selectedTeamId={1}
        teamError={null}
        teams={teams}
        teamsLoading={false}
        user={{ ...user, displayName: "管理员", avatarUrl: null }}
      >
        <div>看板内容</div>
      </AppShell>
    );

    expect(screen.getByLabelText("管理员 的默认头像")).toHaveTextContent("员");
  });

  it("uploads and removes the current user's avatar", async () => {
    const actor = userEvent.setup();
    const teams: Team[] = [{ id: 1, name: "研发部", role: "TEAM_CREATOR", children: [] }];
    const uploadAvatar = vi.fn(async () => undefined);
    const deleteAvatar = vi.fn(async () => undefined);
    const file = new File(["avatar"], "avatar.png", { type: "image/png" });

    render(
      <AppShell
        onLogout={vi.fn()}
        onSelectTeam={vi.fn()}
        onSelectView={vi.fn()}
        activeView="board"
        selectedTeam={teams[0]}
        selectedTeamId={1}
        teamError={null}
        teams={teams}
        teamsLoading={false}
        user={{ ...user, displayName: "管理员", avatarUrl: "/api/users/me/avatar?v=1" }}
        onUploadAvatar={uploadAvatar}
        onDeleteAvatar={deleteAvatar}
      >
        <div>看板内容</div>
      </AppShell>
    );

    await actor.upload(screen.getByLabelText("上传头像"), file);
    await actor.click(screen.getByRole("button", { name: "移除头像" }));

    expect(uploadAvatar).toHaveBeenCalledWith(file);
    expect(deleteAvatar).toHaveBeenCalled();
  });

  it("shows an inline avatar upload error", async () => {
    const actor = userEvent.setup();
    const teams: Team[] = [{ id: 1, name: "研发部", role: "TEAM_CREATOR", children: [] }];
    const uploadAvatar = vi.fn(async () => {
      throw new Error("bad avatar");
    });
    const file = new File(["avatar"], "avatar.png", { type: "image/png" });

    render(
      <AppShell
        onLogout={vi.fn()}
        onSelectTeam={vi.fn()}
        onSelectView={vi.fn()}
        activeView="board"
        selectedTeam={teams[0]}
        selectedTeamId={1}
        teamError={null}
        teams={teams}
        teamsLoading={false}
        user={{ ...user, displayName: "管理员", avatarUrl: null }}
        onUploadAvatar={uploadAvatar}
      >
        <div>看板内容</div>
      </AppShell>
    );

    await actor.upload(screen.getByLabelText("上传头像"), file);

    expect(await screen.findByText("头像上传失败")).toBeInTheDocument();
  });
});
