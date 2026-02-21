import type { FormEvent } from "react";
import { useMemo, useState } from "react";
import { useQuery, useInfiniteQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { fetchPosts } from "./api/posts";
import { fetchSources } from "./api/sources";

function resolveSourceLogoUrl(baseUrl: string): string | null {
  try {
    return `${new URL(baseUrl).origin}/favicon.ico`;
  } catch {
    return null;
  }
}

function resolveSourceHost(baseUrl: string): string {
  try {
    return new URL(baseUrl).hostname.replace(/^www\./, "");
  } catch {
    return baseUrl;
  }
}

type SourceLogoBadgeProps = {
  sourceName: string;
  baseUrl: string;
};

function SourceLogoBadge({ sourceName, baseUrl }: SourceLogoBadgeProps) {
  const [isBroken, setIsBroken] = useState(false);
  const logoUrl = useMemo(() => resolveSourceLogoUrl(baseUrl), [baseUrl]);

  if (!logoUrl || isBroken) {
    return <div className="source-logo-fallback">{sourceName.slice(0, 1).toUpperCase()}</div>;
  }

  return (
    <img
      className="source-logo-image"
      src={logoUrl}
      alt={`${sourceName} 로고`}
      loading="lazy"
      onError={() => setIsBroken(true)}
    />
  );
}

export function App() {
  const [selectedSourceIds, setSelectedSourceIds] = useState<number[]>([]);
  const [titleInput, setTitleInput] = useState("");
  const [titleQuery, setTitleQuery] = useState("");

  const sourceQuery = useQuery({
    queryKey: ["sources"],
    queryFn: fetchSources
  });

  const selectedSourceQueryKey = useMemo(() => [...selectedSourceIds].sort((a, b) => a - b).join("|"), [selectedSourceIds]);

  const postQuery = useInfiniteQuery({
    queryKey: ["posts", selectedSourceQueryKey, titleQuery],
    queryFn: ({ pageParam }) => fetchPosts({
      sourceIds: selectedSourceIds,
      q: titleQuery,
      size: 20,
      cursor: pageParam
    }),
    initialPageParam: null as number | null,
    getNextPageParam: (lastPage) => lastPage.hasNext ? lastPage.nextCursor : undefined,
  });

  const posts = useMemo(() => postQuery.data?.pages.flatMap((page) => page.items) ?? [], [postQuery.data]);
  const sources = sourceQuery.data ?? [];
  const selectedSourceNames = useMemo(() => {
    return sources
      .filter((item) => selectedSourceIds.includes(item.id))
      .map((item) => `#${item.name}`);
  }, [sources, selectedSourceIds]);

  function onTitleSearchSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setTitleQuery(titleInput.trim());
  }

  function toggleSource(sourceId: number) {
    setSelectedSourceIds((prev) => (
      prev.includes(sourceId)
        ? prev.filter((id) => id !== sourceId)
        : [...prev, sourceId]
    ));
  }

  return (
    <main className="layout">
      <header className="hero">
        <h1>TechMoa</h1>
        <p>여러 테크 블로그의 최신 글을 한 곳에서 확인합니다.</p>
      </header>

      <section className="panel">
        <section className="source-explorer" aria-labelledby="source-explorer-title">
          <div className="source-explorer-head">
            <h2 id="source-explorer-title">연동된 테크 블로그</h2>
            <div className="source-head-tools">
              <span className="source-count">{sources.length}개</span>
              {selectedSourceIds.length > 0 && (
                <button type="button" className="source-reset" onClick={() => setSelectedSourceIds([])}>
                  필터 해제
                </button>
              )}
            </div>
          </div>
          <form className="title-search-form" onSubmit={onTitleSearchSubmit}>
            <input
              className="title-search-input"
              type="text"
              value={titleInput}
              onChange={(e) => setTitleInput(e.target.value)}
              placeholder="게시물 제목 검색"
            />
            <button type="submit" className="title-search-button">검색</button>
          </form>
          {selectedSourceIds.length > 0 && (
            <p className="source-selected">
              현재 선택: {selectedSourceNames.join(", ")}
            </p>
          )}
          {titleQuery && <p className="source-selected">제목 검색: "{titleQuery}"</p>}

          {sourceQuery.isLoading && <p className="source-guide">소스 정보를 불러오는 중...</p>}
          {sourceQuery.isError && <p className="source-guide">소스 정보를 불러오지 못했습니다.</p>}
          {!sourceQuery.isLoading && !sourceQuery.isError && (
            <ul className="source-box-grid">
              {sources.map((item) => (
                <li key={item.id}>
                  <button
                    type="button"
                    className={`source-box${selectedSourceIds.includes(item.id) ? " active" : ""}`}
                    onClick={() => toggleSource(item.id)}
                    title={`${item.name} 필터`}
                  >
                    <div className="source-box-row">
                      <SourceLogoBadge sourceName={item.name} baseUrl={item.baseUrl} />
                      <span className="source-box-host">{resolveSourceHost(item.baseUrl)}</span>
                    </div>
                    <span className="source-box-name">#{item.name}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        <h2 className="post-section-title">최신 게시물</h2>
        {postQuery.isLoading && <p>로딩 중...</p>}
        {postQuery.isError && <p>게시물을 불러오지 못했습니다.</p>}
        {!postQuery.isLoading && !postQuery.isError && posts.length === 0 && (
          <p>조건에 맞는 게시물이 없습니다.</p>
        )}
        <ul className="post-list">
          {posts.map((post) => (
            <li key={post.id} className="post-card">
              <Link className="thumbnail-link" to={`/posts/${post.id}`} aria-label={`${post.title} 상세 보기`}>
                {post.thumbnailUrl ? (
                  <img className="post-thumbnail" src={post.thumbnailUrl} alt={`${post.title} 썸네일`} loading="lazy" />
                ) : (
                  <div className="post-thumbnail-placeholder">
                    <span>{post.sourceName}</span>
                  </div>
                )}
              </Link>
              <Link className="post-title" to={`/posts/${post.id}`}>
                {post.title}
              </Link>
              <div className="meta">
                <span>{post.sourceName}</span>
                <span>{post.publishedAt}</span>
              </div>
              {post.summary && <p className="summary">{post.summary}</p>}
            </li>
          ))}
        </ul>
        {postQuery.hasNextPage && (
          <button
            onClick={() => postQuery.fetchNextPage()}
            disabled={postQuery.isFetchingNextPage || postQuery.isFetching}
            className="load-more"
          >
            {postQuery.isFetchingNextPage ? "로딩 중..." : "더 보기"}
          </button>
        )}
      </section>
    </main>
  );
}
