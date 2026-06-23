import { apiRequest } from "./client";
import { Team } from "../types";

export function listTeams() {
  return apiRequest<Team[]>("/api/teams");
}
