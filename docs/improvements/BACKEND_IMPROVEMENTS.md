# Backend Improvements

## 최근 반영 이력
### 2026-02-21. 소스별 수집 주기(intervalMin) 준수 로직 반영
1. 수집 대상 선별 로직 추가
- `backend/src/main/java/com/techmoa/ingestion/application/SourceSyncService.java`에서 활성 소스 순회 시 `intervalMin`과 최근 수집 시각을 비교해 미도래 소스를 스킵하도록 보완
2. 최근 수집 시각 조회 쿼리 추가
- `backend/src/main/java/com/techmoa/ingestion/domain/SyncJobRepository.java`에 `findLatestStartedAtBySourceId` 추가
3. 단위 테스트 추가
- `backend/src/test/java/com/techmoa/ingestion/application/SourceSyncServiceTest.java` 신설
- 주기 미도래 스킵, 주기 도래 실행 시나리오 검증

### 2026-02-21. 백엔드 API/내부 연동 시퀀스 다이어그램 문서화
1. 시퀀스 다이어그램 문서 추가
- `docs/BACKEND_SEQUENCE_DIAGRAM.md` 신설
2. API별 흐름 문서화
- `GET /posts`, `GET /posts/{id}`, `GET /sources`, `GET /tags`, `POST /admin/sources`, `POST /admin/sources/{id}/sync`의 호출 체인을 코드 기준으로 정리
3. 내부 연동 흐름 문서화
- `SourceSyncService`, `RssTechBlogParser`, `PostUpsertService`, `SyncJobRepository`, `GlobalExceptionHandler` 연결 흐름을 시퀀스로 정리
4. Mermaid 파서 호환성 보정
- `POST /api/admin/sources/{id}/sync` 다이어그램의 텍스트/기호를 단순화해 렌더링 오류가 나지 않도록 수정

### 2026-02-21. 게시물 조회 다중 소스 OR 필터 API 지원
1. API 파라미터 확장
- `backend/src/main/java/com/techmoa/post/presentation/PostController.java`에서 `sourceId`를 `List<Long>`으로 수신
2. 서비스 정규화 로직 추가
- `backend/src/main/java/com/techmoa/post/application/PostQueryService.java`에서 소스 ID 목록 정규화(중복 제거/빈 목록 null 처리)
3. 조회 쿼리 OR 필터 반영
- `backend/src/main/java/com/techmoa/post/domain/PostRepository.java`에서 `s.id IN :sourceIds` 조건 적용
- 다중 소스 필터를 별도 쿼리(`findFeedBySourceIds`)로 분리해 안정적으로 OR 조회
4. 회귀 테스트 추가
- `backend/src/test/java/com/techmoa/post/domain/PostRepositoryTest.java`에서 다중 소스 OR 조회 검증

검증
- `backend ./gradlew test` 성공

### 2026-02-21. RSS 썸네일 파싱 및 API 노출
1. 수집 모델 확장
- `backend/src/main/java/com/techmoa/ingestion/parser/ParsedPost.java`에 `thumbnailUrl` 추가
- `backend/src/main/java/com/techmoa/ingestion/parser/rss/RssTechBlogParser.java`에서 RSS `enclosure`/본문 `img` 기반 썸네일 추출 추가
2. 저장 로직 반영
- `backend/src/main/java/com/techmoa/post/application/PostUpsertService.java`에서 썸네일 저장/갱신 처리
3. 응답 계약 확장
- `backend/src/main/java/com/techmoa/post/presentation/dto/PostItemResponse.java`
- `backend/src/main/java/com/techmoa/post/presentation/dto/PostDetailResponse.java`
- 목록/상세 응답에 `thumbnailUrl` 포함
4. 테스트 보강
- `backend/src/test/java/com/techmoa/ingestion/parser/rss/RssTechBlogParserTest.java`
- `backend/src/test/java/com/techmoa/post/application/PostUpsertServiceTest.java`
- `backend/src/test/resources/fixtures/sample-rss.xml`

검증
- `backend ./gradlew test` 성공

## 1. API 계약 안정화
1. 응답 포맷 표준화
- 날짜 포맷(`publishedAt`) 단일 규칙 고정
- 페이지네이션 필드(`items`, `nextCursor`, `hasNext`) 일관성 유지
2. 상태 코드 표 확장
- API별 성공/실패 코드(`200`, `201`, `400`, `401`, `404`, `500`) 명시
3. 요청/응답 스키마 분리
- 필수/선택 필드 및 검증 규칙 문서화

완료 기준
- 주요 API 문서에 공통 포맷/코드 표 반영 완료

## 2. 계약/회귀 테스트 고도화
1. 계약 테스트 자동화
- 대상: `GET /posts`, `GET /posts/{id}`, `POST /admin/sources/{id}/sync`
2. 통합 테스트 확장
- 저장/조회/동기화 경로를 포함한 엔드투엔드 백엔드 흐름 검증

완료 기준
- API 변경 시 테스트 실패로 계약 깨짐 즉시 감지 가능

## 3. 수집(ingestion) 신뢰성 강화
1. 신규 소스 온보딩 표준화
- 정책 확인 -> 파서 선택 -> 샘플 검증 -> 스테이징 검증 -> 운영 전환
2. 장애 대응 절차 정교화
- 장애 식별 -> 원인 분류 -> 단기 조치 -> 영향 보정 -> 재발 방지
3. 실패 데이터 구조화
- `sync_jobs`를 기준으로 실패 원인 분류 집계 가능하도록 정리

완료 기준
- 신규 소스 추가 시 동일 체크리스트 기반으로 온보딩 가능

## 4. 데이터 모델/성능 개선
1. 무결성 강화
- 핵심 유니크 제약(`sources.name`, `posts.canonical_url`, `tags.name`) 운영 유지
2. 조회 성능 최적화
- 목록/검색 쿼리 실행 계획 점검 및 인덱스 전략 업데이트
3. 이력 보관 정책 적용
- `sync_jobs` 보관 기간 및 정리 배치 정책 수립
4. 스키마 변경 표준화
- Flyway 기반 변경, 배포 전/후 버전 검증, 롤백 절차 문서화

완료 기준
- 인덱스/보관/마이그레이션 운영 절차가 문서와 실제 배포에 일치
