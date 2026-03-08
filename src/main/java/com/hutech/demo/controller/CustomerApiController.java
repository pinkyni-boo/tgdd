package com.hutech.demo.controller;

import com.hutech.demo.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerApiController {

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/points")
    public Map<String, Object> getPoints(@RequestParam String phone) {
        return customerRepository.findByPhone(phone)
                .map(c -> Map.<String, Object>of(
                        "found", true,
                        "customerName", c.getCustomerName() != null ? c.getCustomerName() : "",
                        "loyaltyPoints", c.getLoyaltyPoints()))
                .orElse(Map.of("found", false, "customerName", "", "loyaltyPoints", 0L));
    }
}
