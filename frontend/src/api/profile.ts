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
