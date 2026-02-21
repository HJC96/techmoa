import type { FormEvent } from "react";
import { useMemo, useState } from "react";
import { useQuery, useInfiniteQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { fetchPosts } from "./api/posts";
import { fetchSources } from "./api/sources";
import { fetchTags } from "./api/tags";

export function App() {
  const [source, setSource] = useState("");
  const [tag, setTag] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");

  const sourceQuery = useQuery({
    queryKey: ["sources"],
    queryFn: fetchSources
  });

  const tagQuery = useQuery({
    queryKey: ["tags"],
    queryFn: fetchTags
  });

  const postQuery = useInfiniteQuery({
    queryKey: ["posts", source, tag, keyword],
    queryFn: ({ pageParam }) => fetchPosts({ source, tag, q: keyword, size: 20, cursor: pageParam }),
    initialPageParam: null as number | null,
    getNextPageParam: (lastPage) => lastPage.hasNext ? lastPage.nextCursor : undefined,
  });

  const posts = useMemo(() => postQuery.data?.pages.flatMap((page) => page.items) ?? [], [postQuery.data]);

  function onSearchSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setKeyword(keywordInput.trim());
  }

  return (
    <main className="layout">
      <header className="hero">
        <h1>TechMoa</h1>
        <p>여러 테크 블로그의 최신 글을 한 곳에서 확인합니다.</p>
      </header>

      <section className="panel">
        <form className="filters" onSubmit={onSearchSubmit}>
          <select value={source} onChange={(e) => setSource(e.target.value)}>
            <option value="">전체 소스</option>
            {sourceQuery.data?.map((item) => (
              <option key={item.id} value={item.name}>
                {item.name}
              </option>
            ))}
          </select>
          <select value={tag} onChange={(e) => setTag(e.target.value)}>
            <option value="">전체 태그</option>
            {tagQuery.data?.map((item) => (
              <option key={item.id} value={item.name}>
                {item.name}
              </option>
            ))}
          </select>
          <input
            type="text"
            placeholder="키워드 검색"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
          />
          <button type="submit">검색</button>
        </form>

        <h2>최신 게시물</h2>
        {postQuery.isLoading && <p>로딩 중...</p>}
        {postQuery.isError && <p>게시물을 불러오지 못했습니다.</p>}
        {!postQuery.isLoading && !postQuery.isError && posts.length === 0 && (
          <p>조건에 맞는 게시물이 없습니다.</p>
        )}
        <ul className="post-list">
          {posts.map((post) => (
            <li key={post.id} className="post-card">
              <Link to={`/posts/${post.id}`}>
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
