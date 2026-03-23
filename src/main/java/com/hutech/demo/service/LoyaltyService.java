package com.hutech.demo.service;

import com.hutech.demo.model.Customer;
import com.hutech.demo.model.CustomerVoucher;
import com.hutech.demo.model.Order;
import com.hutech.demo.model.Voucher;
import com.hutech.demo.repository.CustomerRepository;
import com.hutech.demo.repository.CustomerVoucherRepository;
import com.hutech.demo.repository.OrderRepository;
import com.hutech.demo.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LoyaltyService {
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final VoucherRepository voucherRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final TotpService totpService;

    @Transactional(readOnly = true)
    public Optional<Customer> findCustomerByPhone(String phone) {
        if (phone == null || phone.isBlank()) return Optional.empty();
        return customerRepository.findByPhone(phone.trim());
    }

    public Customer getOrCreateCustomer(String phone, String customerName) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }
        String normalizedPhone = phone.trim();
        return customerRepository.findByPhone(normalizedPhone).orElseGet(() -> {
            Customer customer = new Customer();
            customer.setPhone(normalizedPhone);
            customer.setCustomerName(customerName);
            customer.setLoyaltyPoints(0);
            return customerRepository.save(customer);
        });
    }

    @Transactional(readOnly = true)
    public List<Order> findOrdersByPhone(String phone) {
        if (phone == null || phone.isBlank()) return List.of();
        return orderRepository.findByPhoneOrderByCreatedAtDesc(phone.trim());
    }

    @Transactional(readOnly = true)
    public List<CustomerVoucher> findCustomerVouchersByPhone(String phone) {
        if (phone == null || phone.isBlank()) return List.of();
        return customerVoucherRepository.findByCustomerPhoneOrderByRedeemedAtDesc(phone.trim());
    }

    @Transactional(readOnly = true)
    public List<CustomerVoucher> findUnusedCustomerVouchersByPhone(String phone) {
        if (phone == null || phone.isBlank()) return List.of();
        return customerVoucherRepository
                .findByCustomerPhoneAndStatusOrderByRedeemedAtDesc(phone.trim(), "UNUSED")
                .stream()
                .filter(cv -> cv.getVoucher() != null && cv.getVoucher().isActive())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Voucher> getRedeemableVouchers() {
        return voucherRepository.findByActiveTrueOrderByPointsRequiredAsc();
    }

    public Voucher getVoucherById(Long voucherId) {
        return voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
    }

    public String ensureVoucherOtpSecret(Long voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        if (voucher.getOtpSecret() == null || voucher.getOtpSecret().isBlank()) {
            voucher.setOtpSecret(totpService.generateSecret());
            voucherRepository.save(voucher);
        }
        return voucher.getOtpSecret();
    }

    public CustomerVoucher redeemVoucher(String phone, Long voucherId) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }

        String normalizedPhone = phone.trim();
        Customer customer = customerRepository.findWithLockByPhone(normalizedPhone)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));

        if (!voucher.isActive()) {
            throw new IllegalStateException("Voucher is inactive");
        }
        if (customer.getLoyaltyPoints() < voucher.getPointsRequired()) {
            throw new IllegalStateException("Not enough loyalty points");
        }

        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - voucher.getPointsRequired());
        customerRepository.save(customer);

        CustomerVoucher customerVoucher = new CustomerVoucher();
        customerVoucher.setCustomer(customer);
        customerVoucher.setVoucher(voucher);
        customerVoucher.setStatus("UNUSED");
        return customerVoucherRepository.save(customerVoucher);
    }
}
