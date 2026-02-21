INSERT INTO sources (name, base_url, feed_url, parser_type, interval_min, active)
VALUES ('네이버테크', 'https://d2.naver.com', 'https://d2.naver.com/d2.atom', 'RSS', 30, TRUE)
ON CONFLICT (name) DO NOTHING;
