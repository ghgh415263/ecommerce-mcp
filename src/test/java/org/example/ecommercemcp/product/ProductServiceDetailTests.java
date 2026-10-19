package org.example.ecommercemcp.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 상품 상세조회 기능 테스트. PostgreSQL(pgvector) 필요.
@SpringBootTest(
    properties = {
      "spring.docker.compose.skip.in-tests=false",
      "app.seed.enabled=false",
      "spring.ai.ollama.init.pull-model-strategy=never"
    })
class ProductServiceDetailTests {

  @Autowired private ProductRepository productRepository;

  @Autowired private ProductService productService;

  private Product testProduct;

  @BeforeEach
  void setUp() {
    // 기존 테스트 데이터 정리
    productRepository.deleteAll();

    // 테스트용 상품 생성
    testProduct =
        new Product(
            "테스트 상품",
            "전자기기",
            50000,
            100,
            "테스트 브랜드",
            "이것은 테스트용 상품입니다.",
            "http://example.com/image.jpg",
            List.of("검은색", "대형"));
    productRepository.save(testProduct);
  }

  @Test
  @DisplayName("상품_ID로_상세_정보를_조회한다")
  void 상품_ID로_상세_정보를_조회한다() {
    // given
    long productId = testProduct.getId();

    // when
    ProductDetail detail = productService.getProductDetail(productId);

    // then
    assertThat(detail)
        .isNotNull()
        .extracting(
            ProductDetail::id,
            ProductDetail::name,
            ProductDetail::category,
            ProductDetail::price,
            ProductDetail::stock,
            ProductDetail::brand,
            ProductDetail::description,
            ProductDetail::imageUrl)
        .containsExactly(
            productId,
            "테스트 상품",
            "전자기기",
            50000,
            100,
            "테스트 브랜드",
            "이것은 테스트용 상품입니다.",
            "http://example.com/image.jpg");
  }

  @Test
  @DisplayName("상품_옵션들이_포함된다")
  void 상품_옵션들이_포함된다() {
    // given
    long productId = testProduct.getId();

    // when
    ProductDetail detail = productService.getProductDetail(productId);

    // then
    assertThat(detail.options()).containsExactly("검은색", "대형");
  }

  @Test
  @DisplayName("존재하지_않는_상품_ID는_null을_반환한다")
  void 존재하지_않는_상품_ID는_null을_반환한다() {
    // given
    long nonExistentId = 99999L;

    // when
    ProductDetail detail = productService.getProductDetail(nonExistentId);

    // then
    assertThat(detail).isNull();
  }
}

