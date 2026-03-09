package com.hutech.demo.runner;

import com.hutech.demo.model.Voucher;
import com.hutech.demo.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class VoucherSeeder implements CommandLineRunner {

    private final VoucherRepository voucherRepository;

    @Override
    public void run(String... args) {
        if (voucherRepository.count() > 0) {
            return;
        }

        voucherRepository.save(createVoucher("VIP50K", "Voucher 50.000đ", 500, 50_000));
        voucherRepository.save(createVoucher("VIP100K", "Voucher 100.000đ", 900, 100_000));
        voucherRepository.save(createVoucher("VIP200K", "Voucher 200.000đ", 1700, 200_000));
    }

    private Voucher createVoucher(String code, String name, long pointsRequired, double discountAmount) {
        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setName(name);
        voucher.setDescription("Doi tu diem tich luy");
        voucher.setPointsRequired(pointsRequired);
        voucher.setDiscountAmount(discountAmount);
        voucher.setActive(true);
        return voucher;
    }
}
