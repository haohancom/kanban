import { useState } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import BoardFilters from "./BoardFilters";
import { BoardTaskFilters } from "../types";

describe("BoardFilters", () => {
  it("emits selected sub-team, member, status, and sprint filters", async () => {
    const onChange = vi.fn();

    function StatefulFilters() {
      const [value, setValue] = useState<BoardTaskFilters>({});

      return (
        <BoardFilters
          teams={[{ id: 2, name: "平台组" }]}
          members={[{ id: 3, displayName: "小王" }]}
          sprints={[{ id: 4, name: "Sprint A" }]}
          value={value}
          onChange={(nextValue) => {
            setValue(nextValue);
            onChange(nextValue);
          }}
        />
      );
    }

    render(<StatefulFilters />);

    await userEvent.selectOptions(screen.getByLabelText("子团队"), "2");
    await userEvent.selectOptions(screen.getByLabelText("成员"), "3");
    await userEvent.selectOptions(screen.getByLabelText("状态"), "TODO");
    await userEvent.selectOptions(screen.getByLabelText("冲刺"), "4");

    expect(onChange).toHaveBeenLastCalledWith({
      subTeamId: 2,
      memberId: 3,
      status: "TODO",
      sprintId: 4
    });
  });
});
