# API Spec (MVP)

Base URL: `/api`

인증
- 일반 조회 API: 인증 없음
- 관리자 API(`/admin/**`): HTTP Basic 인증 필요

## 1. 게시물 목록
`GET /posts?cursor={cursorId}&size=20&sourceId={sourceId1}&sourceId={sourceId2}&tag={tagName}&q={keyword}`

참고
- `sourceId`는 다중 전달 가능하며 OR 조건으로 적용됩니다.
- `q`는 게시물 제목/요약 검색에 사용됩니다.

구현 상태: `구현 완료`

응답 예시
```json
{
  "items": [
    {
      "id": 1201,
      "title": "대규모 트래픽 처리 경험",
      "summary": "메시지 큐 기반으로 처리량을 확장한 사례",
      "thumbnailUrl": "https://...",
      "sourceName": "토스",
      "canonicalUrl": "https://...",
      "publishedAt": "2026-02-20"
    }
  ],
  "nextCursor": 1188,
  "hasNext": true
}
```

## 2. 게시물 상세
`GET /posts/{id}`

구현 상태: `구현 완료`

응답 예시
```json
{
  "id": 1201,
  "title": "대규모 트래픽 처리 경험",
  "summary": "메시지 큐 기반으로 처리량을 확장한 사례",
  "thumbnailUrl": "https://...",
  "sourceName": "토스",
  "canonicalUrl": "https://...",
  "author": "Tech Team",
  "tags": ["Kafka", "Scale"],
  "publishedAt": "2026-02-20"
}
```

## 3. 소스 목록
`GET /sources`

구현 상태: `구현 완료`

응답 예시
```json
[
  {
    "id": 1,
    "name": "카카오테크",
    "baseUrl": "https://tech.kakao.com",
    "feedUrl": "https://tech.kakao.com/feed",
    "parserType": "RSS",
    "active": true
  }
]
```

## 4. 태그 목록
`GET /tags`

구현 상태: `구현 완료`

응답 예시
```json
[
  {
    "id": 1,
    "name": "Spring"
  }
]
```

## 5. 관리자 소스 등록
`POST /admin/sources`

구현 상태: `구현 완료`

요청 바디
```json
{
  "name": "카카오",
  "baseUrl": "https://tech.kakao.com",
  "feedUrl": "https://tech.kakao.com/feed",
  "parserType": "RSS",
  "intervalMin": 30,
  "active": true
}
```

## 6. 관리자 수동 동기화
`POST /admin/sources/{id}/sync`

구현 상태: `구현 완료` (동기 실행)

응답
```json
{
  "sourceId": 1,
  "sourceName": "카카오테크",
  "parsedCount": 20,
  "savedCount": 20,
  "status": "COMPLETED"
}
```

## 7. 관리자 히스토리 백필(전체 수집)
`POST /admin/sources/{id}/backfill?sitemapUrl={optionalSitemapUrl}`

구현 상태: `구현 완료` (동기 실행)

설명
- RSS 최신분이 아닌, 사이트맵 기반으로 과거 글까지 한 번에 수집합니다.
- `sitemapUrl`을 생략하면 `robots.txt`의 `Sitemap:` 항목을 우선 사용하고, 없으면 `{baseUrl}/sitemap.xml`을 시도합니다.

응답
```json
{
  "sourceId": 1,
  "sourceName": "카카오테크",
  "parsedCount": 120,
  "savedCount": 118,
  "status": "COMPLETED"
}
```

## 에러 포맷
```json
{
  "code": "INVALID_REQUEST",
  "message": "sourceName is required",
  "timestamp": "2026-02-20T10:13:21"
}
```

## 개선사항 위치
- API 계약 개선 항목은 `docs/improvements/BACKEND_IMPROVEMENTS.md`에서 관리합니다.
