export type Source = {
  id: number;
  name: string;
  baseUrl: string;
  feedUrl: string | null;
  parserType: string;
  active: boolean;
};

export type PostItem = {
  id: number;
  title: string;
  summary: string | null;
  sourceName: string;
  canonicalUrl: string;
  publishedAt: string; // YYYY-MM-DD
};

export type PostFeed = {
  items: PostItem[];
  nextCursor: number | null;
  hasNext: boolean;
};

export type PostDetail = {
  id: number;
  title: string;
  summary: string | null;
  sourceName: string;
  canonicalUrl: string;
  author: string | null;
  tags: string[];
  publishedAt: string; // YYYY-MM-DD
};
