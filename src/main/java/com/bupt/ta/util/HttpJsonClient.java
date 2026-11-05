package com.bupt.ta.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** Minimal HTTPS JSON POST helper (JDK 11 {@link java.net.http.HttpClient}) — no extra libraries. */
public final class HttpJsonClient {
    private static final int MIN_STREAM_REQUEST_TIMEOUT_MS = 180_000;

    private HttpJsonClient() {}

    /**
     * POSTs JSON to {@code url} and returns the response body as a stream for SSE or chunked reads.
     *
     * <p>Sets {@code Accept: text/event-stream} and {@code Content-Type: application/json}.
     * The caller must close the returned stream. On non-2xx responses, throws {@link IOException}
     * with an HTTP status and a snippet of the error body (up to 4,000 characters).
     *
     * @param url       target HTTPS endpoint
     * @param headers   optional extra request headers; null keys or values are skipped
     * @param body      JSON request body; null is sent as empty string
     * @param timeoutMs connect timeout in milliseconds (minimum 1,000 ms enforced); the
     *                  streaming request gets at least 180,000 ms so a long response is not
     *                  cut off by the ordinary short JSON request timeout
     * @return response body input stream on success (2xx)
     * @throws IOException          on I/O failure or non-2xx HTTP status
     * @throws InterruptedException if the HTTP send is interrupted
     */
    public static InputStream postJsonStream(String url, Map<String, String> headers, String body, int timeoutMs)
            throws IOException, InterruptedException {
        int t = Math.max(1_000, timeoutMs);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(t))
                .build();
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(t, MIN_STREAM_REQUEST_TIMEOUT_MS)))
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8))
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json; charset=utf-8");
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null && v != null) {
                    b.header(k, v);
                }
            });
        }
        HttpResponse<InputStream> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofInputStream());
        int code = resp.statusCode();
        if (code < 200 || code >= 300) {
            String errBody = readStreamSnippet(resp.body(), 4_000);
            throw new IOException("HTTP " + code + ": " + errBody);
        }
        return resp.body();
    }

    private static String readStreamSnippet(InputStream in, int max) throws IOException {
        if (in == null) {
            return "";
        }
        byte[] buf = new byte[max];
        int n = in.read(buf);
        in.close();
        if (n <= 0) {
            return "";
        }
        return new String(buf, 0, n, StandardCharsets.UTF_8);
    }

    public static String postJson(String url, Map<String, String> headers, String body, int timeoutMs)
            throws IOException, InterruptedException {
        int t = Math.max(1_000, timeoutMs);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(t))
                .build();
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(t))
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json; charset=utf-8");
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null && v != null) {
                    b.header(k, v);
                }
            });
        }
        HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int code = resp.statusCode();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + ": " + truncate(resp.body(), 2_000));
        }
        return resp.body();
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }
}
