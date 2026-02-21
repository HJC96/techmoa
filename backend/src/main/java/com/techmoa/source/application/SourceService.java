package com.techmoa.source.application;

import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.source.domain.Source;
import com.techmoa.source.domain.SourceRepository;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceService {

    private final SourceRepository sourceRepository;

    public SourceService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public List<Source> findActiveSources() {
        return sourceRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(Source::getName))
                .toList();
    }

    @Transactional
    public Source createSource(
            String name,
            String baseUrl,
            String feedUrl,
            ParserType parserType,
            Integer intervalMin,
            Boolean active
    ) {
        validateUrl(baseUrl, "baseUrl");
        if (parserType == ParserType.RSS) {
            if (feedUrl == null || feedUrl.isBlank()) {
                throw new IllegalArgumentException("feedUrl is required for RSS parser");
            }
            validateUrl(feedUrl, "feedUrl");
        } else if (feedUrl != null && !feedUrl.isBlank()) {
            validateUrl(feedUrl, "feedUrl");
        }

        Source source = new Source(
                name,
                baseUrl,
                feedUrl,
                parserType,
                intervalMin,
                active
        );
        return sourceRepository.save(source);
    }

    private void validateUrl(String value, String fieldName) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException(fieldName + " must start with http or https");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException(fieldName + " must include a host");
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith(fieldName)) {
                throw e;
            }
            throw new IllegalArgumentException(fieldName + " is not a valid URL");
        }
    }
}
