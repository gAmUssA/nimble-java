# nimble-java

A Java SDK for [Nimble](https://nimbleway.com/)'s web-data API, plus a Quarkus + LangChain4j
agent demo that uses it to answer airline-loyalty questions against the live web.

## What's in this repo

```
nimble-java/
├── nimble-java-sdk/       Type-safe Java client for Nimble's REST API.
│                          Records over /search and /extract. JDK 21+.
└── loyalty-agent-demo/    Quarkus app: a LangChain4j agent that uses the
                           SDK as live-web tools and answers loyalty questions
                           via Claude Sonnet. Streams tool calls and the final
                           answer over Server-Sent Events to a minimal web UI.
```

## Why a Java SDK?

Nimble ships official SDKs for Python, Node, and Go. There's no Java SDK.
Most enterprise Java shops won't reach for an agent stack unless the building
blocks already speak their language. This repo is that building block.

## Prerequisites

- **JDK 21** (LTS). Verify: `java --version`.
- **Maven 3.9+**. Verify: `mvn --version`.
- **Nimble API key** — get one at <https://app.nimbleway.com/> → Account Settings → API Keys.
- **Anthropic API key** — get one at <https://console.anthropic.com/>.

## Quick start

```bash
# 1. Set keys (use the .env at the take-home root, or export inline)
export NIMBLE_API_KEY=...
export ANTHROPIC_API_KEY=...

# 2. Build
mvn -DskipTests package

# 3. Run the agent in dev mode
mvn -pl loyalty-agent-demo quarkus:dev

# 4. Open the UI
open http://localhost:8080
```

Click one of the three pre-canned queries or type your own. Tool calls and the
final answer stream into the page. Watch the Quarkus terminal for verbose
LangChain4j tool-call traces (Claude → Nimble → Claude → answer).

### Loading from .env

If your secrets live in `.env` (e.g. at the take-home root):

```bash
set -a; source ../.env; set +a
mvn -pl loyalty-agent-demo quarkus:dev
```

## The three demo queries

| # | Type                        | Query                                                                           | What it shows                                                |
|---|-----------------------------|---------------------------------------------------------------------------------|--------------------------------------------------------------|
| 1 | Fact that recently changed  | *Current MileagePlus earning rate on Polaris business?*                         | LLM training prior is wrong. Search → extract → fresh number with citation. ~23s, 3 tool calls. |
| 2 | Multi-source reasoning      | *120k miles — transfer to Avianca LifeMiles or fly United direct to Tokyo in March?* | Parallel searches across charts + transfer ratios. Markdown comparison table. ~32s, 4 tool calls. |
| 3 | Schema-first extraction     | *Find a recent OMAAT devaluation post and show the diff as a markdown table.*   | Long-form post in, structured table out. ~42s, 4 tool calls. *Heads-up:* United eliminated fixed award charts in 2019, so the agent will return a policy-change table, not a numeric chart diff. For a cleaner numeric diff, target Hyatt/Hilton/Aeroplan-partner devaluations. |

**Account tier note:** This demo uses `search_depth=lite` (titles + URLs + snippets) because
that's what the standard Nimble account tier ships. `fast` and `deep` require sales contact.
The lite tier actually produces a *better* visible agent loop on stage — search returns
URLs and the agent then calls extractUrl for full content, so the panel sees both tool
types fire per query.

## Tool-call budget

The system prompt caps the agent at four sequential tool calls per question. 
Without that ceiling, Claude tends to perfectionism-search past 10 calls and hit LangChain4j's default
`maxSequentialToolsInvocations` limit. 
Four is enough for: one search → one extract → one verification search → one retry-on-anti-bot extract.

## SDK usage

The SDK is a plain Java library — no Quarkus, no LangChain4j dependency. Drop it
into any Java 21+ project:

```xml
<dependency>
    <groupId>io.nimbleway</groupId>
    <artifactId>nimble-java-sdk</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
NimbleClient nimble = NimbleClient.builder()
        .apiKey(System.getenv("NIMBLE_API_KEY"))
        .build();

// Search
SearchResponse hits = nimble.search(SearchRequest.builder()
        .query("United MileagePlus Polaris earning rate 2026")
        .maxResults(5)
        .timeRange(SearchRequest.TimeRange.month)
        .searchDepth(SearchRequest.Depth.fast)
        .build());

// Extract
ExtractResponse page = nimble.extract(ExtractRequest.builder()
        .url(hits.results().get(0).url())
        .render(true)
        .formats(ExtractRequest.Format.markdown)
        .build());

System.out.println(page.data().markdown());
```

## How the agent uses the SDK

`loyalty-agent-demo` registers the SDK as two LangChain4j `@Tool` methods on
`NimbleTools`:

- `webSearch(query, recency)` → `nimble.search(...)`
- `extractUrl(url)` → `nimble.extract(..., format=markdown)`

The `LoyaltyAgent` interface is annotated `@RegisterAiService(tools = NimbleTools.class)`.
A system prompt instructs Claude to **always** consult the live web before
answering, cite sources, and present comparisons as markdown tables.

```
┌─────────────┐    ┌─────────────────┐    ┌────────────────┐    ┌────────────┐
│ index.html  │───▶│ AgentResource   │───▶│ LoyaltyAgent   │───▶│ Claude     │
│ (SSE stream)│    │ /agent/stream   │    │ (LangChain4j)  │    │ Sonnet 4.5 │
└─────────────┘    └─────────────────┘    └────────┬───────┘    └────────────┘
                                                   │ tools
                                                   ▼
                                          ┌────────────────┐
                                          │ NimbleTools    │
                                          │  · webSearch   │
                                          │  · extractUrl  │
                                          └────────┬───────┘
                                                   │
                                                   ▼
                                          ┌────────────────┐
                                          │ NimbleClient   │
                                          │ (this SDK)     │
                                          └────────┬───────┘
                                                   │
                                                   ▼
                                          ┌────────────────┐
                                          │ Nimble REST    │
                                          │ sdk.nimbleway  │
                                          └────────────────┘
```

## Configuration

`loyalty-agent-demo/src/main/resources/application.properties`:

```properties
nimble.api.key=${NIMBLE_API_KEY}
quarkus.langchain4j.anthropic.api-key=${ANTHROPIC_API_KEY}
quarkus.langchain4j.anthropic.chat-model.model-name=claude-sonnet-4-5
```

Override the model with `-Dquarkus.langchain4j.anthropic.chat-model.model-name=...`
or by exporting the property.

## Project status

- ✅ SDK: `search`, `extract` — sufficient for the three demo queries
- ☐ SDK: `serp`, `map`, `crawl`, `agents` — not yet covered (out of scope for the talk)
- ☐ Async/batch endpoints — sync only for now
- ☐ Tests — smoke-tested via `quarkus:dev` against live APIs

## License

MIT.
