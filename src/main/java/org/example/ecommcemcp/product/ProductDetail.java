package org.example.ecommcemcp.product;

import java.util.List;

public record ProductDetail(
    long id,
    String name,
    String category,
    int price,
    int stock,
    String brand,
    String description,
    String imageUrl,
    List<String> options) {

  public static ProductDetail from(Product p) {
    return new ProductDetail(
        p.getId(),
        p.getName(),
        p.getCategory(),
        p.getPrice(),
        p.getStock(),
        p.getBrand(),
        p.getDescription(),
        p.getImageUrl(),
        List.copyOf(p.getOptions()));
  }
}
