package org.example.ecommcemcp.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProductServiceFuseTests {

  @Test
  void 벡터_순위를_기본으로_따른다() {
    assertThat(ProductService.fuse(List.of(1L, 2L, 3L), List.of())).containsExactly(1L, 2L, 3L);
  }

  @Test
  void 두_목록에_모두_있는_상품이_한쪽에만_있는_상품보다_앞선다() {
    // 3은 벡터 2위 + 키워드 1위, 1은 벡터 1위만
    assertThat(ProductService.fuse(List.of(1L, 3L), List.of(3L))).containsExactly(3L, 1L);
  }

  @Test
  void 키워드에만_걸린_상품도_포함된다() {
    assertThat(ProductService.fuse(List.of(1L), List.of(9L))).containsExactly(1L, 9L);
  }

  @Test
  void 키워드_가중치가_낮아서_키워드_1위가_벡터_1위를_뒤집지_못한다() {
    assertThat(ProductService.fuse(List.of(1L, 2L), List.of(7L, 1L))).first().isEqualTo(1L);
    assertThat(ProductService.fuse(List.of(1L), List.of(7L))).containsExactly(1L, 7L);
  }

  @Test
  void 둘_다_비어_있으면_빈_목록() {
    assertThat(ProductService.fuse(List.of(), List.of())).isEmpty();
  }
}
