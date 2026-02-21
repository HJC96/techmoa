INSERT INTO sources (name, base_url, feed_url, parser_type, interval_min, active)
VALUES
    ('카카오테크', 'https://tech.kakao.com', 'https://tech.kakao.com/feed', 'RSS', 30, TRUE),
    ('인프런테크', 'https://tech.inflab.com', 'https://tech.inflab.com/rss.xml', 'RSS', 30, TRUE),
    ('토스테크', 'https://toss.tech', 'https://toss.tech/rss.xml', 'RSS', 30, TRUE)
ON CONFLICT (name) DO NOTHING;
