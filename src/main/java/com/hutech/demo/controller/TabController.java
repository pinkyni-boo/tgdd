package com.hutech.demo.controller;

import com.hutech.demo.model.Category;
import com.hutech.demo.model.Product;
import com.hutech.demo.service.CategoryService;
import com.hutech.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class TabController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping("/tabs/category/{id}")
    public String showCategoryTab(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryService.getCategoryById(id).orElse(null);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Khong tim thay danh muc.");
            return "redirect:/";
        }

        Set<Long> ids = new HashSet<>();
        collectCategoryIds(category, ids);

        List<Product> products = productService.getAllProducts().stream()
                .filter(p -> p.getCategory() != null && ids.contains(p.getCategory().getId()))
                .toList();

        model.addAttribute("tabTitle", category.getName());
        model.addAttribute("products", products);
        return "home/tab-products";
    }

    @GetMapping("/tabs/{slug}")
    public String showStaticTab(@PathVariable String slug, Model model) {
        String normalizedSlug = normalize(slug).replace('_', '-');
        List<Product> products = filterProductsBySlug(normalizedSlug);
        model.addAttribute("tabTitle", resolveTabTitle(normalizedSlug));
        model.addAttribute("products", products);
        return "home/tab-products";
    }

    private List<Product> filterProductsBySlug(String slug) {
        if ("flash-sale".equals(slug)) {
            return productService.getPromotionalProducts();
        }
        if ("all".equals(slug)) {
            return productService.getAllProducts();
        }

        List<String> keywords = resolveKeywords(slug);
        if (keywords.isEmpty()) {
            return productService.getAllProducts();
        }

        List<Product> result = new ArrayList<>();
        for (Product product : productService.getAllProducts()) {
            String name = normalize(product.getName());
            String categoryName = product.getCategory() == null ? "" : normalize(product.getCategory().getName());
            String parentName = "";
            if (product.getCategory() != null && product.getCategory().getParent() != null) {
                parentName = normalize(product.getCategory().getParent().getName());
            }

            boolean matched = false;
            for (String key : keywords) {
                if (name.contains(key) || categoryName.contains(key) || parentName.contains(key)) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                result.add(product);
            }
        }
        return result;
    }

    private List<String> resolveKeywords(String slug) {
        return switch (slug) {
            case "dien-thoai" -> List.of("dien thoai", "phone");
            case "apple" -> List.of("apple", "iphone", "ipad", "macbook");
            case "laptop" -> List.of("laptop", "notebook", "macbook");
            case "phu-kien" -> List.of("phu kien", "sac", "cap", "tai nghe", "loa", "camera", "chuot", "ban phim");
            case "dong-ho" -> List.of("dong ho", "watch");
            case "pc-may-in" -> List.of("pc", "may in", "desktop", "mini pc");
            case "sac-cap" -> List.of("sac", "cap", "plug", "battery");
            default -> List.of();
        };
    }

    private String resolveTabTitle(String slug) {
        return switch (slug) {
            case "flash-sale" -> "Flash Sale";
            case "all" -> "Tat ca san pham";
            case "dien-thoai" -> "Dien thoai";
            case "apple" -> "Apple";
            case "laptop" -> "Laptop";
            case "phu-kien" -> "Phu kien";
            case "dong-ho" -> "Dong ho";
            case "pc-may-in" -> "PC, May in";
            case "sac-cap" -> "Sac, cap";
            default -> "San pham";
        };
    }

    private void collectCategoryIds(Category category, Set<Long> ids) {
        if (category == null || category.getId() == null || ids.contains(category.getId())) {
            return;
        }
        ids.add(category.getId());
        if (category.getChildren() != null) {
            for (Category child : category.getChildren()) {
                collectCategoryIds(child, ids);
            }
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{Alnum}\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
