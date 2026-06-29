import { apiRequest } from "./client";
import { Sprint } from "../types";

export function listSprints(teamId: number) {
  return apiRequest<Sprint[]>(`/api/teams/${teamId}/sprints`);
}

function scopedUrl(sprintId: number, teamId?: number) {
  const suffix = teamId === undefined ? "" : `?teamId=${teamId}`;
  return `/api/sprints/${sprintId}${suffix}`;
}

export function createSprint(teamId: number, values: { name: string }) {
  return apiRequest<Sprint>(`/api/teams/${teamId}/sprints`, {
    method: "POST",
    body: values
  });
}

export function updateSprint(sprintId: number, values: { name?: string; active?: boolean }, teamId?: number) {
  return apiRequest<Sprint>(scopedUrl(sprintId, teamId), {
    method: "PATCH",
    body: values
  });
}

export function deleteSprint(sprintId: number, teamId?: number) {
  return apiRequest<void>(scopedUrl(sprintId, teamId), {
    method: "DELETE"
  });
}
