package com.hutech.demo.controller;

import com.hutech.demo.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public String showCart(Model model) {
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("totalAmount", cartService.getTotalAmount());
        model.addAttribute("shippingFee", cartService.calculateShippingFee());
        model.addAttribute("payableAmount", cartService.getPayableAmount());
        model.addAttribute("totalQuantity", cartService.getTotalQuantity());
        model.addAttribute("hideFooter", true);
        return "cart/cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId, @RequestParam int quantity) {
        cartService.addToCart(productId, quantity);
        return "redirect:/cart";
    }

    @GetMapping("/add")
    public String addToCartGet(@RequestParam Long productId, @RequestParam int quantity) {
        cartService.addToCart(productId, quantity);
        return "redirect:/cart";
    }

    @GetMapping("/buy-now")
    public String buyNow(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity) {
        cartService.addToCart(productId, quantity);
        return "redirect:/order/checkout";
    }

    @PostMapping("/update")
    public String updateCart(@RequestParam Long productId, @RequestParam int quantity,
                             @RequestParam(value = "redirectTo", required = false) String redirectTo) {
        cartService.updateQuantity(productId, quantity);
        if ("/order/checkout".equals(redirectTo)) {
            return "redirect:/order/checkout";
        }
        return "redirect:/cart";
    }

    @GetMapping("/remove/{productId}")
    public String removeFromCart(@PathVariable Long productId,
                                 @RequestParam(value = "redirectTo", required = false) String redirectTo) {
        cartService.removeFromCart(productId);
        if ("/order/checkout".equals(redirectTo)) {
            return "redirect:/order/checkout";
        }
        return "redirect:/cart";
    }

    @GetMapping("/clear")
    public String clearCart() {
        cartService.clearCart();
        return "redirect:/cart";
    }

    @PostMapping("/api/update")
    @ResponseBody
    public Map<String, Object> apiUpdateCart(@RequestParam Long productId, @RequestParam int quantity) {
        cartService.updateQuantity(productId, quantity);
        int lineCount = cartService.getCartItems().size();
        int totalQuantity = cartService.getTotalQuantity();
        return Map.of(
                "subtotalAmount", cartService.getTotalAmount(),
                "shippingFee", cartService.calculateShippingFee(),
                "payableAmount", cartService.getPayableAmount(),
                "totalQuantity", totalQuantity,
                "itemCount", lineCount);
    }

    @PostMapping("/api/remove")
    @ResponseBody
    public Map<String, Object> apiRemoveFromCart(@RequestParam Long productId) {
        cartService.removeFromCart(productId);
        int lineCount = cartService.getCartItems().size();
        int totalQuantity = cartService.getTotalQuantity();
        return Map.of(
                "subtotalAmount", cartService.getTotalAmount(),
                "shippingFee", cartService.calculateShippingFee(),
                "payableAmount", cartService.getPayableAmount(),
                "totalQuantity", totalQuantity,
                "itemCount", lineCount);
    }
}
