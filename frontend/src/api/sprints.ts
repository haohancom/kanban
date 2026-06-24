import { apiRequest } from "./client";
import { Sprint } from "../types";

export function listSprints(teamId: number) {
  return apiRequest<Sprint[]>(`/api/teams/${teamId}/sprints`);
}

export function createSprint(teamId: number, values: { name: string }) {
  return apiRequest<Sprint>(`/api/teams/${teamId}/sprints`, {
    method: "POST",
    body: values
  });
}

export function updateSprint(sprintId: number, values: { name?: string; active?: boolean }) {
  return apiRequest<Sprint>(`/api/sprints/${sprintId}`, {
    method: "PATCH",
    body: values
  });
}
