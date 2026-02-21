# TECHMOA Project Log

## 프로젝트 목표
- 카카오/인프런/토스 등 테크 블로그의 최신 게시물을 한 곳에서 확인
- 사용자에게는 빠른 탐색 경험 제공, 운영자는 소스 추가/장애 대응이 쉬운 구조 유지

## 범위 (MVP)
- 게시물 목록/상세 조회
- 소스 필터, 태그 필터, 검색(제목/요약 기반)
- 정기 수집 + 수동 수집 트리거
- 중복 제거(canonical URL 기준) 및 수집 실패 로깅

## 의사결정 로그
### 2026-02-20
1. 아키텍처는 MSA 대신 `모듈러 모놀리식`으로 시작
2. 수집 우선순위는 `RSS/Atom > Sitemap > HTML 파싱`
3. DB는 PostgreSQL, 캐시는 Redis 적용
4. 검색은 MVP에서 DB 인덱스 검색으로 시작하고 추후 OpenSearch 확장

## 작업 이력
### 2026-02-20 (1차 설계 문서화)
- `README.md` 생성: 프로젝트 개요 및 문서 인덱스 정리
- `docs/ARCHITECTURE.md` 생성: 아키텍처/모듈/확장 기준 정리
- `docs/ERD.md` 생성: 엔티티/인덱스/관계/구현 메모 정리
- `docs/API_SPEC.md` 생성: MVP API 및 요청/응답 포맷 정리
- `docs/INGESTION_PLAYBOOK.md` 생성: 수집 플로우/재시도/온보딩 규칙 정리
### 2026-02-20 (2차 실행 준비)
- `backend/README.md` 생성: 백엔드 초기 구현 순서와 패키지 구조 정리
- `frontend/README.md` 생성: 프론트 MVP 화면/구현 순서 정리
- `docker-compose.yml` 생성: PostgreSQL/Redis 로컬 실행 베이스 추가
### 2026-02-20 (3차 초기 코드 스캐폴딩)
- `backend/build.gradle`, `backend/settings.gradle` 생성
- `backend/src/main/java/com/techmoa/TechmoaApplication.java` 생성
- `backend/src/main/java/com/techmoa/ingestion/parser/*` 생성 (파서 인터페이스 초안)
- `frontend/package.json`, `frontend/vite.config.ts`, `frontend/src/*` 생성
- `techmoa/.gitignore` 생성
### 2026-02-20 (4차 DB 마이그레이션)
- `backend/src/main/resources/db/migration/V1__init_schema.sql` 생성
- `sources/posts/tags/post_tags/sync_jobs` 초기 스키마 반영
### 2026-02-20 (5차 수집 기본 구현)
- `source/domain/Source`, `SourceRepository` 생성
- `ingestion/application/SourceSyncService` 생성
- `ingestion/scheduler/SourceSyncScheduler` 생성
- `ingestion/parser/rss/RssTechBlogParser` 생성
- `TechmoaApplication`에 `@EnableScheduling` 적용
### 2026-02-20 (6차 조회/관리 API 1차 구현)
- `GET /api/sources` 구현 (`source/presentation/SourceController`)
- `POST /api/admin/sources` 구현 (`admin/presentation/AdminSourceController`)
- `GET /api/posts` 구현 (`post/presentation/PostController`, cursor 기반)
- `PostQueryService`, `PostRepository` 추가
### 2026-02-20 (7차 수동 동기화 API)
- `POST /api/admin/sources/{id}/sync` 구현
- `SourceSyncService.syncSourceById()` 추가
### 2026-02-20 (8차 파싱 결과 저장)
- `PostUpsertService` 추가 (canonical URL 기준 upsert)
- `SourceSyncService`에 `postUpsertService` 연동
- 수집 완료 로그에 `parsedCount/savedCount` 기록
### 2026-02-20 (9차 초기 소스 시드)
- `V2__seed_sources.sql` 생성
- 카카오/인프런/토스 RSS 소스 기본값 추가 (`ON CONFLICT DO NOTHING`)
### 2026-02-20 (10차 프론트 API 연동)
- `frontend/src/api/*` 생성 (`GET /posts`, `GET /sources` 호출)
- `frontend/src/App.tsx`에서 목데이터 제거 후 실 API 연동
- 소스 필터/키워드 검색 UI 연결
### 2026-02-20 (11차 태그 저장/조회 연결)
- `tag/domain/Tag`, `TagRepository` 추가
- `TagController` 추가 (`GET /api/tags`)
- `PostUpsertService`에 태그 upsert + `post_tags` 매핑 저장 연결
- `GET /api/posts`에 `tag` 필터 구현
- 프론트에 태그 필터 UI/API 연동
### 2026-02-20 (12차 입력 검증/에러 응답 개선)
- `SourceService`에 URL 검증 및 RSS feedUrl 필수 검증 추가
- `common/exception/GlobalExceptionHandler` 추가
- 공통 에러 포맷 `ApiErrorResponse` 적용
### 2026-02-20 (13차 관리자 API 인증)
- `spring-boot-starter-security` 추가
- `SecurityConfig` 추가: `/api/admin/**` HTTP Basic 보호
- `application-local.yml`에 관리자 계정 환경변수 설정 추가
### 2026-02-20 (14차 테스트 보강)
- RSS 파서 fixture 테스트 추가 (`RssTechBlogParserTest`)
- 태그/업서트 저장 테스트 추가 (`PostUpsertServiceTest`)
- 관리자 인증 테스트 추가 (`AdminSourceControllerSecurityTest`)
- 소스 URL 검증 단위 테스트 추가 (`SourceServiceTest`)
### 2026-02-20 (15차 수동 동기화 응답 고도화)
- `SyncResult` 도입
- 수동 동기화 API 응답에 `sourceName/parsedCount/savedCount` 추가
- 스케줄러 동기화는 소스 단위 예외 격리 유지
### 2026-02-20 (16차 sync_jobs 영속화)
- `ingestion/domain/SyncJob`, `SyncJobRepository`, `SyncJobStatus` 추가
- 동기화 시작 시 `RUNNING`, 성공 시 `COMPLETED`, 실패 시 `FAILED` 저장
- 실패 시 `errorMessage` 저장
### 2026-02-20 (17차 게시물 상세 API)
- `GET /api/posts/{id}` 구현
- 상세 응답에 `tags` 포함
- 미존재 게시물 요청 시 `404 NOT_FOUND` 처리
### 2026-02-20 (18차 빌드/테스트 자동 검증)
- `backend/gradlew`, `backend/gradle/wrapper/*` 추가
- `./gradlew test` 실행 성공 (9 tests, BUILD SUCCESSFUL)
- `TechmoaApplicationTests`는 test profile에서 Flyway 비활성화로 보정
### 2026-02-20 (19차 프론트 상세 라우팅)
- `/posts/:postId` 라우트 추가 (`PostDetailPage`)
- 목록 페이지에서 상세 페이지 이동 링크 적용
- 상세 페이지에서 태그/원문 링크 렌더링

### 2026-02-21 (20차 무한 스크롤 및 배포 준비)
- `frontend` 무한 스크롤 구현 (Load More 버튼 방식)
  - `api/posts.ts`: `cursor` 파라미터 추가
  - `App.tsx`: `useInfiniteQuery` 도입 및 더 보기 버튼 추가
- `Dockerfile` 작성
  - `backend/Dockerfile`: Multi-stage build (Gradle build -> JRE alpine)
  - `frontend/Dockerfile`: Multi-stage build (Node build -> Nginx alpine)
  - `frontend/nginx.conf`: SPA 라우팅 지원 설정
- 빌드 검증
  - `backend`: `./gradlew build` 성공
  - `frontend`: `npm install && npm run build` 성공

### 2026-02-21 (21차 환경 호환성 및 보안 설정)
- **PostgreSQL 16 지원**: `backend/build.gradle`에 `flyway-database-postgresql` 의존성 추가 (Flyway 10+ 호환성 해결)
- **CORS 설정**: `SecurityConfig.java`에 `CorsConfigurationSource` 추가하여 프론트엔드 접속 허용
- **쿼리 오류 수정**: PostgreSQL `LOWER(bytea)` 오류 해결을 위해 검색 파라미터에 `CAST(:q AS string)` 적용
- **날짜 포맷 정규화**: `publishedAt`을 서버 응답 시 `yyyy-MM-dd` 문자열로 변환

### 2026-02-21 (22차 AWS EC2 운영 환경 구축 및 배포)
- **인프라 오케스트레이션**: 루트 `docker-compose.yml` 고도화 (DB, Redis, Backend, Frontend 통합)
- **운영 프로필 설정**: `application-prod.yml` 추가 및 Docker 환경 변수 연동
- **EC2 성능 최적화**: 
  - t2.micro(1GB RAM) 환경을 위한 **Swap Memory(2G)** 설정
  - Docker Buildx 최신 버전(0.17.1) 수동 업데이트로 빌드 오류 해결
- **네트워크 설정**: Elastic IP(탄력적 IP) 할당 및 보안 그룹(80, 8080, 22) 설정 가이드
- **최종 배포 성공**: `docker-compose up -d --build`를 통한 전체 서비스 가동 확인

## 진행 체크리스트
- [x] 상위 아키텍처 정의
- [x] 도메인/DB 모델 정의
- [x] API 초안 정의
- [x] 수집 파이프라인/에러 처리 전략 정의
- [x] Spring Boot 코드 베이스 생성 (초기 스캐폴딩)
- [x] React 코드 베이스 생성 (초기 스캐폴딩)
- [x] 로컬 개발환경(docker compose) 구성
- [x] Flyway V1 스키마 생성
- [x] 첫 소스 3개 수집기 구현 (RSS 접근 검증 완료)
- [x] 기본 조회/관리 API 골격 구현
- [x] 수동 동기화 API 구현
- [x] 게시물 저장 upsert 기본 구현
- [x] 태그 저장/조회/필터 구현
- [x] sync_jobs 동기화 이력 저장 구현
- [x] 게시물 상세 조회 API 구현
- [x] 관리자 API 기본 인증 적용
- [x] 프론트 실 API 연동 (목데이터 제거)
- [x] 프론트 상세 페이지 라우팅(`/posts/:id`) 구현
- [x] 단위/슬라이스 테스트 코드 추가
- [x] 백엔드 자동 테스트 실행 확인
- [x] 무한 스크롤/커서 pagination UI 적용
- [x] Docker 기반 배포 환경 구축 (Dockerfile, Compose)
- [x] AWS EC2 실서버 배포 및 운영 환경 최적화
- [ ] 통합 테스트 작성 및 E2E 테스트 검토

## 확인 필요 이슈
- 현재 로컬 `gradle` 명령은 네이티브 라이브러리 로딩 오류로 실행 실패 (wrapper로 우회)
