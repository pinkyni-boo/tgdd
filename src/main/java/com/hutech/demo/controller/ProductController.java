package com.hutech.demo.controller;

import com.hutech.demo.model.Product;
import com.hutech.demo.service.CategoryService;
import com.hutech.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;

    // ===== PRODUCT DETAIL PAGE (TGDD style) =====
    @GetMapping("/{id}")
    public String showProductDetail(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + id));
        model.addAttribute("product", product);

        // Related products (same category, exclude current)
        if (product.getCategory() != null) {
            List<Product> related = productService.getProductsByCategoryId(product.getCategory().getId())
                    .stream()
                    .filter(p -> !p.getId().equals(id))
                    .limit(10)
                    .collect(Collectors.toList());
            model.addAttribute("relatedProducts", related);
        } else {
            List<Product> related = productService.getAllProducts()
                    .stream()
                    .filter(p -> !p.getId().equals(id))
                    .limit(10)
                    .collect(Collectors.toList());
            model.addAttribute("relatedProducts", related);
        }
        return "products/product-detail";
    }

    // Display a list of all products (Admin View)
    @GetMapping
    public String showProductList(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "/products/product-list";
    }

    // For adding a new product
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "/products/add-product";
    }

    // Process the form for adding a new product
    @PostMapping("/add")
    public String addProduct(@Valid Product product, BindingResult result, 
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile, 
                             Model model) {
        System.out.println("Processing ADD product: " + product.getName());
        if (imageFile != null) {
            System.out.println("Image size: " + imageFile.getSize());
        }

        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "/products/add-product";
        }
        try {
            productService.addProduct(product, imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("categories", categoryService.getAllCategories());
            return "/products/add-product";
        }
        return "redirect:/products";
    }

    // For editing a product
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "/products/update-product";
    }

    // Process the form for updating a product
    @PostMapping("/update/{id}")
    public String updateProduct(@PathVariable Long id, @Valid Product product, 
                                BindingResult result, 
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                Model model) {
        System.out.println("Processing UPDATE product ID: " + id);
        if (imageFile != null) {
            System.out.println("Received image: " + imageFile.getOriginalFilename() + " Size: " + imageFile.getSize());
        } else {
            System.out.println("No image file received.");
        }

        if (result.hasErrors()) {
            product.setId(id);
            model.addAttribute("categories", categoryService.getAllCategories());
            // Log the errors
            System.out.println("Validation Errors for ID " + id + ":");
            result.getAllErrors().forEach(error -> {
                System.out.println(" - " + error.getDefaultMessage() + " (" + error.getObjectName() + ")");
            });
            
            return "/products/update-product";
        }
        try {
            productService.updateProduct(product, imageFile);
            System.out.println("Update successful for ID: " + id);
        } catch (Exception e) {
            System.err.println("FATAL ERROR updating product: " + e.getMessage());
            e.printStackTrace();
            product.setId(id);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "/products/update-product";
        }
        return "redirect:/products";
    }

    // Handle request to delete a product
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProductById(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa sản phẩm này vì đã có trong đơn hàng.");
        }
        return "redirect:/products";
    }
    // Manual trigger to fix images
    @GetMapping("/fix-images")
    public String fixImages() {
        productService.fixProductImages();
        return "redirect:/products";
    }
}
