package org.example.ecommercemcp.config;

import org.example.ecommercemcp.product.ProductTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

  @Bean
  public ToolCallbackProvider productToolCallbackProvider(ProductTools productTools) {

    return MethodToolCallbackProvider.builder().toolObjects(productTools).build();
  }
}
