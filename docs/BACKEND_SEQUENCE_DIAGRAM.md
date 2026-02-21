# Backend Sequence Diagram

기준: 현재 `main` 브랜치 코드(2026-02-21)

## 1. `GET /api/posts` (목록/검색/다중 소스 OR 필터)
```mermaid
sequenceDiagram
    autonumber
    actor C as "Client (Frontend)"
    participant SEC as "Spring Security FilterChain"
    participant PC as "PostController"
    participant PQS as "PostQueryService"
    participant PR as "PostRepository"
    participant DB as "PostgreSQL"
    participant GEH as "GlobalExceptionHandler"

    C->>SEC: GET /api/posts?cursor&size&sourceId&tag&q
    SEC->>PC: permitAll
    PC->>PQS: getFeed(cursor, size, sourceIds, tag, q)
    PQS->>PQS: normalizeSize / normalizeSourceIds / normalizeText

    alt sourceIds 없음
        PQS->>PR: findFeed(cursor, tag, q, pageable)
    else sourceIds 있음
        PQS->>PR: findFeedBySourceIds(cursor, sourceIds, tag, q, pageable)
    end

    PR->>DB: SELECT posts + JOIN source + optional tag/title/summary filter
    DB-->>PR: List<Post>
    PR-->>PQS: List<Post>
    PQS->>PQS: page slice(size+1), hasNext/nextCursor 계산
    PQS-->>PC: PostFeedResponse
    PC-->>C: 200 OK (items, nextCursor, hasNext)

    opt 예외 발생
        PC-->>GEH: throw Exception
        GEH-->>C: 에러 응답(JSON)
    end
```

## 2. `GET /api/posts/{id}` (상세)
```mermaid
sequenceDiagram
    autonumber
    actor C as "Client (Frontend)"
    participant SEC as "Spring Security FilterChain"
    participant PC as "PostController"
    participant PQS as "PostQueryService"
    participant PR as "PostRepository"
    participant DB as "PostgreSQL"
    participant GEH as "GlobalExceptionHandler"

    C->>SEC: GET /api/posts/{id}
    SEC->>PC: permitAll
    PC->>PQS: getPostDetail(postId)
    PQS->>PR: findDetailById(postId)
    PR->>DB: SELECT DISTINCT post + source + tags
    DB-->>PR: Optional<Post>

    alt 게시물 존재
        PR-->>PQS: Post
        PQS-->>PC: PostDetailResponse
        PC-->>C: 200 OK
    else 게시물 없음
        PR-->>PQS: Optional.empty
        PQS-->>GEH: NoSuchElementException
        GEH-->>C: 404 NOT_FOUND
    end
```

## 3. `GET /api/sources`, `GET /api/tags`
```mermaid
sequenceDiagram
    autonumber
    actor C as "Client (Frontend)"
    participant SEC as "Spring Security FilterChain"
    participant SC as "SourceController"
    participant SS as "SourceService"
    participant SR as "SourceRepository"
    participant TC as "TagController"
    participant TS as "TagService"
    participant TR as "TagRepository"
    participant DB as "PostgreSQL"

    par GET /api/sources
        C->>SEC: GET /api/sources
        SEC->>SC: permitAll
        SC->>SS: findActiveSources()
        SS->>SR: findByActiveTrue()
        SR->>DB: SELECT * FROM sources WHERE active=true
        DB-->>SR: Source list
        SR-->>SS: Source list
        SS->>SS: 이름 정렬
        SS-->>SC: Source list
        SC-->>C: 200 OK
    and GET /api/tags
        C->>SEC: GET /api/tags
        SEC->>TC: permitAll
        TC->>TS: findAll()
        TS->>TR: findAll()
        TR->>DB: SELECT * FROM tags
        DB-->>TR: Tag list
        TR-->>TS: Tag list
        TS->>TS: 이름 정렬
        TS-->>TC: Tag list
        TC-->>C: 200 OK
    end
```

## 4. `POST /api/admin/sources` (관리자 소스 등록)
```mermaid
sequenceDiagram
    autonumber
    actor A as "Admin Client"
    participant SEC as "Spring Security FilterChain"
    participant ADC as "AdminSourceController"
    participant SS as "SourceService"
    participant SR as "SourceRepository"
    participant DB as "PostgreSQL"
    participant GEH as "GlobalExceptionHandler"

    A->>SEC: POST /api/admin/sources + Basic Auth

    alt 인증 실패
        SEC-->>A: 401 Unauthorized
    else 인증 성공
        SEC->>ADC: createSource(request)
        ADC->>ADC: parseParserType()
        ADC->>SS: createSource(name, baseUrl, feedUrl, parserType, intervalMin, active)
        SS->>SS: URL 규칙 검증, RSS feedUrl 필수 검증
        SS->>SR: save(Source)
        SR->>DB: INSERT INTO sources
        DB-->>SR: saved Source
        SR-->>SS: Source
        SS-->>ADC: Source
        ADC-->>A: 200 OK SourceResponse
    end

    opt 유효성/중복/파서 타입 오류
        ADC-->>GEH: ValidationException / ResponseStatusException / DataIntegrityViolationException
        GEH-->>A: 400/409 등 에러 응답
    end
```

## 5. `POST /api/admin/sources/{id}/sync` (관리자 수동 동기화)
```mermaid
sequenceDiagram
    autonumber
    actor A as "Admin Client"
    participant SEC as "Spring Security"
    participant ADC as "AdminSourceController"
    participant SSS as "SourceSyncService"
    participant SR as "SourceRepository"
    participant SJR as "SyncJobRepository"
    participant PRS as "Parser"
    participant PUS as "PostUpsertService"
    participant PR as "PostRepository"
    participant TR as "TagRepository"
    participant DB as "PostgreSQL"

    A->>SEC: POST /api/admin/sources/:id/sync with Basic Auth
    SEC->>ADC: syncSource(sourceId)
    ADC->>SSS: syncSourceById(sourceId)
    SSS->>SR: findById(sourceId)
    SR->>DB: select source
    DB-->>SR: source row
    SR-->>SSS: Source

    SSS->>SJR: save start job
    SJR->>DB: insert sync_jobs status RUNNING
    DB-->>SJR: SyncJob

    SSS->>PRS: fetch posts
    PRS-->>SSS: ParsedPost list
    SSS->>PUS: upsert(source, parsedPosts)

    loop each parsed post
        PUS->>PR: findByCanonicalUrl
        PR->>DB: select post by canonical_url
        DB-->>PR: post or empty
        PR-->>PUS: Optional Post

        PUS->>TR: findByNameIn
        TR->>DB: select tags by names
        DB-->>TR: existing tags
        TR-->>PUS: tag list

        opt new tag
            PUS->>TR: save tag
            TR->>DB: insert tag
            DB-->>TR: saved tag
        end

        PUS->>PR: save post and tags
        PR->>DB: insert or update post and post_tags
        DB-->>PR: saved post
    end

    PUS-->>SSS: savedCount
    SSS->>SJR: save complete job
    SJR->>DB: update sync_jobs status COMPLETED
    DB-->>SJR: updated row
    SSS-->>ADC: SyncResult
    ADC-->>A: 200 OK
```

## 6. 내부 수집 트리거 (스케줄러)
```mermaid
sequenceDiagram
    autonumber
    participant SCH as "SourceSyncScheduler"
    participant SSS as "SourceSyncService"
    participant SR as "SourceRepository"
    participant DB as "PostgreSQL"

    SCH->>SSS: syncActiveSources() @Scheduled
    SSS->>SR: findByActiveTrue()
    SR->>DB: SELECT active sources
    DB-->>SR: source list
    SR-->>SSS: source list

    loop each active source
        SSS->>SSS: syncSource(source) 호출
        Note over SSS: 동기화 내부 플로우는 5번 API의 syncSource와 동일
    end
```

## 7. 예외 응답 흐름 (공통)
```mermaid
sequenceDiagram
    autonumber
    actor C as "Client"
    participant CTL as "Controller"
    participant SVC as "Service"
    participant GEH as "GlobalExceptionHandler"

    C->>CTL: API 요청
    CTL->>SVC: 비즈니스 호출
    SVC-->>CTL: Exception throw
    CTL-->>GEH: 예외 전파
    GEH-->>C: ApiErrorResponse(code, message, timestamp)
```
