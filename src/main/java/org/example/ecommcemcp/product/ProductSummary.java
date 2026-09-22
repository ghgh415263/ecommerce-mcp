package org.example.ecommcemcp.product;

public record ProductSummary(
    long id, String name, String category, int price, int stock, String brand) {

  public static ProductSummary from(Product p) {
    return new ProductSummary(
        p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock(), p.getBrand());
  }
}
