# TechMoa

여러 테크 블로그의 최신 글을 수집하고 한 곳에서 탐색하는 서비스입니다.

## 문서
- `docs/TECHMOA_PROJECT_LOG.md`: 진행 로그 + 다음 단계 체크리스트
- `docs/ARCHITECTURE.md`: 시스템 아키텍처 및 모듈 구조
- `docs/ERD.md`: 데이터 모델 및 인덱스 전략
- `docs/API_SPEC.md`: MVP API 명세
- `docs/INGESTION_PLAYBOOK.md`: 수집 파이프라인/파서 전략
- `backend/README.md`: 백엔드 부트스트랩 가이드
- `frontend/README.md`: 프론트엔드 부트스트랩 가이드
- `docker-compose.yml`: 로컬 PostgreSQL/Redis 실행

## 실행 방법

### 로컬 실행
1.  **DB 및 Redis 실행**: `docker-compose up -d postgres redis`
2.  **백엔드 실행**: `cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'`
3.  **프론트엔드 실행**: `cd frontend && npm install && npm run dev`

### 전체 배포 (Docker Compose)
1.  **빌드 및 실행**: `docker-compose up -d --build`
2.  **정지 및 삭제**: `docker-compose down`

## 현재 상태
- [x] 요구사항 기반 초안 아키텍처 정의
- [x] MVP 데이터 모델 정의
- [x] MVP API 정의
- [x] 수집 파이프라인 정의
- [x] Spring Boot 프로젝트 초기 스캐폴딩
- [x] React 프로젝트 초기 스캐폴딩
- [x] Flyway V1 스키마 작성
- [x] 기본 조회/관리 API 골격 구현
- [x] 태그 저장/조회/필터 구현
- [x] sync_jobs 동기화 이력 저장 구현
- [x] 관리자 API 기본 인증 적용
- [x] 프론트 실 API 연동 (목데이터 제거)
- [x] 프론트 상세 페이지 라우팅(`/posts/:id`) 구현
- [x] 단위/슬라이스 테스트 코드 추가
- [x] 백엔드 테스트 자동 실행 확인
- [x] 프론트 빌드 실행 확인
- [x] 1차 수집 소스 3개 연동 (RSS 우선)
