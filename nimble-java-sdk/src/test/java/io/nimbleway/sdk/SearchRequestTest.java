package io.nimbleway.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchRequestTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void of_setsQuery() {
        SearchRequest req = SearchRequest.of("hello");
        assertEquals("hello", req.query());
    }

    @Test
    void build_throwsWhenQueryNull() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SearchRequest.builder().build());
        assertTrue(ex.getMessage().contains("query"));
    }

    @Test
    void build_throwsWhenQueryBlank() {
        assertThrows(IllegalStateException.class,
                () -> SearchRequest.builder().query("   ").build());
    }

    @Test
    void build_succeedsWithQuery() {
        SearchRequest req = SearchRequest.builder().query("java").build();
        assertEquals("java", req.query());
    }

    @Test
    void enumFocus_serialisesToLowercase() throws Exception {
        SearchRequest req = SearchRequest.builder()
                .query("test")
                .focus(SearchRequest.Focus.NEWS)
                .build();
        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"news\""), "focus should serialise as 'news', got: " + json);
    }

    @Test
    void enumDepth_serialisesToLowercase() throws Exception {
        SearchRequest req = SearchRequest.builder()
                .query("test")
                .searchDepth(SearchRequest.Depth.DEEP)
                .build();
        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"deep\""), "depth should serialise as 'deep', got: " + json);
    }

    @Test
    void enumTimeRange_serialisesToLowercase() throws Exception {
        SearchRequest req = SearchRequest.builder()
                .query("test")
                .timeRange(SearchRequest.TimeRange.WEEK)
                .build();
        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"week\""), "timeRange should serialise as 'week', got: " + json);
    }

    @Test
    void localDate_serialisesToIso8601() throws Exception {
        SearchRequest req = SearchRequest.builder()
                .query("test")
                .startDate(LocalDate.of(2024, 1, 15))
                .endDate(LocalDate.of(2024, 3, 31))
                .build();
        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"2024-01-15\""), "startDate should be ISO-8601, got: " + json);
        assertTrue(json.contains("\"2024-03-31\""), "endDate should be ISO-8601, got: " + json);
    }

    @Test
    void nullFields_omittedFromJson() throws Exception {
        SearchRequest req = SearchRequest.of("minimal");
        String json = mapper.writeValueAsString(req);
        assertFalse(json.contains("focus"), "null focus should be omitted");
        assertFalse(json.contains("start_date"), "null startDate should be omitted");
    }

    @Test
    void builder_fullRequest() {
        SearchRequest req = SearchRequest.builder()
                .query("AI news")
                .maxResults(10)
                .focus(SearchRequest.Focus.GENERAL)
                .searchDepth(SearchRequest.Depth.FAST)
                .timeRange(SearchRequest.TimeRange.DAY)
                .includeAnswer(true)
                .includeDomains(List.of("example.com"))
                .excludeDomains(List.of("spam.com"))
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .country("US")
                .locale("en-US")
                .build();

        assertEquals("AI news", req.query());
        assertEquals(10, req.maxResults());
        assertEquals(SearchRequest.Focus.GENERAL, req.focus());
        assertEquals(SearchRequest.Depth.FAST, req.searchDepth());
        assertEquals(SearchRequest.TimeRange.DAY, req.timeRange());
        assertTrue(req.includeAnswer());
        assertEquals(List.of("example.com"), req.includeDomains());
        assertEquals(List.of("spam.com"), req.excludeDomains());
        assertEquals(LocalDate.of(2024, 1, 1), req.startDate());
        assertEquals(LocalDate.of(2024, 12, 31), req.endDate());
        assertEquals("US", req.country());
        assertEquals("en-US", req.locale());
    }
}
