package org.example.ecommcemcp.product;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    private int price;

    private int stock;

    private String brand;

    @Column(length = 2000)
    private String description;

    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_options", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "option_name")
    private List<String> options = new ArrayList<>();

    protected Product() {
    }

    public Product(String name, String category, int price, int stock,
                   String brand, String description, String imageUrl, List<String> options) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.brand = brand;
        this.description = description;
        this.imageUrl = imageUrl;
        this.options = new ArrayList<>(options);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public String getBrand() {
        return brand;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getOptions() {
        return options;
    }
}
