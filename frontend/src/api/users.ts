import { apiRequest } from "./client";
import { UserAccount } from "../types";

export function listUsers() {
  return apiRequest<UserAccount[]>("/api/users");
}

export function createUser(values: {
  username: string;
  displayName: string;
  password: string;
  superAdmin: boolean;
}) {
  return apiRequest<UserAccount>("/api/users", {
    method: "POST",
    body: values
  });
}

export function updateUser(userId: number, values: { displayName?: string; superAdmin?: boolean }) {
  return apiRequest<UserAccount>(`/api/users/${userId}`, {
    method: "PATCH",
    body: values
  });
}

export function resetUserPassword(userId: number, password: string) {
  return apiRequest<void>(`/api/users/${userId}/password`, {
    method: "PATCH",
    body: { password }
  });
}
