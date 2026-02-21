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
  const [source, setSource] = useState("");
  const [sourceSearch, setSourceSearch] = useState("");

  const sourceQuery = useQuery({
    queryKey: ["sources"],
    queryFn: fetchSources
  });

  const postQuery = useInfiniteQuery({
    queryKey: ["posts", source],
    queryFn: ({ pageParam }) => fetchPosts({ source, size: 20, cursor: pageParam }),
    initialPageParam: null as number | null,
    getNextPageParam: (lastPage) => lastPage.hasNext ? lastPage.nextCursor : undefined,
  });

  const posts = useMemo(() => postQuery.data?.pages.flatMap((page) => page.items) ?? [], [postQuery.data]);
  const filteredSources = useMemo(() => {
    const sources = sourceQuery.data ?? [];
    const normalizedSearch = sourceSearch.trim().toLowerCase();
    if (!normalizedSearch) {
      return sources;
    }
    return sources.filter((item) => (
      item.name.toLowerCase().includes(normalizedSearch)
      || resolveSourceHost(item.baseUrl).toLowerCase().includes(normalizedSearch)
    ));
  }, [sourceQuery.data, sourceSearch]);

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
            <span className="source-count">{sourceQuery.data?.length ?? 0}개</span>
          </div>
          <div className="source-toolbar">
            <input
              className="source-search-input"
              type="text"
              value={sourceSearch}
              onChange={(e) => setSourceSearch(e.target.value)}
              placeholder="블로그 이름 또는 도메인 검색"
            />
            {source && (
              <button type="button" className="source-reset" onClick={() => setSource("")}>
                필터 해제
              </button>
            )}
          </div>
          {source && <p className="source-selected">현재 선택: #{source}</p>}

          {sourceQuery.isLoading && <p className="source-guide">소스 정보를 불러오는 중...</p>}
          {sourceQuery.isError && <p className="source-guide">소스 정보를 불러오지 못했습니다.</p>}
          {!sourceQuery.isLoading && !sourceQuery.isError && (
            <ul className="source-box-grid">
              {filteredSources.map((item) => (
                <li key={item.id}>
                  <button
                    type="button"
                    className={`source-box${source === item.name ? " active" : ""}`}
                    onClick={() => setSource((prev) => prev === item.name ? "" : item.name)}
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
