import { apiRequest } from "./client";
import { BoardTask } from "../types";

export function listDeletedTasks(teamId: number) {
  return apiRequest<BoardTask[]>(`/api/teams/${teamId}/recycle-bin/tasks`);
}

export function restoreTask(taskId: number) {
  return apiRequest<void>(`/api/recycle-bin/tasks/${taskId}/restore`, {
    method: "POST"
  });
}

export function deleteTaskForever(taskId: number) {
  return apiRequest<void>(`/api/recycle-bin/tasks/${taskId}`, {
    method: "DELETE"
  });
}

export function bulkDeleteTasks(taskIds: number[]) {
  return apiRequest<void>("/api/recycle-bin/tasks/bulk-delete", {
    method: "POST",
    body: { taskIds }
  });
}

export function deleteAllTasks(teamId: number) {
  return apiRequest<void>(`/api/teams/${teamId}/recycle-bin/tasks`, {
    method: "DELETE"
  });
}
