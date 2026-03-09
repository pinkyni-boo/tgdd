package com.hutech.demo.controller;

import com.hutech.demo.model.Product;
import com.hutech.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @GetMapping("/")
    public String showHomePage(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("flashSaleProducts", productService.getPromotionalProducts());
        return "home/home";
    }

    /**
     * AJAX endpoint: return products by category (or all if categoryId is missing/0)
     */
    @GetMapping("/api/products")
    @ResponseBody
    public List<Map<String, Object>> getProductsByCategory(
            @RequestParam(required = false) Long categoryId) {
        List<Product> products;
        if (categoryId != null && categoryId > 0) {
            products = productService.getProductsByCategoryId(categoryId);
        } else {
            products = productService.getAllProducts();
        }
        return products.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("price", p.getPrice());
            m.put("discount", p.getDiscount());
            m.put("image", p.getImage());
            m.put("categoryName", p.getCategory() != null ? p.getCategory().getName() : "");
            m.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : 0);
            m.put("promotional", p.isPromotional());
            m.put("promotionQuantity", p.getPromotionQuantity());
            m.put("originalPrice", p.getOriginalPrice());
            return m;
        }).collect(Collectors.toList());
    }
}
