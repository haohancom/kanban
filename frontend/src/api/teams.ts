import { apiRequest } from "./client";
import { AssignableUser, Team, TeamMember, TeamRole } from "../types";

export function listTeams() {
  return apiRequest<Team[]>("/api/teams");
}

export function createTeam(values: { name: string; parentId?: number | null }) {
  return apiRequest<Team>("/api/teams", {
    method: "POST",
    body: values
  });
}

export function updateTeam(teamId: number, values: { name: string }) {
  return apiRequest<Team>(`/api/teams/${teamId}`, {
    method: "PATCH",
    body: values
  });
}

export function listTeamMembers(teamId: number) {
  return apiRequest<TeamMember[]>(`/api/teams/${teamId}/members`);
}

export function listAssignableUsers(teamId: number) {
  return apiRequest<AssignableUser[]>(`/api/teams/${teamId}/members/assignable-users`);
}

export function addTeamMember(teamId: number, values: { userId: number; role: TeamRole }) {
  return apiRequest<TeamMember>(`/api/teams/${teamId}/members`, {
    method: "POST",
    body: values
  });
}

export function updateTeamMember(teamId: number, membershipId: number, values: { role: TeamRole }) {
  return apiRequest<TeamMember>(`/api/teams/${teamId}/members/${membershipId}`, {
    method: "PATCH",
    body: values
  });
}

export function removeTeamMember(teamId: number, membershipId: number) {
  return apiRequest<void>(`/api/teams/${teamId}/members/${membershipId}`, {
    method: "DELETE"
  });
}
