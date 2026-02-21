CREATE TABLE sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    base_url TEXT NOT NULL,
    feed_url TEXT,
    parser_type VARCHAR(50) NOT NULL,
    interval_min INT NOT NULL DEFAULT 30,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sources_name ON sources (name);
CREATE INDEX idx_sources_active ON sources (active);

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources (id),
    canonical_url TEXT NOT NULL,
    title VARCHAR(500) NOT NULL,
    summary TEXT,
    author VARCHAR(200),
    thumbnail_url TEXT,
    published_at TIMESTAMP NOT NULL,
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_posts_canonical_url ON posts (canonical_url);
CREATE INDEX idx_posts_published_at ON posts (published_at DESC);
CREATE INDEX idx_posts_source_published ON posts (source_id, published_at DESC);

CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_tags_name ON tags (name);

CREATE TABLE post_tags (
    post_id BIGINT NOT NULL REFERENCES posts (id),
    tag_id BIGINT NOT NULL REFERENCES tags (id),
    PRIMARY KEY (post_id, tag_id)
);

CREATE INDEX idx_post_tags_tag_id ON post_tags (tag_id);

CREATE TABLE sync_jobs (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources (id),
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE INDEX idx_sync_jobs_source_started ON sync_jobs (source_id, started_at DESC);
CREATE INDEX idx_sync_jobs_status ON sync_jobs (status);
