package io.nimbleway.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchRequest(
    String query,
    @JsonProperty("max_results") Integer maxResults,
    Focus focus,
    @JsonProperty("search_depth") Depth searchDepth,
    @JsonProperty("time_range") TimeRange timeRange,
    @JsonProperty("include_answer") Boolean includeAnswer,
    @JsonProperty("include_domains") List<String> includeDomains,
    @JsonProperty("exclude_domains") List<String> excludeDomains,
    @JsonProperty("start_date") LocalDate startDate,
    @JsonProperty("end_date") LocalDate endDate,
    String country,
    String locale
) {

  public enum Focus {
    @JsonProperty("general")  GENERAL,
    @JsonProperty("news")     NEWS,
    @JsonProperty("location") LOCATION,
    @JsonProperty("coding")   CODING,
    @JsonProperty("geo")      GEO,
    @JsonProperty("shopping") SHOPPING,
    @JsonProperty("social")   SOCIAL,
    @JsonProperty("academic") ACADEMIC
  }

  public enum Depth {
    @JsonProperty("lite") LITE,
    @JsonProperty("fast") FAST,
    @JsonProperty("deep") DEEP
  }

  public enum TimeRange {
    @JsonProperty("hour")  HOUR,
    @JsonProperty("day")   DAY,
    @JsonProperty("week")  WEEK,
    @JsonProperty("month") MONTH,
    @JsonProperty("year")  YEAR
  }

  public static SearchRequest of(String query) {
    return builder().query(query).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String query;
    private Integer maxResults;
    private Focus focus;
    private Depth searchDepth;
    private TimeRange timeRange;
    private Boolean includeAnswer;
    private List<String> includeDomains;
    private List<String> excludeDomains;
    private LocalDate startDate;
    private LocalDate endDate;
    private String country;
    private String locale;

    public Builder query(String v) {
      this.query = v;
      return this;
    }

    public Builder maxResults(Integer v) {
      this.maxResults = v;
      return this;
    }

    public Builder focus(Focus v) {
      this.focus = v;
      return this;
    }

    public Builder searchDepth(Depth v) {
      this.searchDepth = v;
      return this;
    }

    public Builder timeRange(TimeRange v) {
      this.timeRange = v;
      return this;
    }

    public Builder includeAnswer(Boolean v) {
      this.includeAnswer = v;
      return this;
    }

    public Builder includeDomains(List<String> v) {
      this.includeDomains = v;
      return this;
    }

    public Builder excludeDomains(List<String> v) {
      this.excludeDomains = v;
      return this;
    }

    public Builder startDate(LocalDate v) {
      this.startDate = v;
      return this;
    }

    public Builder endDate(LocalDate v) {
      this.endDate = v;
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

    public SearchRequest build() {
      if (query == null || query.isBlank()) {
        throw new IllegalStateException("query is required");
      }
      return new SearchRequest(query, maxResults, focus, searchDepth, timeRange,
                               includeAnswer, includeDomains, excludeDomains, startDate, endDate, country, locale);
    }
  }
}
