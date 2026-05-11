package io.nimbleway.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractResponse(
    String url,
    @JsonProperty("task_id") String taskId,
    String status,
    @JsonProperty("status_code") int statusCode,
    Data data,
    Map<String, Object> metadata,
    List<String> warnings
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(
      String html,
      String markdown,
      Map<String, Object> parsing,
      Map<String, Object> headers,
      Map<String, Object> cookies
  ) {

  }
}
