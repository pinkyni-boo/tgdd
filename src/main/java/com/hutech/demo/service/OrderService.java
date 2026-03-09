package com.hutech.demo.service;

import com.hutech.demo.model.CartItem;
import com.hutech.demo.model.Customer;
import com.hutech.demo.model.CustomerVoucher;
import com.hutech.demo.model.Order;
import com.hutech.demo.model.OrderDetail;
import com.hutech.demo.model.Product;
import com.hutech.demo.model.Voucher;
import com.hutech.demo.repository.CustomerRepository;
import com.hutech.demo.repository.CustomerVoucherRepository;
import com.hutech.demo.repository.OrderDetailRepository;
import com.hutech.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartService cartService;
    private final CustomerRepository customerRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final ProductService productService;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByCustomer(String customerName) {
        return orderRepository.findByCustomerName(customerName);
    }

    public List<Order> getOrdersByPhone(String phone) {
        return orderRepository.findByPhoneOrderByCreatedAtDesc(phone);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    public Order getOrderByIdAndPhone(Long id, String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }
        return orderRepository.findByIdAndPhone(id, phone.trim())
                .orElseThrow(() -> new IllegalArgumentException("Order not found for this phone: " + id));
    }

    @Transactional
    public void confirmOrder(Long id) {
        Order order = getOrderById(id);
        order.setStatus("CONFIRMED");
        orderRepository.save(order);
    }

    @Transactional
    public Order createOrder(String customerName, String phone, String address, String payment,
                             boolean useLoyaltyPoints, String voucherCode, List<CartItem> cartItems) {
        double subtotal = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();

        int totalQty = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        double shippingFee = (totalQty >= 2 && subtotal >= 1_000_000d) ? 0d : (cartItems.isEmpty() ? 0d : 30_000d);

        Customer customer = findOrCreateCustomer(customerName, phone);

        double voucherDiscount = 0d;
        String voucherCodeUsed = null;
        CustomerVoucher usedCustomerVoucher = null;

        if (voucherCode != null && !voucherCode.isBlank()) {
            Optional<CustomerVoucher> cvOpt = customerVoucherRepository
                    .findFirstByCustomerPhoneAndVoucherCodeIgnoreCaseAndStatusOrderByRedeemedAtAsc(
                            phone, voucherCode.trim(), "UNUSED");
            if (cvOpt.isPresent()) {
                CustomerVoucher cv = cvOpt.get();
                Voucher voucher = cv.getVoucher();
                if (voucher != null && voucher.isActive()) {
                    voucherDiscount = Math.min(voucher.getDiscountAmount(), subtotal + shippingFee);
                    voucherCodeUsed = voucher.getCode();
                    usedCustomerVoucher = cv;
                }
            }
        }

        double amountAfterVoucher = Math.max(0d, subtotal + shippingFee - voucherDiscount);

        long pointsUsed = 0;
        if (useLoyaltyPoints && customer.getLoyaltyPoints() > 0) {
            pointsUsed = Math.min(customer.getLoyaltyPoints(), (long) amountAfterVoucher);
        }

        double finalAmount = Math.max(0d, amountAfterVoucher - pointsUsed);
        long newPoints = (long) (finalAmount / 1000d);

        customer.setCustomerName(customerName);
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - pointsUsed + newPoints);
        customerRepository.save(customer);

        Order order = new Order();
        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setAddress(address);
        order.setPayment(payment);
        order.setStatus("PENDING");
        order.setPaymentStatus("VNPAY".equalsIgnoreCase(payment) ? "UNPAID" : "PAID");
        order.setSubtotalAmount(subtotal);
        order.setShippingFee(shippingFee);
        order.setVoucherDiscount(voucherDiscount);
        order.setVoucherCodeUsed(voucherCodeUsed);
        order.setTotalAmount(finalAmount);
        order.setLoyaltyPointsUsed(pointsUsed);
        order.setLoyaltyPointsEarned(newPoints);
        order = orderRepository.save(order);

        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(product.getPrice());
            detail.setDiscountPercent(product.getDiscount());
            detail.setPromotionalApplied(product.isPromotional() && product.getPromotionQuantity() > 0 && product.getDiscount() > 0);
            orderDetailRepository.save(detail);

            if (detail.isPromotionalApplied()) {
                productService.consumePromotionQuantity(product, item.getQuantity());
            }
        }

        if (usedCustomerVoucher != null) {
            usedCustomerVoucher.setStatus("USED");
            usedCustomerVoucher.setUsedAt(java.time.LocalDateTime.now());
            usedCustomerVoucher.setUsedOrder(order);
            customerVoucherRepository.save(usedCustomerVoucher);
        }

        cartService.clearCart();
        return order;
    }

    @Transactional
    public void markAsPaid(Long orderId) {
        Order order = getOrderById(orderId);
        order.setPaymentStatus("PAID");
        orderRepository.save(order);
    }

    @Transactional
    public void markPaymentFailed(Long orderId) {
        Order order = getOrderById(orderId);
        order.setPaymentStatus("FAILED");
        orderRepository.save(order);
    }

    private Customer findOrCreateCustomer(String customerName, String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }
        return customerRepository.findByPhone(phone)
                .map(existing -> {
                    existing.setCustomerName(customerName);
                    return existing;
                })
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setPhone(phone);
                    customer.setCustomerName(customerName);
                    customer.setLoyaltyPoints(0);
                    return customer;
                });
    }
}
