package io.nimbleway.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class NimbleClientTest {

    // Minimal stub HttpClient that returns a pre-canned response body and status code
    @SuppressWarnings("unchecked")
    private HttpClient stubHttpClient(int status, String body) {
        return new HttpClient() {
            @Override
            public <T> HttpResponse<T> send(HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler) {
                return stubResponse(status, body);
            }

            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler) {
                return CompletableFuture.completedFuture(stubResponse(status, body));
            }

            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler,
                    HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                return CompletableFuture.completedFuture(stubResponse(status, body));
            }

            @Override
            public java.util.Optional<java.net.CookieHandler> cookieHandler() { return java.util.Optional.empty(); }
            @Override
            public java.util.Optional<java.time.Duration> connectTimeout() { return java.util.Optional.empty(); }
            @Override
            public java.net.http.HttpClient.Redirect followRedirects() { return Redirect.NORMAL; }
            @Override
            public java.util.Optional<java.net.ProxySelector> proxy() { return java.util.Optional.empty(); }
            @Override
            public javax.net.ssl.SSLContext sslContext() { return null; }
            @Override
            public javax.net.ssl.SSLParameters sslParameters() { return null; }
            @Override
            public java.util.Optional<java.net.Authenticator> authenticator() { return java.util.Optional.empty(); }
            @Override
            public java.net.http.HttpClient.Version version() { return Version.HTTP_1_1; }
            @Override
            public java.util.Optional<java.util.concurrent.Executor> executor() { return java.util.Optional.empty(); }

            @SuppressWarnings("unchecked")
            private <T> HttpResponse<T> stubResponse(int statusCode, String responseBody) {
                return (HttpResponse<T>) new HttpResponse<String>() {
                    @Override public int statusCode() { return statusCode; }
                    @Override public String body() { return responseBody; }
                    @Override public HttpRequest request() { return null; }
                    @Override public java.util.Optional<HttpResponse<String>> previousResponse() { return java.util.Optional.empty(); }
                    @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a, b) -> true); }
                    @Override public java.net.URI uri() { return null; }
                    @Override public java.net.http.HttpClient.Version version() { return Version.HTTP_1_1; }
                    @Override public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return java.util.Optional.empty(); }
                };
            }
        };
    }

    private NimbleClient clientWith(int status, String body) {
        return NimbleClient.builder()
                .apiKey("test-key")
                .baseUrl("https://stub.local")
                .httpClient(stubHttpClient(status, body))
                .objectMapper(new ObjectMapper().registerModule(new JavaTimeModule()))
                .build();
    }

    // ── Builder validation ────────────────────────────────────────────────────

    @Test
    void builder_throwsWhenApiKeyMissing() {
        assertThrows(IllegalStateException.class, () -> NimbleClient.builder().build());
    }

    @Test
    void builder_throwsWhenApiKeyBlank() {
        assertThrows(IllegalStateException.class,
                () -> NimbleClient.builder().apiKey("  ").build());
    }

    // ── Closeable ─────────────────────────────────────────────────────────────

    @Test
    void nimbleClient_implementsCloseable() {
        assertTrue(Closeable.class.isAssignableFrom(NimbleClient.class));
    }

    @Test
    void close_doesNotThrow() {
        NimbleClient client = NimbleClient.builder()
                .apiKey("key")
                .build();
        assertDoesNotThrow(client::close);
    }

    // ── Successful search ─────────────────────────────────────────────────────

    @Test
    void search_parsesSuccessResponse() {
        String responseJson = """
                {"answer":"42","total_results":1,"results":[
                  {"title":"T","description":"D","url":"https://x.com","content":"C"}
                ],"request_id":"r1"}
                """;
        NimbleClient client = clientWith(200, responseJson);
        SearchResponse resp = client.search(SearchRequest.of("test"));
        assertEquals("42", resp.answer());
        assertEquals(1, resp.totalResults());
        assertEquals("r1", resp.requestId());
        assertEquals("T", resp.results().get(0).title());
    }

    // ── Successful extract ────────────────────────────────────────────────────

    @Test
    void extract_parsesSuccessResponse() {
        String responseJson = """
                {"url":"https://x.com","task_id":"t1","status":"success",
                 "status_code":200,"data":{"html":"<h1>Hi</h1>","markdown":"# Hi"}}
                """;
        NimbleClient client = clientWith(200, responseJson);
        ExtractResponse resp = client.extract(ExtractRequest.of("https://x.com"));
        assertEquals("https://x.com", resp.url());
        assertEquals("t1", resp.taskId());
        assertEquals("<h1>Hi</h1>", resp.data().html());
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test
    void search_throwsNimbleExceptionOn4xx() {
        NimbleClient client = clientWith(401, "{\"error\":\"Unauthorized\"}");
        NimbleException ex = assertThrows(NimbleException.class,
                () -> client.search(SearchRequest.of("test")));
        assertEquals(401, ex.statusCode());
        assertTrue(ex.responseBody().contains("Unauthorized"));
    }

    @Test
    void search_throwsNimbleExceptionOn5xx() {
        NimbleClient client = clientWith(500, "Internal Server Error");
        NimbleException ex = assertThrows(NimbleException.class,
                () -> client.search(SearchRequest.of("test")));
        assertEquals(500, ex.statusCode());
    }

    // ── Async API ─────────────────────────────────────────────────────────────

    @Test
    void searchAsync_returnsCompletableFuture() {
        String responseJson = """
                {"answer":null,"total_results":0,"results":[],"request_id":"async1"}
                """;
        NimbleClient client = clientWith(200, responseJson);
        CompletableFuture<SearchResponse> future = client.searchAsync(SearchRequest.of("async"));
        assertNotNull(future);
        SearchResponse resp = future.join();
        assertEquals("async1", resp.requestId());
    }

    @Test
    void extractAsync_returnsCompletableFuture() {
        String responseJson = """
                {"url":"https://x.com","task_id":"a2","status":"success",
                 "status_code":200,"data":{"markdown":"# Hello"}}
                """;
        NimbleClient client = clientWith(200, responseJson);
        CompletableFuture<ExtractResponse> future =
                client.extractAsync(ExtractRequest.of("https://x.com"));
        assertNotNull(future);
        ExtractResponse resp = future.join();
        assertEquals("a2", resp.taskId());
    }

    @Test
    void searchAsync_failsWithNimbleExceptionOn4xx() {
        NimbleClient client = clientWith(403, "{\"error\":\"Forbidden\"}");
        CompletableFuture<SearchResponse> future = client.searchAsync(SearchRequest.of("test"));
        Exception ex = assertThrows(Exception.class, future::join);
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        assertInstanceOf(NimbleException.class, cause);
        assertEquals(403, ((NimbleException) cause).statusCode());
    }
}
