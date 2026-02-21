import { apiGet } from "./client";
import type { Source } from "./types";

export function fetchSources(): Promise<Source[]> {
  return apiGet<Source[]>("/sources");
}
