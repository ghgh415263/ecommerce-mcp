package org.example.ecommcemcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// compose.yaml의 PostgreSQL(pgvector)이 필요하므로 테스트에서도 docker compose를 띄운다.
// 시드/임베딩(Ollama)과 모델 pull은 끈다.
@SpringBootTest(
    properties = {
      "spring.docker.compose.skip.in-tests=false",
      "app.seed.enabled=false",
      "spring.ai.ollama.init.pull-model-strategy=never"
    })
class EcommceMcpApplicationTests {

  @Test
  void contextLoads() {}
}
