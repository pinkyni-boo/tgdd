package com.hutech.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String phone;
    private String address;
    private String payment;
    // PENDING, CONFIRMED, SHIPPED, COMPLETED
    private String status = "PENDING";
    // UNPAID, PAID, FAILED
    private String paymentStatus = "UNPAID";

    private double subtotalAmount;
    private double shippingFee;
    private double voucherDiscount;
    private String voucherCodeUsed;
    // Final payable amount
    private double totalAmount;

    // Loyalty accounting
    private long loyaltyPointsUsed;
    private long loyaltyPointsEarned;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order")
    private List<OrderDetail> orderDetails;

    public double getTotalPrice() {
        if (orderDetails == null) return 0;
        return orderDetails.stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getUnitPrice())
                .sum();
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
