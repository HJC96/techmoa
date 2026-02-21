package com.techmoa.source.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.source.domain.Source;
import com.techmoa.source.domain.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SourceServiceTest {

    @Mock
    private SourceRepository sourceRepository;

    private SourceService sourceService;

    @BeforeEach
    void setUp() {
        sourceService = new SourceService(sourceRepository);
    }

    @Test
    void createSource_throwsWhenRssFeedUrlMissing() {
        assertThatThrownBy(() -> sourceService.createSource(
                "카카오테크",
                "https://tech.kakao.com",
                null,
                ParserType.RSS,
                30,
                true
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("feedUrl is required");
    }

    @Test
    void createSource_throwsWhenBaseUrlInvalid() {
        assertThatThrownBy(() -> sourceService.createSource(
                "카카오테크",
                "not-a-url",
                "https://tech.kakao.com/feed",
                ParserType.RSS,
                30,
                true
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl");
    }

    @Test
    void createSource_savesWhenUrlsAreValid() {
        when(sourceRepository.save(any(Source.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sourceService.createSource(
                "카카오테크",
                "https://tech.kakao.com",
                "https://tech.kakao.com/feed",
                ParserType.RSS,
                30,
                true
        );

        verify(sourceRepository).save(any(Source.class));
    }
}
