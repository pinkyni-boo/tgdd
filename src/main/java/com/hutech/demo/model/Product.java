package com.hutech.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double price;
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private String image;
    
    // Discount percentage (0-100)
    private double discount;

    private boolean isPromotional;

    // Remaining quantity for flash-sale pricing
    private int promotionQuantity = 0;

    // Base price before promotion; used to restore price when promotion ends
    private Double originalPrice;
}
