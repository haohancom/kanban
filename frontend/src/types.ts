export type TeamRole = "TEAM_CREATOR" | "TEAM_ADMIN" | "TEAM_MEMBER";
export type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE";

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

export interface BoardTask {
  id: number;
  teamId?: number;
  teamName: string;
  title: string;
  description?: string;
  remarks?: string;
  risks?: string;
  status: TaskStatus;
  sprintId?: number | null;
  sprintName?: string | null;
  assigneeId?: number | null;
  assigneeDisplayName?: string | null;
  createdBy?: number;
  createdByDisplayName?: string;
  deletedAt?: string | null;
}

export interface BoardTaskFilters {
  subTeamId?: number;
  memberId?: number;
  status?: TaskStatus;
  sprintId?: number;
}

export interface TaskFormValues {
  title: string;
  description: string;
  remarks: string;
  risks: string;
  status: TaskStatus;
  sprintId: number | null;
  assigneeId: number | null;
}

export interface TeamMember {
  id: number;
  teamId: number;
  userId: number;
  username: string;
  displayName: string;
  role: TeamRole;
}

export interface AssignableUser {
  id: number;
  username: string;
  displayName: string;
}

export interface Sprint {
  id: number;
  teamId: number;
  name: string;
  active: boolean;
}

export interface UserAccount {
  id: number;
  username: string;
  displayName: string;
  superAdmin: boolean;
}

export interface SnapshotSettings {
  enabled: boolean;
  cron: string;
  retentionDays: number;
  outputPath: string;
}

export interface SnapshotRunResult {
  fileName: string;
}
