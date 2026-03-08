package com.hutech.demo.service;

import com.hutech.demo.model.CartItem;
import com.hutech.demo.model.Customer;
import com.hutech.demo.model.Order;
import com.hutech.demo.model.OrderDetail;
import com.hutech.demo.repository.CustomerRepository;
import com.hutech.demo.repository.OrderDetailRepository;
import com.hutech.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private CustomerRepository customerRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByCustomer(String customerName) {
        return orderRepository.findByCustomerName(customerName);
    }
    
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    @Transactional
    public void confirmOrder(Long id) {
        Order order = getOrderById(id);
        order.setStatus("CONFIRMED");
        orderRepository.save(order);
    }

    @Transactional
    public Order createOrder(String customerName, String phone, String address, String payment,
                             boolean useLoyaltyPoints, List<CartItem> cartItems) {
        double totalAmount = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();

        // Find or create customer by phone
        Customer customer = customerRepository.findByPhone(phone).orElse(null);
        if (customer == null) {
            customer = new Customer();
            customer.setPhone(phone);
            customer.setCustomerName(customerName);
            customer.setLoyaltyPoints(0);
        } else {
            customer.setCustomerName(customerName);
        }

        // Apply loyalty points discount if requested
        long pointsUsed = 0;
        if (useLoyaltyPoints && customer.getLoyaltyPoints() > 0) {
            pointsUsed = Math.min(customer.getLoyaltyPoints(), (long) totalAmount);
            totalAmount -= pointsUsed;
            customer.setLoyaltyPoints(customer.getLoyaltyPoints() - pointsUsed);
        }

        // Earn new points from this order (1 point per 1000đ spent)
        long newPoints = (long) (totalAmount / 1000);
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + newPoints);
        customerRepository.save(customer);

        Order order = new Order();
        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setAddress(address);
        order.setPayment(payment);
        order.setTotalAmount(totalAmount);
        order.setLoyaltyPointsUsed(pointsUsed);
        order.setLoyaltyPointsEarned(newPoints);
        order = orderRepository.save(order);

        for (CartItem item : cartItems) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());
            orderDetailRepository.save(detail);
        }

        cartService.clearCart();
        return order;
    }
}
