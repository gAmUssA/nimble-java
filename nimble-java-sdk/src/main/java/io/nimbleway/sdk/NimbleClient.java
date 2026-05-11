package io.nimbleway.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public final class NimbleClient {

  private static final String DEFAULT_BASE_URL = "https://sdk.nimbleway.com/v1";

  private final String baseUrl;
  private final String apiKey;
  private final HttpClient http;
  private final ObjectMapper json;
  private final Duration requestTimeout;

  private NimbleClient(Builder b) {
    this.baseUrl = b.baseUrl;
    this.apiKey = b.apiKey;
    this.requestTimeout = b.requestTimeout;
    this.http = b.httpClient != null ? b.httpClient
                                     : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    this.json = b.objectMapper != null ? b.objectMapper
                                       : new ObjectMapper().registerModule(new JavaTimeModule());
  }

  public SearchResponse search(SearchRequest req) {
    return post("/search", req, SearchResponse.class);
  }

  public ExtractResponse extract(ExtractRequest req) {
    return post("/extract", req, ExtractResponse.class);
  }

  private <T> T post(String path, Object body, Class<T> responseType) {
    try {
      String payload = json.writeValueAsString(body);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + path))
          .timeout(requestTimeout)
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .POST(BodyPublishers.ofString(payload))
          .build();
      HttpResponse<String> response = http.send(request, BodyHandlers.ofString());
      int code = response.statusCode();
      if (code >= 200 && code < 300) {
        return json.readValue(response.body(), responseType);
      }
      String errBody = response.body();
      String snippet = errBody == null ? "(empty)"
                                       : (errBody.length() > 500 ? errBody.substring(0, 500) + "…" : errBody);
      throw new NimbleException(code, errBody,
                                "Nimble API error %d on POST %s — body: %s".formatted(code, path, snippet));
    } catch (NimbleException e) {
      throw e;
    } catch (Exception e) {
      throw new NimbleException("Nimble API call failed: " + path, e);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String apiKey;
    private String baseUrl = DEFAULT_BASE_URL;
    private Duration requestTimeout = Duration.ofSeconds(60);
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    public Builder apiKey(String v) {
      this.apiKey = v;
      return this;
    }

    public Builder baseUrl(String v) {
      this.baseUrl = v;
      return this;
    }

    public Builder requestTimeout(Duration v) {
      this.requestTimeout = v;
      return this;
    }

    public Builder httpClient(HttpClient v) {
      this.httpClient = v;
      return this;
    }

    public Builder objectMapper(ObjectMapper v) {
      this.objectMapper = v;
      return this;
    }

    public NimbleClient build() {
      if (apiKey == null || apiKey.isBlank()) {
        throw new IllegalStateException("apiKey is required");
      }
      return new NimbleClient(this);
    }
  }
}
