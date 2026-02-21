import { apiGet } from "./client";
import type { PostDetail, PostFeed } from "./types";

type FetchPostsParams = {
  sourceIds?: number[];
  tag?: string;
  q?: string;
  size?: number;
  cursor?: number | null;
};

export function fetchPosts(params: FetchPostsParams): Promise<PostFeed> {
  const query = new URLSearchParams();
  if (params.sourceIds && params.sourceIds.length > 0) {
    params.sourceIds.forEach((sourceId) => {
      query.append("sourceId", String(sourceId));
    });
  }
  if (params.q) {
    query.set("q", params.q);
  }
  if (params.tag) {
    query.set("tag", params.tag);
  }
  if (params.cursor !== null && params.cursor !== undefined) {
    query.set("cursor", String(params.cursor));
  }
  query.set("size", String(params.size ?? 20));

  return apiGet<PostFeed>(`/posts?${query.toString()}`);
}

export function fetchPostDetail(postId: number): Promise<PostDetail> {
  return apiGet<PostDetail>(`/posts/${postId}`);
}
