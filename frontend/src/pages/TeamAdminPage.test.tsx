import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import TeamAdminPage, { type TeamAdminApi } from "./TeamAdminPage";
import { Team } from "../types";

const selectedTeam: Team = {
  id: 1,
  name: "研发部",
  children: []
};

describe("TeamAdminPage", () => {
  it("creates a sub-team and adds a member", async () => {
    const user = userEvent.setup();
    const api: TeamAdminApi = {
      listMembers: vi.fn(async () => [
        { id: 10, teamId: 1, userId: 1, username: "admin", displayName: "管理员", role: "TEAM_CREATOR" as const }
      ]),
      createTeam: vi.fn(async (values) => ({ id: 2, name: values.name, parentId: values.parentId, children: [] })),
      updateTeam: vi.fn(async (teamId, values) => ({ id: teamId, name: values.name, children: [] })),
      listAssignableUsers: vi.fn(async () => [
        { id: 1, username: "admin", displayName: "管理员" },
        { id: 2, username: "wang", displayName: "小王" }
      ]),
      addMember: vi.fn(async () => ({
        id: 11,
        teamId: 1,
        userId: 2,
        username: "wang",
        displayName: "小王",
        role: "TEAM_MEMBER" as const
      })),
      updateMember: vi.fn(async (teamId, membershipId, values) => ({
        id: membershipId,
        teamId,
        userId: 2,
        username: "wang",
        displayName: "小王",
        role: values.role
      })),
      removeMember: vi.fn(async () => undefined)
    };

    render(<TeamAdminPage selectedTeam={selectedTeam} onTeamsChanged={vi.fn()} api={api} />);

    expect(await screen.findByText("管理员")).toBeInTheDocument();
    expect(screen.queryByRole("option", { name: "管理员 (admin)" })).not.toBeInTheDocument();

    await user.type(screen.getByLabelText("子团队名称"), "平台组");
    await user.click(screen.getByRole("button", { name: "创建子团队" }));
    await user.selectOptions(screen.getByLabelText("成员"), "2");
    await user.click(screen.getByRole("button", { name: "添加成员" }));

    expect(api.createTeam).toHaveBeenCalledWith({ name: "平台组", parentId: 1 });
    expect(api.listAssignableUsers).toHaveBeenCalledWith(1);
    expect(api.addMember).toHaveBeenCalledWith(1, { userId: 2, role: "TEAM_MEMBER" });
  });
});
