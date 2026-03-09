package com.hutech.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer_vouchers")
public class CustomerVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    // UNUSED, USED
    private String status = "UNUSED";

    private LocalDateTime redeemedAt;
    private LocalDateTime usedAt;

    @ManyToOne
    @JoinColumn(name = "used_order_id")
    private Order usedOrder;

    @PrePersist
    public void onCreate() {
        if (redeemedAt == null) {
            redeemedAt = LocalDateTime.now();
        }
    }
}
