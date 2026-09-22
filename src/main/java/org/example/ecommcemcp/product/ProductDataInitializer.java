package org.example.ecommcemcp.product;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 개발용 시드 데이터. DB에 없는 상품(상품명 기준)만 추가하고, 새로 추가한 상품만 벡터 스토어에 색인한다.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class ProductDataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductService productService;

    public ProductDataInitializer(ProductRepository productRepository, ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> existingNames = productRepository.findAll().stream()
                .map(Product::getName)
                .collect(Collectors.toSet());

        List<Product> newProducts = ProductSeedData.all().stream()
                .filter(p -> !existingNames.contains(p.getName()))
                .toList();
        if (newProducts.isEmpty()) {
            return;
        }

        productService.index(productRepository.saveAll(newProducts));
    }
}
