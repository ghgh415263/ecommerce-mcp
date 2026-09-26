# ecommerce-mcp

AI 관련 기능들을 테스트해 보기 위한 프로젝트입니다.

이커머스 상품 검색을 MCP 도구로 제공하는 Spring AI MCP 서버를 예제로 삼아, 하이브리드 검색(벡터 + 키워드)과 AI 도구 연동을 실험합니다.

## AI 기능

- **MCP 서버**: `ProductTools`의 `@Tool` 메서드를 MCP(STREAMABLE) 도구로 노출해 LLM 클라이언트가 상품을 검색할 수 있습니다.
- **하이브리드 검색**: Ollama `bge-m3` 임베딩 기반 벡터 검색(pgvector)과 `pg_trgm` 키워드 검색을 RRF로 결합합니다.
- **PR AI 리뷰 및 서머리**: PR을 올리면 AI 리뷰와 서머리가 자동으로 진행됩니다. 서머리는 IBM Bob이 변경사항을 요약해 PR 코멘트로 남깁니다(`.github/workflows/pr-summary.yml`). 다시 돌리고 싶으면 PR에 `ai-check` 라벨을 붙이면 됩니다.

## 실행

Docker가 필요합니다. Postgres(pgvector)와 Ollama는 앱 실행 시 자동으로 기동됩니다.

```bash
./gradlew bootRun   # 서버 실행
./gradlew build     # 컴파일 + checkstyle + 테스트
```
