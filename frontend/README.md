# Frontend Bootstrap Guide

## 목표
- 최신 글 통합 피드, 소스 필터, 태그 필터, 검색 UI 제공

## 권장 스택
- React + TypeScript
- Vite
- React Query (서버 상태)
- React Router
- Zustand (UI 상태가 커질 때)

## 화면 구조 (MVP)
1. 홈 `/`
2. 소스별 목록 `/sources/:sourceName`
3. 게시물 상세 `/posts/:id`

## 구현 순서
1. API 클라이언트/타입 정의
2. 게시물 목록 페이지 + 무한 스크롤(커서 기반)
3. 상단 필터(소스/태그/검색) 적용
4. 상세 페이지 + 원문 링크 이동
5. 로딩/에러/빈 상태 UI 추가

## Docker 빌드
- `docker build -t techmoa-frontend .`

## 현재 생성된 코드
- `package.json`
- `vite.config.ts`
- `index.html`
- `src/main.tsx`
- `src/App.tsx`
- `src/pages/PostDetailPage.tsx`
- `src/styles.css`
- `src/api/*` (posts/sources/tags API client)
