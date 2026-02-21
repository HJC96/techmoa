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

## 단계별 검증 가이드
1. 의존성 설치 및 실행
- `npm install`
- `npm run dev`
2. 목록 화면 확인
- 홈(`/`)에서 게시물 목록이 렌더링되는지 확인합니다.
- `더 보기` 동작으로 커서 기반 추가 로딩이 되는지 확인합니다.
3. 필터/검색 확인
- 소스 필터, 태그 필터, 키워드 검색을 각각 적용해 요청 파라미터가 반영되는지 확인합니다.
4. 상세 페이지 확인
- 목록 항목 클릭 시 `/posts/:id` 이동과 상세 데이터 렌더링을 확인합니다.
5. 빌드 검증
- `npm run build`로 프로덕션 번들 생성 성공 여부를 확인합니다.

## 개선사항 위치
- 프론트엔드 개선 로드맵은 `docs/improvements/FRONTEND_IMPROVEMENTS.md`에서 관리합니다.
