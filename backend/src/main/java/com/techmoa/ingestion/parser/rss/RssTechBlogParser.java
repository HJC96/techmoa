package com.techmoa.ingestion.parser.rss;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techmoa.ingestion.parser.ParsedPost;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.ingestion.parser.SourceProfile;
import com.techmoa.ingestion.parser.TechBlogParser;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RssTechBlogParser implements TechBlogParser {

    private static final Logger log = LoggerFactory.getLogger(RssTechBlogParser.class);

    @Override
    public boolean supports(ParserType parserType) {
        return ParserType.RSS == parserType;
    }

    @Override
    public List<ParsedPost> fetch(SourceProfile sourceProfile) {
        if (sourceProfile.feedUrl() == null || sourceProfile.feedUrl().isBlank()) {
            log.warn("RSS source has no feedUrl. sourceName={}", sourceProfile.sourceName());
            return Collections.emptyList();
        }

        try (XmlReader reader = new XmlReader(new URL(sourceProfile.feedUrl()))) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            return feed.getEntries().stream()
                    .map(this::toParsedPost)
                    .filter(post -> post.canonicalUrl() != null && !post.canonicalUrl().isBlank())
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse RSS feed. sourceName=" + sourceProfile.sourceName(),
                    e
            );
        }
    }

    private ParsedPost toParsedPost(SyndEntry entry) {
        return new ParsedPost(
                entry.getTitle(),
                entry.getLink(),
                entry.getDescription() == null ? null : entry.getDescription().getValue(),
                entry.getAuthor(),
                resolvePublishedAt(entry.getPublishedDate()),
                resolveTags(entry.getCategories())
        );
    }

    private LocalDateTime resolvePublishedAt(Date publishedDate) {
        if (publishedDate == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(publishedDate.toInstant(), ZoneId.systemDefault());
    }

    private List<String> resolveTags(List<SyndCategory> categories) {
        if (categories == null) {
            return Collections.emptyList();
        }
        return categories.stream()
                .map(SyndCategory::getName)
                .filter(Objects::nonNull)
                .toList();
    }
}
