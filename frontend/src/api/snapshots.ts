import { apiRequest } from "./client";
import { SnapshotRunResult, SnapshotSettings } from "../types";

export function getSettings() {
  return apiRequest<SnapshotSettings>("/api/admin/snapshot-settings");
}

export function updateSettings(settings: SnapshotSettings) {
  return apiRequest<SnapshotSettings>("/api/admin/snapshot-settings", {
    method: "PATCH",
    body: settings
  });
}

export function runSnapshot() {
  return apiRequest<SnapshotRunResult>("/api/admin/snapshots/run", {
    method: "POST"
  });
}
