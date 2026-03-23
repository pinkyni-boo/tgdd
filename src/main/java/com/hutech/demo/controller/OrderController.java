package com.hutech.demo.controller;

import com.hutech.demo.model.CartItem;
import com.hutech.demo.model.Order;
import com.hutech.demo.service.CartService;
import com.hutech.demo.service.OrderService;
import com.hutech.demo.service.UserService;
import com.hutech.demo.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final VnPayService vnPayService;
    private final UserService userService;

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
    public String checkout(Model model, Authentication authentication) {
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("totalAmount", cartService.getTotalAmount());
        model.addAttribute("shippingFee", cartService.calculateShippingFee());
        model.addAttribute("payableAmount", cartService.getPayableAmount());
        model.addAttribute("totalQuantity", cartService.getTotalQuantity());
        model.addAttribute("vnpayEnabled", vnPayService.isConfigured());
        model.addAttribute("hideFooter", true);
        if (isRealAuthenticated(authentication)) {
            var user = userService.getDomainUserByUsername(authentication.getName());
            model.addAttribute("loggedPhone", user.getPhone());
            model.addAttribute("loggedUsername", user.getUsername());
        }
        return "cart/checkout";
    }

    @PostMapping("/order/submit")
    public String submitOrder(@RequestParam("customerName") String customerName,
                              @RequestParam(value = "phone", required = false) String phone,
                              @RequestParam(value = "address", required = false) String address,
                              @RequestParam(value = "payment", required = false) String payment,
                              @RequestParam(value = "vnpayMethod", required = false) String vnpayMethod,
                              @RequestParam(value = "useLoyaltyPoints", defaultValue = "false") boolean useLoyaltyPoints,
                              @RequestParam(value = "voucherCode", required = false) String voucherCode,
                              Authentication authentication,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        List<CartItem> cartItems = cartService.getCartItems();
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        if (isRealAuthenticated(authentication)) {
            var user = userService.getDomainUserByUsername(authentication.getName());
            if (user.getPhone() != null && !user.getPhone().isBlank()) {
                phone = user.getPhone();
            }
            if (customerName == null || customerName.isBlank()) {
                customerName = user.getUsername();
            }
        }

        if (payment == null || payment.isBlank()) {
            payment = "COD";
        }

        try {
            Order order = orderService.createOrder(
                    customerName, phone, address, payment, useLoyaltyPoints, voucherCode, cartItems);

            if ("VNPAY".equalsIgnoreCase(payment)) {
                if (!vnPayService.isConfigured()) {
                    orderService.markAsPaid(order.getId());
                    redirectAttributes.addFlashAttribute(
                            "message", "VNPay simulation successful for order #" + order.getId() + ".");
                    return "redirect:/order/confirmation?orderId=" + order.getId();
                }
                String paymentUrl = vnPayService.createPaymentUrl(order, resolveClientIp(request), vnpayMethod);
                return "redirect:" + paymentUrl;
            }

            redirectAttributes.addFlashAttribute(
                    "message", "Order #" + order.getId() + " has been created successfully.");
            return "redirect:/order/confirmation?orderId=" + order.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/order/checkout";
        }
    }

    @GetMapping("/payment/vnpay-return")
    public String handleVnpayReturn(@RequestParam Map<String, String> queryParams,
                                    RedirectAttributes redirectAttributes) {
        Long orderId = parseOrderId(queryParams.get("vnp_TxnRef"));
        if (orderId == null) {
            redirectAttributes.addFlashAttribute("error", "Không xác định được đơn hàng từ phản hồi VNPay.");
            return "redirect:/order/confirmation";
        }

        boolean validSignature = vnPayService.verifyReturn(queryParams);
        if (!validSignature) {
            orderService.markPaymentFailed(orderId);
            redirectAttributes.addFlashAttribute("error", "Chữ ký VNPay không hợp lệ.");
            return "redirect:/order/confirmation?orderId=" + orderId;
        }

        String responseCode = queryParams.getOrDefault("vnp_ResponseCode", "");
        if ("00".equals(responseCode)) {
            orderService.markAsPaid(orderId);
            redirectAttributes.addFlashAttribute("message", "Thanh toán VNPay thành công cho đơn #" + orderId + ".");
        } else {
            orderService.markPaymentFailed(orderId);
            redirectAttributes.addFlashAttribute("error", "Thanh toán VNPay thất bại (mã: " + responseCode + ").");
        }

        return "redirect:/order/confirmation?orderId=" + orderId;
    }

    @GetMapping("/order/confirmation")
    public String orderConfirmation(@RequestParam(value = "orderId", required = false) Long orderId,
                                    Model model) {
        if (orderId != null) {
            model.addAttribute("order", orderService.getOrderById(orderId));
        }
        if (!model.containsAttribute("message") && !model.containsAttribute("error")) {
            model.addAttribute("message", "Đơn hàng của bạn đã được tạo thành công.");
        }
        return "cart/order-confirmation";
    }

    @GetMapping("/order/history")
    public String orderHistory(Model model) {
        return "orders/order-history";
    }

    @PostMapping("/order/history")
    public String searchOrderHistory(@RequestParam("keyword") String keyword, Model model) {
        String normalized = keyword == null ? "" : keyword.trim();
        List<Order> orders;
        if (normalized.matches("\\d+")) {
            orders = orderService.getOrdersByPhone(normalized);
        } else {
            orders = orderService.getOrdersByCustomer(normalized);
        }
        model.addAttribute("orders", orders);
        model.addAttribute("keyword", normalized);
        return "orders/order-history";
    }

    @GetMapping("/order/history/detail/{id}")
    public String orderHistoryDetail(@PathVariable Long id,
                                     @RequestParam("phone") String phone,
                                     @RequestParam(value = "source", required = false) String source,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        String normalizedPhone = phone == null ? "" : phone.trim();
        if (!normalizedPhone.matches("\\d{9,12}")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại không hợp lệ.");
            return "redirect:/order/history";
        }

        try {
            model.addAttribute("order", orderService.getOrderByIdAndPhone(id, normalizedPhone));
            model.addAttribute("isLookupView", true);
            model.addAttribute("backPhone", normalizedPhone);
            model.addAttribute("backToLoyalty", "loyalty".equalsIgnoreCase(source));
            return "orders/order-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng theo số điện thoại đã nhập.");
            return "redirect:/order/history";
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private Long parseOrderId(String txnRef) {
        if (txnRef == null || txnRef.isBlank()) {
            return null;
        }

        String value = txnRef.trim();
        if (value.startsWith("OD")) {
            value = value.substring(2);
        }

        int underscore = value.indexOf('_');
        String numericPart = underscore >= 0 ? value.substring(0, underscore) : value;
        try {
            return Long.parseLong(numericPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isRealAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
