package com.hutech.demo.repository;

import com.hutech.demo.model.CustomerVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Long> {
    List<CustomerVoucher> findByCustomerPhoneOrderByRedeemedAtDesc(String phone);
    List<CustomerVoucher> findByCustomerPhoneAndStatusOrderByRedeemedAtDesc(String phone, String status);

    Optional<CustomerVoucher> findFirstByCustomerPhoneAndVoucherCodeIgnoreCaseAndStatusOrderByRedeemedAtAsc(
            String phone, String voucherCode, String status);
}
