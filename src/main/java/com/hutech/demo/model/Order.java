package com.hutech.demo.model;

import jakarta.persistence.*;
import lombok.*;
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
    private String status = "PENDING"; // PENDING, CONFIRMED, SHIPPED, COMPLETED

    private double totalAmount;          // tổng tiền đơn hàng (sau khi trừ tích lũy)
    private long loyaltyPointsUsed;      // điểm tích lũy đã dùng
    private long loyaltyPointsEarned;    // điểm tích lũy được từ đơn này

    @OneToMany(mappedBy = "order")
    private List<OrderDetail> orderDetails;

    public double getTotalPrice() {
        if (orderDetails == null) return 0;
        return orderDetails.stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getProduct().getPrice())
                .sum();
    }
}
