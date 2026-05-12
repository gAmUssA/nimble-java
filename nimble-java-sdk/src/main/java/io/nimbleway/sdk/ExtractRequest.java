package io.nimbleway.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExtractRequest(
    String url,
    Boolean render,
    Boolean parse,
    Map<String, Object> parser,
    List<Format> formats,
    String country,
    String locale
) {

  public enum Format {
    @JsonProperty("html")       HTML,
    @JsonProperty("markdown")   MARKDOWN,
    @JsonProperty("screenshot") SCREENSHOT,
    @JsonProperty("headers")    HEADERS,
    @JsonProperty("links")      LINKS
  }

  public static ExtractRequest of(String url) {
    return builder().url(url).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String url;
    private Boolean render;
    private Boolean parse;
    private Map<String, Object> parser;
    private List<Format> formats;
    private String country;
    private String locale;

    public Builder url(String v) {
      this.url = v;
      return this;
    }

    public Builder render(boolean v) {
      this.render = v;
      return this;
    }

    public Builder parse(boolean v) {
      this.parse = v;
      return this;
    }

    public Builder parser(Map<String, Object> v) {
      this.parser = v;
      return this;
    }

    public Builder formats(List<Format> v) {
      this.formats = v;
      return this;
    }

    public Builder formats(Format... v) {
      this.formats = List.of(v);
      return this;
    }

    public Builder country(String v) {
      this.country = v;
      return this;
    }

    public Builder locale(String v) {
      this.locale = v;
      return this;
    }

    public ExtractRequest build() {
      if (url == null || url.isBlank()) {
        throw new IllegalStateException("url is required");
      }
      return new ExtractRequest(url, render, parse, parser, formats, country, locale);
    }
  }
}
