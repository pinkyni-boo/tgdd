package com.hutech.demo.controller;

import com.hutech.demo.model.Customer;
import com.hutech.demo.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/loyalty")
    public String loyaltyPage(@RequestParam(value = "phone", required = false) String phone,
                              @RequestParam(value = "tab", defaultValue = "lookup") String tab,
                              Model model) {
        model.addAttribute("redeemableVouchers", loyaltyService.getRedeemableVouchers());
        model.addAttribute("activeTab", normalizeTab(tab));

        String normalizedPhone = phone == null ? "" : phone.trim();
        model.addAttribute("lookupPhone", normalizedPhone);

        if (normalizedPhone.isBlank()) {
            return "loyalty/lookup";
        }

        Optional<Customer> customer = loyaltyService.findCustomerByPhone(normalizedPhone);
        if (customer.isEmpty()) {
            model.addAttribute("notFound", true);
            return "loyalty/lookup";
        }

        model.addAttribute("customer", customer.get());
        model.addAttribute("orders", loyaltyService.findOrdersByPhone(normalizedPhone));
        model.addAttribute("customerVouchers", loyaltyService.findCustomerVouchersByPhone(normalizedPhone));
        return "loyalty/lookup";
    }

    @PostMapping("/loyalty/redeem")
    public String redeemVoucher(@RequestParam("phone") String phone,
                                @RequestParam("voucherId") Long voucherId,
                                RedirectAttributes redirectAttributes) {
        String normalizedPhone = phone == null ? "" : phone.trim();
        if (normalizedPhone.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Phone is required.");
            return "redirect:/loyalty?tab=redeem";
        }

        try {
            loyaltyService.redeemVoucher(normalizedPhone, voucherId);
            redirectAttributes.addFlashAttribute("message", "Voucher redeemed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/loyalty?phone=" + URLEncoder.encode(normalizedPhone, StandardCharsets.UTF_8) + "&tab=redeem";
    }

    private String normalizeTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return "lookup";
        }
        String normalized = tab.trim().toLowerCase();
        return "redeem".equals(normalized) ? "redeem" : "lookup";
    }
}
