# Backend Bootstrap Guide

## 목표
- Java/Spring Boot 기반 API + 수집 스케줄러를 하나의 애플리케이션으로 시작

## 권장 스택
- Java 21
- Spring Boot 3.4+
- Spring Web, Spring Data JPA, Validation, Actuator
- PostgreSQL, Redis
- Flyway
- Jsoup (HTML 파서), Rome (RSS/Atom)

## 패키지 구조
```text
com.techmoa
├─ TechmoaApplication
├─ common
├─ source
├─ post
├─ ingestion
└─ admin
```

## 초기 구현 순서
1. `sources`, `posts`, `tags`, `post_tags`, `sync_jobs` 엔티티/리포지토리 생성
2. Flyway V1 스키마 작성
3. `GET /api/posts`, `GET /api/sources` 구현
4. `SyncScheduler` + `TechBlogParser` 인터페이스 구현
5. RSS 기반 소스 1개 연동 후 테스트 추가

## 실행 전 준비
- PostgreSQL/Redis 실행 (`../docker-compose.yml`)
- 환경 변수 설정 (`SPRING_PROFILES_ACTIVE=local`)
- 관리자 인증 계정 설정(선택):
  - `TECHMOA_ADMIN_USERNAME` (기본값: `admin`)
  - `TECHMOA_ADMIN_PASSWORD` (기본값: `admin1234`)

## 빌드/테스트
- 테스트 실행: `./gradlew test`
- 로컬 실행: `./gradlew bootRun --args='--spring.profiles.active=local'`

## Docker 빌드
- `docker build -t techmoa-backend .`

## 현재 생성된 코드
- `build.gradle`, `settings.gradle`
- `src/main/java/com/techmoa/TechmoaApplication.java`
- `src/main/java/com/techmoa/ingestion/parser/*`
- `src/main/java/com/techmoa/ingestion/parser/rss/RssTechBlogParser.java`
- `src/main/java/com/techmoa/source/*` (Entity/Repository/Service/Controller)
- `src/main/java/com/techmoa/post/*` (Entity/Repository/QueryService/Controller)
- `src/main/java/com/techmoa/tag/*` (Entity/Repository/Service/Controller)
- `src/main/java/com/techmoa/admin/presentation/AdminSourceController.java`
- `src/main/java/com/techmoa/common/config/SecurityConfig.java`
- `src/main/java/com/techmoa/common/exception/*`
- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/resources/db/migration/V2__seed_sources.sql`
- `src/test/java/com/techmoa/ingestion/parser/rss/RssTechBlogParserTest.java`
- `src/test/java/com/techmoa/post/application/PostUpsertServiceTest.java`
- `src/test/java/com/techmoa/source/application/SourceServiceTest.java`
- `src/test/java/com/techmoa/admin/presentation/AdminSourceControllerSecurityTest.java`
