# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Spring AI **MCP server** that exposes e-commerce product search as MCP tools (for an LLM client to call). It is not a REST API for browsers/frontends — the only "API surface" is the set of `@Tool`-annotated methods in `ProductTools`, served over the MCP STREAMABLE protocol via Spring WebMVC.

## Commands

Requires Docker (Postgres/pgvector + Ollama are started automatically via `spring-boot-docker-compose`, `compose.yaml`, when the app or tests run).

```bash
./gradlew bootRun                      # run the server (also starts compose services, left running: lifecycle-management=start_only)
./gradlew build                        # compile + checkstyle + test
./gradlew test                         # run all tests
./gradlew test --tests "EcommceMcpApplicationTests"   # run a single test class
./gradlew checkstyleMain               # lint main sources only (checkstyleTest is disabled, see below)
```

Test method names use Korean BDD-style phrases (e.g. `키워드에만_걸린_상품도_포함된다`) — quote/escape accordingly when filtering with `--tests` on a single method.

`checkstyleTest` is intentionally disabled in `build.gradle` because test method names don't fit Java naming conventions.

### Checkstyle hook

A `PostToolUse` hook (`.claude/hooks/checkstyle-lint.sh`, wired in `.claude/settings.json`) runs `./gradlew checkstyleMain` automatically after every Write/Edit to a `.java` file and blocks with violation output. Rules are in `config/checkstyle/checkstyle.xml` (Google style, relaxed; max line length 150, no tabs, no star imports, standard whitespace/brace/naming checks). `maxWarnings = 0` in `build.gradle`, so any violation fails the build.

## Architecture

Single domain package: `org.example.ecommcemcp.product`. Everything (entity, repository, service, DTOs, MCP tools, seed data) lives there; `config/McpToolConfig` is the only thing outside it.

**MCP tool registration**: `ProductTools` methods annotated `@Tool` are wired into an MCP `ToolCallbackProvider` bean in `McpToolConfig` via `MethodToolCallbackProvider`. Adding a new tool means adding a `@Tool`-annotated method to `ProductTools` (or a new `*Tools` component picked up the same way) — there is no manual registration step beyond that.

**Hybrid search** (`ProductService.hybridSearch`) is the core piece of logic. It combines two independently-ranked candidate lists via **Reciprocal Rank Fusion (RRF)**:
- *Vector ranking*: semantic similarity search against `VectorStore` (pgvector), using Ollama `bge-m3` embeddings (1024 dims, cosine distance, HNSW index). Filtered by `app.search.similarity-threshold` (currently 0.42 — tuned empirically per the comment in `application.properties`; re-measure if the embedding model or product data changes).
- *Keyword ranking*: per-token `pg_trgm word_similarity` against a concatenation of name/brand/category/description/options (`ProductRepository.keywordScores`, native query), filtered by `KEYWORD_MIN_SCORE`.
- Fusion weights vector results higher than keyword results (`VECTOR_WEIGHT` vs `KEYWORD_WEIGHT` in `ProductService`) because raw keyword hits are noisier. `ProductService.fuse(...)` is the pure, unit-tested fusion function (`ProductServiceFuseTests`) — RRF tuning constants (`CANDIDATES`, `RRF_K`, weights) are documented inline as measured against 51 seed products / 27 queries.
- Vector store documents are keyed by product ID as a string (`Document` id = `String.valueOf(productId)`), so re-indexing a product overwrites its embedding. Anything that inserts/updates a `Product` must call `ProductService.index(...)` afterward or search results go stale — there's no automatic sync (e.g. no JPA listener).

**Data model**: `Product` is the JPA entity (options are an `@ElementCollection` in a side table `product_options`). `ProductDetail`/`ProductSummary` are read-only records built via `from(Product)` factory methods — controllers/tools never expose the entity directly.

**Dev seed data**: `ProductDataInitializer` (an `ApplicationRunner`, active only when `app.seed.enabled=true`) inserts `ProductSeedData.all()` products that don't already exist (matched by name) and indexes only the newly-inserted ones into the vector store.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/): `<type>: <subject>`, optionally `<type>(<scope>): <subject>`.

- Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `style`, `perf`.
- Subject follows the existing repo convention of Korean, written in imperative/declarative present tense (e.g. `feat: 하이브리드 검색에 카테고리 필터 추가`), not past tense (`추가함`, `추가했음`).
- Keep the subject line under ~72 chars; add a body only when the "why" isn't obvious from the diff.
- One logical change per commit — don't mix unrelated fixes/features in one commit.

## Config notes (`application.properties`)

- `spring.jpa.hibernate.ddl-auto=update` — schema evolves via Hibernate, not migrations.
- `schema.sql` only bootstraps the `pg_trgm` extension (needed for keyword search); it doesn't define tables.
- Postgres is exposed on `15432` (not 5432) to avoid Windows' reserved port range.
- Tests (`EcommceMcpApplicationTests`) still require the Postgres/pgvector compose service but disable seeding and Ollama model pulling via property overrides.
