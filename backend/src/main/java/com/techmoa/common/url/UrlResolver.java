package com.techmoa.common.url;

import java.net.URI;
import java.util.Locale;

public final class UrlResolver {

    private UrlResolver() {
    }

    public static String resolveAbsoluteUrl(String rawUrl, String... baseUrls) {
        String url = normalize(rawUrl);
        if (url == null) {
            return null;
        }

        if (isHttpOrHttpsAbsolute(url)) {
            return url;
        }

        if (url.startsWith("//")) {
            String scheme = resolveBaseScheme(baseUrls);
            return scheme + ":" + url;
        }

        for (String rawBaseUrl : baseUrls) {
            String baseUrl = normalize(rawBaseUrl);
            if (baseUrl == null || !isHttpOrHttpsAbsolute(baseUrl)) {
                continue;
            }
            try {
                return URI.create(baseUrl).resolve(url).toString();
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private static boolean isHttpOrHttpsAbsolute(String url) {
        try {
            URI uri = URI.create(url);
            if (!uri.isAbsolute()) {
                return false;
            }
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    private static String resolveBaseScheme(String... baseUrls) {
        if (baseUrls == null) {
            return "https";
        }

        for (String rawBaseUrl : baseUrls) {
            String baseUrl = normalize(rawBaseUrl);
            if (baseUrl == null) {
                continue;
            }

            try {
                URI uri = URI.create(baseUrl);
                String scheme = uri.getScheme();
                if (scheme == null) {
                    continue;
                }
                String lowerScheme = scheme.toLowerCase(Locale.ROOT);
                if ("http".equals(lowerScheme) || "https".equals(lowerScheme)) {
                    return lowerScheme;
                }
            } catch (Exception ignored) {
            }
        }

        return "https";
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
