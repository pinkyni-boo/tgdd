package com.hutech.demo.controller;

import com.hutech.demo.model.Customer;
import com.hutech.demo.model.CustomerVoucher;
import com.hutech.demo.model.User;
import com.hutech.demo.model.Voucher;
import com.hutech.demo.service.LoyaltyService;
import com.hutech.demo.service.TotpService;
import com.hutech.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final UserService userService;
    private final TotpService totpService;

    @GetMapping("/loyalty")
    public String loyaltyPage(@RequestParam(value = "tab", defaultValue = "lookup") String tab,
                              Authentication authentication,
                              Model model) {
        User user = getCurrentUser(authentication);
        Customer customer = loyaltyService.getOrCreateCustomer(user.getPhone(), user.getUsername());

        model.addAttribute("redeemableVouchers", loyaltyService.getRedeemableVouchers());
        model.addAttribute("activeTab", normalizeTab(tab));
        model.addAttribute("lookupPhone", user.getPhone());
        model.addAttribute("customer", customer);
        model.addAttribute("orders", loyaltyService.findOrdersByPhone(user.getPhone()));
        model.addAttribute("customerVouchers", loyaltyService.findCustomerVouchersByPhone(user.getPhone()));
        model.addAttribute("mfaEnabled", user.isMfaEnabled());
        return "loyalty/lookup";
    }

    @PostMapping("/loyalty/redeem")
    public String redeemVoucher(@RequestParam("voucherId") Long voucherId,
                                @RequestParam("otpCode") String otpCode,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(authentication);

        try {
            Voucher voucher = loyaltyService.getVoucherById(voucherId);
            String voucherSecret = loyaltyService.ensureVoucherOtpSecret(voucherId);

            if (!totpService.verifyCode(voucherSecret, otpCode)) {
                redirectAttributes.addFlashAttribute("error", "OTP khong dung cho voucher " + voucher.getCode() + ".");
                return "redirect:/loyalty?tab=redeem";
            }

            CustomerVoucher redeemed = loyaltyService.redeemVoucher(user.getPhone(), voucherId);
            String voucherCode = redeemed.getVoucher() != null ? redeemed.getVoucher().getCode() : "";
            redirectAttributes.addFlashAttribute("message", "Doi voucher thanh cong: " + voucherCode);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/loyalty?tab=redeem";
    }

    @GetMapping("/loyalty/voucher-otp")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getVoucherOtp(@RequestParam("voucherId") Long voucherId,
                                                              Authentication authentication) {
        User user = getCurrentUser(authentication);
        Voucher voucher = loyaltyService.getVoucherById(voucherId);
        String secret = loyaltyService.ensureVoucherOtpSecret(voucherId);

        String accountLabel = user.getUsername() + "-" + voucher.getCode();
        String otpAuthUrl = totpService.buildOtpAuthUrl(accountLabel, secret);
        String qrCodeDataUri = totpService.buildQrCodeDataUri(otpAuthUrl);

        return ResponseEntity.ok(Map.of(
                "voucherCode", voucher.getCode(),
                "voucherName", voucher.getName(),
                "manualSecret", secret,
                "qrCodeDataUri", qrCodeDataUri
        ));
    }

    @GetMapping("/loyalty/mfa/setup")
    public String setupMfa(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);

        String secret = user.getMfaSecret();
        if (secret == null || secret.isBlank()) {
            secret = totpService.generateSecret();
            userService.setMfaSecret(user.getUsername(), secret);
            user = userService.getDomainUserByUsername(user.getUsername());
        }

        String otpAuthUrl = totpService.buildOtpAuthUrl(user.getUsername(), secret);
        model.addAttribute("qrCodeDataUri", totpService.buildQrCodeDataUri(otpAuthUrl));
        model.addAttribute("manualSecret", secret);
        model.addAttribute("mfaEnabled", user.isMfaEnabled());
        return "loyalty/mfa-setup";
    }

    @PostMapping("/loyalty/mfa/verify")
    public String verifyMfa(@RequestParam("otpCode") String otpCode,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(authentication);
        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Chua co ma OTP setup. Vui long tao lai QR.");
            return "redirect:/loyalty/mfa/setup";
        }

        if (!totpService.verifyCode(user.getMfaSecret(), otpCode)) {
            redirectAttributes.addFlashAttribute("error", "Xac thuc OTP that bai. Vui long thu lai.");
            return "redirect:/loyalty/mfa/setup";
        }

        userService.enableMfa(user.getUsername());
        redirectAttributes.addFlashAttribute("message", "Kich hoat OTP thanh cong. Ban da co the doi diem.");
        return "redirect:/loyalty?tab=redeem";
    }

    private String normalizeTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return "lookup";
        }
        String normalized = tab.trim().toLowerCase();
        return "redeem".equals(normalized) ? "redeem" : "lookup";
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Unauthenticated user");
        }
        return userService.getDomainUserByUsername(authentication.getName());
    }
}
