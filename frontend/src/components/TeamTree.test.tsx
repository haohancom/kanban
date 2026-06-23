import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import TeamTree from "./TeamTree";

describe("TeamTree", () => {
  it("renders nested teams and selects a child", async () => {
    const onSelect = vi.fn();

    render(
      <TeamTree
        teams={[
          {
            id: 1,
            name: "研发部",
            children: [{ id: 2, name: "平台组", children: [] }]
          }
        ]}
        selectedTeamId={1}
        onSelect={onSelect}
      />
    );

    expect(screen.getByText("研发部")).toBeInTheDocument();
    await userEvent.click(screen.getByText("平台组"));

    expect(onSelect).toHaveBeenCalledWith(2);
  });
});
