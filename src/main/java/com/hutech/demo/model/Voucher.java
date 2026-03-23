package com.hutech.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vouchers")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    // Points required to redeem one voucher
    private long pointsRequired;

    // Flat discount amount in VND
    private double discountAmount;

    @Column(name = "otp_secret", length = 100)
    private String otpSecret;

    private boolean active = true;
}
