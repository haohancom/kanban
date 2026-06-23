export type TeamRole = "TEAM_CREATOR" | "TEAM_ADMIN" | "TEAM_MEMBER";

export interface MembershipSummary {
  teamId: number;
  teamName?: string;
  role: TeamRole;
}

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  superAdmin: boolean;
  memberships?: MembershipSummary[];
}

export interface Team {
  id: number;
  name: string;
  parentId?: number | null;
  role?: TeamRole | null;
  children: Team[];
}
