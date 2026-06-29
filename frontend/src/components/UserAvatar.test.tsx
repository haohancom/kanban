import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import UserAvatar from "./UserAvatar";

describe("UserAvatar", () => {
  it("uses the last non-whitespace display-name character for the default avatar", () => {
    render(<UserAvatar displayName="管理员 " username="admin" />);

    expect(screen.getByLabelText("管理员 的默认头像")).toHaveTextContent("员");
  });

  it("falls back to username when display name is blank", () => {
    render(<UserAvatar displayName=" " username="admin" />);

    expect(screen.getByLabelText("admin 的默认头像")).toHaveTextContent("n");
  });

  it("renders an uploaded avatar image when avatarUrl exists", () => {
    render(<UserAvatar avatarUrl="/api/users/me/avatar?v=1" displayName="管理员" username="admin" />);

    expect(screen.getByRole("img", { name: "管理员 的头像" })).toHaveAttribute(
      "src",
      "/api/users/me/avatar?v=1"
    );
  });
});
