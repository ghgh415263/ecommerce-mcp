package org.example.ecommcemcp.product;

import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ProductTools {

  private static final int DEFAULT_TOP_K = 5;
  private static final int MAX_TOP_K = 20;

  private final ProductService productService;

  public ProductTools(ProductService productService) {
    this.productService = productService;
  }

  @Tool(description = "상품 ID로 상품 상세 정보(이름, 카테고리, 가격, 재고, 브랜드, 설명, 이미지, 옵션)를 조회한다. 없으면 null을 반환한다.")
  public ProductDetail getProduct(@ToolParam(description = "조회할 상품 ID") long productId) {
    return productService.getProduct(productId).orElse(null);
  }

  @Tool(
      description =
          "키워드(상품명 일부)와 카테고리로 상품 목록을 검색한다. 조건을 생략하면 전체 목록을 반환한다. 요약 정보만 반환하므로 상세 정보는 getProduct로 조회한다.")
  public List<ProductSummary> searchProducts(
      @ToolParam(description = "상품명에 포함된 검색 키워드", required = false) String keyword,
      @ToolParam(description = "카테고리 정확히 일치 (예: 전자기기, 생활용품, 패션)", required = false)
          String category) {
    return productService.search(keyword, category);
  }

  @Tool(
      description =
          "자연어 설명이나 키워드로 상품을 찾는다(의미 검색 + 키워드 검색 결합). 상품명을 정확히 몰라도 되고, 브랜드나 규격 같은 단어를 넣어도 된다. 예: '사무실에서 조용하게 쓸 입력 장치', '나이키 신발', 'USB-C 케이블'. 관련도가 높은 순으로 요약 정보를 반환하며 상세 정보는 getProduct로 조회한다. 관련 상품이 없으면 빈 목록을 반환한다.")
  public List<ProductSummary> hybridSearchProducts(
      @ToolParam(description = "찾고 싶은 상품에 대한 자연어 설명 또는 키워드") String query,
      @ToolParam(description = "반환할 최대 개수 (기본 5, 최대 20)", required = false) Integer topK) {
    int limit = topK == null ? DEFAULT_TOP_K : Math.max(1, Math.min(topK, MAX_TOP_K));
    return productService.hybridSearch(query, limit);
  }
}
