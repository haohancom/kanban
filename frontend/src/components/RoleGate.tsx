import { ReactNode } from "react";
import { CurrentUser, Team } from "../types";

type Requirement = "super-admin" | "team-manager" | "team-member";

interface RoleGateProps {
  canManageTeam?: boolean;
  children: ReactNode;
  requirement: Requirement;
  team?: Team | null;
  user: CurrentUser;
}

export default function RoleGate({
  canManageTeam = false,
  children,
  requirement,
  team,
  user
}: RoleGateProps) {
  if (!isAllowed(user, requirement, team, canManageTeam)) {
    return null;
  }

  return <>{children}</>;
}

function isAllowed(
  user: CurrentUser,
  requirement: Requirement,
  team?: Team | null,
  canManageTeam = false
) {
  if (requirement === "super-admin") {
    return user.superAdmin;
  }

  if (user.superAdmin) {
    return Boolean(team);
  }

  if (requirement === "team-manager") {
    return Boolean(
      team && (canManageTeam || team.role === "TEAM_CREATOR" || team.role === "TEAM_ADMIN")
    );
  }

  if (!team?.role) {
    return false;
  }

  if (requirement === "team-member") {
    return true;
  }

  return false;
}
