package org.example.ecommcemcp.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

  /** 빈 문자열은 "조건 없음"으로 취급한다. (null 파라미터는 PostgreSQL 타입 추론 문제가 있어 쓰지 않는다) */
  @Query(
      """
            select p from Product p
            where lower(p.name) like lower(concat('%', :keyword, '%'))
              and (:category = '' or p.category = :category)
            order by p.id
            """)
  List<Product> search(@Param("keyword") String keyword, @Param("category") String category);

  /**
   * 토큰 하나가 상품 텍스트(이름/브랜드/카테고리/설명/옵션)에 얼마나 들어 있는지 pg_trgm word_similarity(0~1)로 계산한다. 토큰이 텍스트 안에 그대로
   * 있으면 1.0이고, minScore 미만은 제외한다.
   */
  @Query(
      value =
          """
            select t.id as id, t.score as score
            from (
                select p.id as id,
                       cast(word_similarity(:token, concat_ws(' ', p.name, p.brand, p.category, p.description,
                           coalesce((select string_agg(o.option_name, ' ') from product_options o
                                     where o.product_id = p.id), ''))) as double precision) as score
                from products p
            ) t
            where t.score >= :minScore
            """,
      nativeQuery = true)
  List<KeywordScore> keywordScores(
      @Param("token") String token, @Param("minScore") double minScore);
}
