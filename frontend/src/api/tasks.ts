import { apiRequest } from "./client";
import { BoardTask, BoardTaskFilters, Sprint, TaskFormValues, TaskStatus, TeamMember } from "../types";

type TaskPayload = Partial<Omit<TaskFormValues, "status"> & { status: TaskStatus }>;

export function listBoardTasks(teamId: number, filters: BoardTaskFilters = {}) {
  const query = new URLSearchParams();

  appendFilter(query, "subTeamId", filters.subTeamId);
  appendFilter(query, "memberId", filters.memberId);
  appendFilter(query, "status", filters.status);
  appendFilter(query, "sprintId", filters.sprintId);

  const suffix = query.toString() ? `?${query.toString()}` : "";
  return apiRequest<BoardTask[]>(`/api/teams/${teamId}/board/tasks${suffix}`);
}

export function createTask(teamId: number, values: TaskFormValues) {
  return apiRequest<BoardTask>(`/api/teams/${teamId}/tasks`, {
    method: "POST",
    body: values
  });
}

export function updateTask(taskId: number, values: TaskPayload) {
  return apiRequest<BoardTask>(`/api/tasks/${taskId}`, {
    method: "PATCH",
    body: values
  });
}

export function listTeamMembers(teamId: number) {
  return apiRequest<TeamMember[]>(`/api/teams/${teamId}/members`);
}

export function listTeamSprints(teamId: number) {
  return apiRequest<Sprint[]>(`/api/teams/${teamId}/sprints`);
}

function appendFilter(query: URLSearchParams, key: string, value: number | string | undefined) {
  if (value !== undefined) {
    query.set(key, String(value));
  }
}
