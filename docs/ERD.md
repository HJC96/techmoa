# ERD (MVP)

## 테이블

### `sources`
- `id` BIGSERIAL PK
- `name` VARCHAR(100) NOT NULL
- `base_url` TEXT NOT NULL
- `feed_url` TEXT NULL
- `parser_type` VARCHAR(50) NOT NULL
- `interval_min` INT NOT NULL DEFAULT 30
- `active` BOOLEAN NOT NULL DEFAULT TRUE
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

인덱스
- `idx_sources_active (active)`
- `uk_sources_name (name)` UNIQUE

### `posts`
- `id` BIGSERIAL PK
- `source_id` BIGINT NOT NULL FK -> `sources.id`
- `canonical_url` TEXT NOT NULL
- `title` VARCHAR(500) NOT NULL
- `summary` TEXT NULL
- `author` VARCHAR(200) NULL
- `thumbnail_url` TEXT NULL
- `published_at` TIMESTAMP NOT NULL
- `fetched_at` TIMESTAMP NOT NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

인덱스
- `uk_posts_canonical_url (canonical_url)` UNIQUE
- `idx_posts_published_at (published_at DESC)`
- `idx_posts_source_published (source_id, published_at DESC)`

### `tags`
- `id` BIGSERIAL PK
- `name` VARCHAR(100) NOT NULL
- `created_at` TIMESTAMP NOT NULL

인덱스
- `uk_tags_name (name)` UNIQUE

### `post_tags`
- `post_id` BIGINT NOT NULL FK -> `posts.id`
- `tag_id` BIGINT NOT NULL FK -> `tags.id`

인덱스
- `pk_post_tags (post_id, tag_id)` PRIMARY KEY
- `idx_post_tags_tag_id (tag_id)`

### `sync_jobs`
- `id` BIGSERIAL PK
- `source_id` BIGINT NOT NULL FK -> `sources.id`
- `status` VARCHAR(30) NOT NULL
- `started_at` TIMESTAMP NOT NULL
- `ended_at` TIMESTAMP NULL
- `success_count` INT NOT NULL DEFAULT 0
- `failure_count` INT NOT NULL DEFAULT 0
- `error_message` TEXT NULL

인덱스
- `idx_sync_jobs_source_started (source_id, started_at DESC)`
- `idx_sync_jobs_status (status)`

## 관계 요약
- `sources 1:N posts`
- `posts N:M tags` via `post_tags`
- `sources 1:N sync_jobs`

## 구현 메모
- `canonical_url`은 중복 제거의 기준 키
- `published_at`이 없는 소스는 `fetched_at` 대체 후 품질 경고 로그 남김
