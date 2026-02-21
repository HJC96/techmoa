package com.techmoa.common.url;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlResolverTest {

    @Test
    void resolveAbsoluteUrl_returnsAbsoluteAsIs() {
        String resolved = UrlResolver.resolveAbsoluteUrl(
                "https://images.example.com/a.png",
                "https://example.com"
        );

        assertThat(resolved).isEqualTo("https://images.example.com/a.png");
    }

    @Test
    void resolveAbsoluteUrl_resolvesRootRelativePath() {
        String resolved = UrlResolver.resolveAbsoluteUrl(
                "/content/images/a.png",
                "https://d2.naver.com/helloworld/1234"
        );

        assertThat(resolved).isEqualTo("https://d2.naver.com/content/images/a.png");
    }

    @Test
    void resolveAbsoluteUrl_resolvesProtocolRelativePath() {
        String resolved = UrlResolver.resolveAbsoluteUrl(
                "//static.example.com/a.png",
                "https://example.com/post/1"
        );

        assertThat(resolved).isEqualTo("https://static.example.com/a.png");
    }

    @Test
    void resolveAbsoluteUrl_returnsNullWhenCannotResolve() {
        String resolved = UrlResolver.resolveAbsoluteUrl("/images/a.png");

        assertThat(resolved).isNull();
    }
}
