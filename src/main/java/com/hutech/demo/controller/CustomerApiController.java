package com.hutech.demo.controller;

import com.hutech.demo.service.LoyaltyService;
import com.hutech.demo.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerApiController {

    private final CustomerRepository customerRepository;
    private final LoyaltyService loyaltyService;

    @GetMapping("/points")
    public Map<String, Object> getPoints(@RequestParam String phone) {
        return customerRepository.findByPhone(phone)
                .map(c -> Map.<String, Object>of(
                        "found", true,
                        "customerName", c.getCustomerName() != null ? c.getCustomerName() : "",
                        "loyaltyPoints", c.getLoyaltyPoints()))
                .orElse(Map.of("found", false, "customerName", "", "loyaltyPoints", 0L));
    }

    @GetMapping("/vouchers")
    public List<Map<String, Object>> getUnusedVouchers(@RequestParam String phone) {
        return loyaltyService.findUnusedCustomerVouchersByPhone(phone).stream()
                .map(cv -> Map.<String, Object>of(
                        "id", cv.getId(),
                        "code", cv.getVoucher().getCode(),
                        "name", cv.getVoucher().getName(),
                        "discountAmount", cv.getVoucher().getDiscountAmount(),
                        "pointsRequired", cv.getVoucher().getPointsRequired(),
                        "redeemedAt", cv.getRedeemedAt() != null ? cv.getRedeemedAt().toString() : ""
                ))
                .toList();
    }
}
