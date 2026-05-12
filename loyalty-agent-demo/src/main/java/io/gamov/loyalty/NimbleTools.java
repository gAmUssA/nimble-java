package io.gamov.loyalty;

import org.jboss.logging.Logger;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.nimbleway.sdk.ExtractRequest;
import io.nimbleway.sdk.NimbleClient;
import io.nimbleway.sdk.SearchRequest;
import io.nimbleway.sdk.SearchResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NimbleTools {

  private static final Logger LOG = Logger.getLogger(NimbleTools.class);

  @Inject
  NimbleClient nimble;

  @Inject
  AgentEventBus events;

  @Tool("""
      Search the live web for current information. Use this whenever the user asks about facts \
      that may have changed recently (airline loyalty programs, award charts, transfer ratios, \
      devaluation announcements, fare-class earnings rates, etc.). The LLM's training data is \
      stale — this tool sees the live web. Returns a list of {title, url, snippet} results. \
      Snippets are short. Call extractUrl on the most relevant result to get full page content.
      """)
  public String webSearch(
      @P("The search query in plain English (e.g., 'United MileagePlus Polaris business earning rate 2026')") String query,
      @P("How recent results should be: 'day', 'week', 'month', 'year', or null for any time. Use 'month' or 'week' for current loyalty program rules.") String recency) {

    SearchRequest.TimeRange tr = parseRecency(recency);
    LOG.infof("→ nimble.search(query=%s, time_range=%s)", query, tr);
    events.emit("[tool] → nimble.search query=\"" + query + "\""
                + (tr == null ? "" : " time_range=" + tr));
    long t0 = System.currentTimeMillis();
    SearchRequest req = SearchRequest.builder()
        .query(query)
        .maxResults(5)
        .focus(SearchRequest.Focus.GENERAL)
        .searchDepth(SearchRequest.Depth.LITE)
        .timeRange(tr)
        .build();

    SearchResponse res = nimble.search(req);
    long elapsed = System.currentTimeMillis() - t0;
    int count = res.results() == null ? 0 : res.results().size();
    LOG.infof("← nimble.search %d results in %dms", count, elapsed);
    events.emit("[tool] ← " + count + " results in " + elapsed + "ms");
    if (res.results() == null || res.results().isEmpty()) {
      return "No results found for: " + query;
    }
    StringBuilder out = new StringBuilder();
    out.append("Found ").append(res.totalResults()).append(" results for: ").append(query).append("\n\n");
    for (int i = 0; i < res.results().size(); i++) {
      SearchResponse.Result r = res.results().get(i);
      out.append("[").append(i + 1).append("] ").append(r.title()).append("\n");
      out.append("    URL: ").append(r.url()).append("\n");
      if (r.description() != null) {
        out.append("    Snippet: ").append(r.description()).append("\n");
      }
      if (r.content() != null && !r.content().isBlank()) {
        String snippet = r.content().length() > 800
                         ? r.content().substring(0, 800) + "…[truncated]"
                         : r.content();
        out.append("    Content: ").append(snippet).append("\n");
      }
      out.append("\n");
    }
    return out.toString();
  }

  @Tool("""
      Fetch the full contents of a specific web page as clean markdown. Use this when you have \
      a URL (typically from webSearch) and need the full text — e.g., to compare a devaluation \
      post against current charts, or to extract a table that didn't fit in a search snippet. \
      Returns the page rendered as markdown after JavaScript execution. \
      \
      If the response says "0 chars markdown", the page is gated by anti-bot. \
      Pick a different URL from the search results and try once more — DO NOT retry the same URL.
      """)
  public String extractUrl(
      @P("The fully-qualified URL to extract") String url) {

    LOG.infof("→ nimble.extract(url=%s)", url);
    events.emit("[tool] → nimble.extract " + url);
    long t0 = System.currentTimeMillis();
    ExtractRequest req = ExtractRequest.builder()
        .url(url)
        .render(true)
        .formats(ExtractRequest.Format.MARKDOWN)
        .build();

    var res = nimble.extract(req);
    long elapsed = System.currentTimeMillis() - t0;
    int len = res.data() != null && res.data().markdown() != null
              ? res.data().markdown().length() : 0;
    LOG.infof("← nimble.extract status=%s, %d chars markdown in %dms",
              res.status(), len, elapsed);
    events.emit("[tool] ← " + res.status() + " " + len + " chars in " + elapsed + "ms");
    String md = res.data() != null ? res.data().markdown() : null;
    if (md == null || md.isBlank()) {
      return "[Extracted page from %s but no markdown content was returned. status=%s]"
          .formatted(url, res.status());
    }
    // Cap returned markdown so we don't blow the context window.
    if (md.length() > 12_000) {
      md = md.substring(0, 12_000) + "\n\n…[truncated to 12k chars]";
    }
    return "Page: " + url + "\n\n" + md;
  }

  private static SearchRequest.TimeRange parseRecency(String s) {
      if (s == null || s.isBlank() || s.equalsIgnoreCase("all") || s.equalsIgnoreCase("null")) {
          return null;
      }
    try {
      return SearchRequest.TimeRange.valueOf(s.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
