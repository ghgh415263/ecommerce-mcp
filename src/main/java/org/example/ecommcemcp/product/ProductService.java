package org.example.ecommcemcp.product;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

  // 하이브리드 검색 파라미터. 51개 상품/질의 27개로 측정해 정한 값이다(벡터 단독 대비 개선폭은 작았다).
  private static final int CANDIDATES = 20;
  private static final int RRF_K = 60;
  private static final double VECTOR_WEIGHT = 1.0;
  private static final double KEYWORD_WEIGHT = 0.5;
  private static final double KEYWORD_MIN_SCORE = 0.6;
  private static final int MIN_TOKEN_LENGTH = 2;

  private final ProductRepository productRepository;
  private final VectorStore vectorStore;
  private final double similarityThreshold;

  public ProductService(
      ProductRepository productRepository,
      VectorStore vectorStore,
      @Value("${app.search.similarity-threshold}") double similarityThreshold) {
    this.productRepository = productRepository;
    this.vectorStore = vectorStore;
    this.similarityThreshold = similarityThreshold;
  }

  public Optional<ProductDetail> getProduct(long id) {
    return productRepository.findById(id).map(ProductDetail::from);
  }

  public List<ProductSummary> search(String keyword, String category) {
    return productRepository
        .search(nullToEmpty(keyword).strip(), nullToEmpty(category).strip())
        .stream()
        .map(ProductSummary::from)
        .toList();
  }

  /** 벡터(의미) 검색과 키워드(pg_trgm) 검색 결과를 RRF로 합쳐 상위 topK개를 반환한다. */
  public List<ProductSummary> hybridSearch(String query, int topK) {
    List<Long> fusedIds =
        fuse(vectorRanking(query), keywordRanking(query)).stream().limit(topK).toList();

    Map<Long, Product> byId =
        productRepository.findAllById(fusedIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

    return fusedIds.stream()
        .map(byId::get)
        .filter(Objects::nonNull)
        .map(ProductSummary::from)
        .toList();
  }

  /** 유사도 하한(similarityThreshold)을 넘는 후보를 유사도 순으로 반환한다. */
  private List<Long> vectorRanking(String query) {
    return vectorStore
        .similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(CANDIDATES)
                .similarityThreshold(similarityThreshold)
                .build())
        .stream()
        .map(doc -> Long.valueOf(doc.getId()))
        .toList();
  }

  /** 공백으로 나눈 토큰별 word_similarity를 합산해 점수순으로 반환한다. 토큰이 정확히 들어 있어야 점수가 높다. */
  private List<Long> keywordRanking(String query) {
    Map<Long, Double> totals = new HashMap<>();
    Arrays.stream(query.strip().split("\\s+"))
        .filter(token -> token.length() >= MIN_TOKEN_LENGTH)
        .distinct()
        .forEach(
            token ->
                productRepository
                    .keywordScores(token, KEYWORD_MIN_SCORE)
                    .forEach(row -> totals.merge(row.getId(), row.getScore(), Double::sum)));

    return totals.entrySet().stream()
        .sorted(
            Map.Entry.<Long, Double>comparingByValue()
                .reversed()
                .thenComparing(Map.Entry.comparingByKey()))
        .map(Map.Entry::getKey)
        .limit(CANDIDATES)
        .toList();
  }

  /**
   * Reciprocal Rank Fusion. 각 목록에서 순위가 높을수록 점수를 받고, 두 목록에 모두 있으면 점수가 합쳐진다. 키워드 목록은 흔한 단어("있는" 등)에
   * 걸리는 노이즈가 있어 가중치를 낮춘다.
   */
  static List<Long> fuse(List<Long> vectorIds, List<Long> keywordIds) {
    Map<Long, Double> scores = new HashMap<>();
    addRrfScores(scores, vectorIds, VECTOR_WEIGHT);
    addRrfScores(scores, keywordIds, KEYWORD_WEIGHT);

    return scores.entrySet().stream()
        .sorted(
            Map.Entry.<Long, Double>comparingByValue()
                .reversed()
                .thenComparing(Map.Entry.comparingByKey()))
        .map(Map.Entry::getKey)
        .toList();
  }

  private static void addRrfScores(Map<Long, Double> scores, List<Long> rankedIds, double weight) {
    for (int i = 0; i < rankedIds.size(); i++) {
      scores.merge(rankedIds.get(i), weight / (RRF_K + i + 1), Double::sum);
    }
  }

  /** 상품을 벡터 스토어에 (재)색인한다. 문서 ID가 상품 ID라서 같은 상품은 덮어쓴다. 상품을 저장/수정하는 곳에서 함께 호출해야 검색 결과가 최신으로 유지된다. */
  public void index(List<Product> products) {
    List<Document> documents =
        products.stream()
            .map(
                p ->
                    new Document(
                        String.valueOf(p.getId()),
                        toEmbeddingText(p),
                        Map.of("productId", p.getId(), "category", p.getCategory())))
            .toList();
    vectorStore.add(documents);
  }

  private static String toEmbeddingText(Product p) {
    return "%s\n브랜드: %s\n카테고리: %s\n옵션: %s\n%s"
        .formatted(
            p.getName(),
            p.getBrand(),
            p.getCategory(),
            String.join(", ", p.getOptions()),
            p.getDescription());
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
