import { CurrentUser } from "../types";
import { apiRequest } from "./client";

export function uploadCurrentUserAvatar(file: File) {
  const body = new FormData();
  body.append("file", file);

  return apiRequest<CurrentUser>("/api/users/me/avatar", {
    method: "PUT",
    body
  });
}

export function deleteCurrentUserAvatar() {
  return apiRequest<CurrentUser>("/api/users/me/avatar", {
    method: "DELETE"
  });
}

export interface UpdateCurrentUserPayload {
  displayName: string;
}

export interface UpdateCurrentUserPasswordPayload {
  currentPassword?: string;
  newPassword: string;
}

export function updateCurrentUser(payload: UpdateCurrentUserPayload) {
  return apiRequest<CurrentUser>("/api/users/me", {
    method: "PATCH",
    body: payload
  });
}

export function updateCurrentUserPassword(payload: UpdateCurrentUserPasswordPayload) {
  return apiRequest<CurrentUser>("/api/users/me/password", {
    method: "PATCH",
    body: payload
  });
}
