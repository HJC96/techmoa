import { apiGet } from "./client";

export type Tag = {
  id: number;
  name: string;
};

export function fetchTags(): Promise<Tag[]> {
  return apiGet<Tag[]>("/tags");
}
