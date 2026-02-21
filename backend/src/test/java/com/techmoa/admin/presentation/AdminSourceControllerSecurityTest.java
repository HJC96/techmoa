package com.techmoa.admin.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techmoa.common.config.SecurityConfig;
import com.techmoa.ingestion.application.SourceSyncService;
import com.techmoa.ingestion.application.SyncResult;
import com.techmoa.ingestion.parser.ParserType;
import com.techmoa.source.application.SourceService;
import com.techmoa.source.domain.Source;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(AdminSourceController.class)
@Import(SecurityConfig.class)
class AdminSourceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SourceService sourceService;

    @MockitoBean
    private SourceSyncService sourceSyncService;

    @Test
    void createSource_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"카카오테크",
                                  "baseUrl":"https://tech.kakao.com",
                                  "feedUrl":"https://tech.kakao.com/feed",
                                  "parserType":"RSS",
                                  "intervalMin":30,
                                  "active":true
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createSource_acceptsAuthenticatedAdmin() throws Exception {
        when(sourceService.createSource(
                anyString(),
                anyString(),
                anyString(),
                eq(ParserType.RSS),
                anyInt(),
                anyBoolean()
        )).thenReturn(new Source(
                "카카오테크",
                "https://tech.kakao.com",
                "https://tech.kakao.com/feed",
                ParserType.RSS,
                30,
                true
        ));

        mockMvc.perform(post("/api/admin/sources")
                        .with(httpBasic("admin", "admin1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"카카오테크",
                                  "baseUrl":"https://tech.kakao.com",
                                  "feedUrl":"https://tech.kakao.com/feed",
                                  "parserType":"RSS",
                                  "intervalMin":30,
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void backfill_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/sources/1/backfill"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void backfill_acceptsAuthenticatedAdmin() throws Exception {
        when(sourceSyncService.backfillSourceById(eq(1L), anyString()))
                .thenReturn(new SyncResult("카카오테크", 120, 118));

        mockMvc.perform(post("/api/admin/sources/1/backfill")
                        .with(httpBasic("admin", "admin1234"))
                        .queryParam("sitemapUrl", "https://tech.kakao.com/sitemap.xml"))
                .andExpect(status().isOk());
    }
}
