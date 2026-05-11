package io.nimbleway.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResponse(
    String answer,
    @JsonProperty("total_results") int totalResults,
    List<Result> results,
    @JsonProperty("request_id") String requestId
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Result(
      String title,
      String description,
      String url,
      String content,
      @JsonProperty("extra_fields") Map<String, Object> extraFields
  ) {

  }
}
