import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { fetchPostDetail } from "../api/posts";

export function PostDetailPage() {
  const { postId } = useParams();
  const numericPostId = Number(postId);

  const postQuery = useQuery({
    queryKey: ["post-detail", numericPostId],
    queryFn: () => fetchPostDetail(numericPostId),
    enabled: Number.isFinite(numericPostId)
  });

  if (!Number.isFinite(numericPostId)) {
    return (
      <main className="layout">
        <p>잘못된 게시물 경로입니다.</p>
        <Link to="/">목록으로</Link>
      </main>
    );
  }

  if (postQuery.isLoading) {
    return (
      <main className="layout">
        <p>상세 정보를 불러오는 중입니다...</p>
      </main>
    );
  }

  if (postQuery.isError || !postQuery.data) {
    return (
      <main className="layout">
        <p>게시물을 찾을 수 없습니다.</p>
        <Link to="/">목록으로</Link>
      </main>
    );
  }

  const post = postQuery.data;
  return (
    <main className="layout">
      <Link className="back-link" to="/">
        목록으로
      </Link>
      <article className="panel">
        {post.thumbnailUrl && (
          <div className="detail-thumbnail-wrap">
            <img className="detail-thumbnail" src={post.thumbnailUrl} alt={`${post.title} 썸네일`} loading="lazy" />
          </div>
        )}
        <h1>{post.title}</h1>
        <div className="meta">
          <span>{post.sourceName}</span>
          <span>{post.publishedAt}</span>
        </div>
        {post.author && <p className="detail-author">작성자: {post.author}</p>}
        {post.summary && <p className="detail-summary">{post.summary}</p>}
        <div className="tag-row">
          {post.tags.map((tag) => (
            <span key={tag} className="tag-chip">
              {tag}
            </span>
          ))}
        </div>
        <a className="origin-link" href={post.canonicalUrl} target="_blank" rel="noreferrer">
          원문 보기
        </a>
      </article>
    </main>
  );
}
