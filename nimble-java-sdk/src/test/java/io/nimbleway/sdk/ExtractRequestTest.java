package io.nimbleway.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractRequestTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void of_setsUrl() {
        ExtractRequest req = ExtractRequest.of("https://example.com");
        assertEquals("https://example.com", req.url());
    }

    @Test
    void build_throwsWhenUrlNull() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ExtractRequest.builder().build());
        assertTrue(ex.getMessage().contains("url"));
    }

    @Test
    void build_throwsWhenUrlBlank() {
        assertThrows(IllegalStateException.class,
                () -> ExtractRequest.builder().url("  ").build());
    }

    @Test
    void build_succeedsWithUrl() {
        ExtractRequest req = ExtractRequest.builder().url("https://example.com").build();
        assertEquals("https://example.com", req.url());
    }

    @Test
    void enumFormat_serialisesToLowercase() throws Exception {
        ExtractRequest req = ExtractRequest.builder()
                .url("https://example.com")
                .formats(ExtractRequest.Format.MARKDOWN, ExtractRequest.Format.HTML)
                .build();
        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"markdown\""), "MARKDOWN should serialise as 'markdown', got: " + json);
        assertTrue(json.contains("\"html\""), "HTML should serialise as 'html', got: " + json);
    }

    @Test
    void parser_acceptsMapOfStringObject() {
        Map<String, Object> schema = Map.of("fields", List.of("title", "price"));
        ExtractRequest req = ExtractRequest.builder()
                .url("https://example.com")
                .parser(schema)
                .build();
        assertEquals(schema, req.parser());
    }

    @Test
    void parser_serialisedAsJsonObject() throws Exception {
        Map<String, Object> schema = Map.of("type", "product");
        ExtractRequest req = ExtractRequest.builder()
                .url("https://example.com")
                .parser(schema)
                .build();
        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"parser\""), "parser field should be present");
        assertTrue(json.contains("\"product\""), "parser content should be serialised");
    }

    @Test
    void nullFields_omittedFromJson() throws Exception {
        ExtractRequest req = ExtractRequest.of("https://example.com");
        String json = mapper.writeValueAsString(req);
        assertFalse(json.contains("render"), "null render should be omitted");
        assertFalse(json.contains("parser"), "null parser should be omitted");
    }

    @Test
    void builder_fullRequest() {
        Map<String, Object> schema = Map.of("fields", List.of("title"));
        ExtractRequest req = ExtractRequest.builder()
                .url("https://example.com/page")
                .render(true)
                .parse(true)
                .parser(schema)
                .formats(List.of(ExtractRequest.Format.HTML, ExtractRequest.Format.LINKS))
                .country("US")
                .locale("en-US")
                .build();

        assertEquals("https://example.com/page", req.url());
        assertTrue(req.render());
        assertTrue(req.parse());
        assertEquals(schema, req.parser());
        assertEquals(List.of(ExtractRequest.Format.HTML, ExtractRequest.Format.LINKS), req.formats());
        assertEquals("US", req.country());
        assertEquals("en-US", req.locale());
    }
}
