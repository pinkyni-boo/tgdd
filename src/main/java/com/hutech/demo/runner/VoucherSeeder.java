package com.hutech.demo.runner;

import com.hutech.demo.model.Voucher;
import com.hutech.demo.repository.VoucherRepository;
import com.hutech.demo.service.TotpService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class VoucherSeeder implements CommandLineRunner {

    private final VoucherRepository voucherRepository;
    private final TotpService totpService;

    @Override
    public void run(String... args) {
        // Luon upsert de moi lan khoi dong deu co voucher test OTP.
        upsertVoucher("OTPTEST5K", "Voucher test 5.000đ", 10, 5_000);
        upsertVoucher("OTPTEST20K", "Voucher test 20.000đ", 50, 20_000);
        upsertVoucher("OTPTEST50K", "Voucher test 50.000đ", 120, 50_000);

        // Bo voucher muc diem cao de test du lieu that.
        upsertVoucher("VIP50K", "Voucher 50.000đ", 500, 50_000);
        upsertVoucher("VIP100K", "Voucher 100.000đ", 900, 100_000);
        upsertVoucher("VIP200K", "Voucher 200.000đ", 1700, 200_000);
    }

    private void upsertVoucher(String code, String name, long pointsRequired, double discountAmount) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code).orElseGet(Voucher::new);
        voucher.setCode(code.toUpperCase());
        voucher.setName(name);
        voucher.setDescription("Doi tu diem tich luy");
        voucher.setPointsRequired(pointsRequired);
        voucher.setDiscountAmount(discountAmount);
        if (voucher.getOtpSecret() == null || voucher.getOtpSecret().isBlank()) {
            voucher.setOtpSecret(totpService.generateSecret());
        }
        voucher.setActive(true);
        voucherRepository.save(voucher);
    }
}
