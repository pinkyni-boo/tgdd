package com.hutech.demo.controller;

import com.hutech.demo.model.CartItem;
import com.hutech.demo.model.Order;
import com.hutech.demo.service.CartService;
import com.hutech.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private CartService cartService;

    @GetMapping("/orders") // Admin Route
    public String orderList(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "orders/order-list";
    }

    @GetMapping("/orders/detail/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrderById(id));
        return "orders/order-detail";
    }

    @PostMapping("/orders/confirm/{id}")
    public String confirmOrder(@PathVariable Long id) {
        orderService.confirmOrder(id);
        return "redirect:/orders/detail/" + id;
    }

    @GetMapping("/order/checkout") // Customer Route
    public String checkout(Model model) {
        // Pass cart items to checkout view if needed for display
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("totalAmount", cartService.getTotalAmount());
        return "cart/checkout";
    }

    @PostMapping("/order/submit")
    public String submitOrder(@RequestParam("customerName") String customerName,
                              @RequestParam(value = "phone", required = false) String phone,
                              @RequestParam(value = "address", required = false) String address,
                              @RequestParam(value = "payment", required = false) String payment,
                              @RequestParam(value = "useLoyaltyPoints", defaultValue = "false") boolean useLoyaltyPoints) {
        List<CartItem> cartItems = cartService.getCartItems();
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }
        orderService.createOrder(customerName, phone, address, payment, useLoyaltyPoints, cartItems);
        return "redirect:/order/confirmation";
    }

    @GetMapping("/order/confirmation")
    public String orderConfirmation(Model model) {
        model.addAttribute("message", "Đơn hàng đã được đặt thành công!");
        return "cart/order-confirmation";
    }

    @GetMapping("/order/history")
    public String orderHistory(Model model) {
        return "orders/order-history";
    }

    @PostMapping("/order/history")
    public String searchOrderHistory(@RequestParam("customerName") String customerName, Model model) {
        List<Order> orders = orderService.getOrdersByCustomer(customerName);
        model.addAttribute("orders", orders);
        model.addAttribute("customerName", customerName);
        return "orders/order-history";
    }
}
