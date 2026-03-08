package com.hutech.demo.runner;

import com.hutech.demo.model.Product;
import com.hutech.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import java.util.List;
import java.util.Random;

// @Component — disabled: no longer seeds demo data on startup
// @Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        seedCategories(); // Seed categories structure
        seedProductsData(); // Seed products if empty
        fixProductImages(); // Update existing products with URLs if needed
        seedDiscounts();
    }
    
    private void fixProductImages() {
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            // Update if local OR placeholder
            boolean needsUpdate = p.getImage() == null 
                               || !p.getImage().startsWith("http")
                               || p.getImage().contains("placehold.co");
            
            if (needsUpdate) {
                String name = p.getName() != null ? p.getName() : "Product";
                String encoded = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
                // Use a real photo service (Picsum) instead of text
                String url = "https://picsum.photos/seed/" + encoded + "/600/600";
                p.setImage(url);
                productRepository.save(p);
                System.out.println("Auto-updated image for: " + name);
            }
        }
    }

    @Autowired
    private com.hutech.demo.repository.CategoryRepository categoryRepository;

    private void seedCategories() {
        // Check if "Phụ kiện" already exists to avoid duplication of CATEGORIES
        if (categoryRepository.findByName("Phụ kiện").isPresent()) {
            return;
        }

        // 1. Create Parent: Phụ kiện
        com.hutech.demo.model.Category phuKien = new com.hutech.demo.model.Category();
        phuKien.setName("Phụ kiện");
        phuKien.setIcon("fa-solid fa-headphones");
        phuKien = categoryRepository.save(phuKien);

        // 2. Create Sub-categories
        createSubCategory("Sạc dự phòng", "fa-solid fa-battery-full", phuKien);
        createSubCategory("Sạc, cáp", "fa-solid fa-plug", phuKien);
        createSubCategory("Tai nghe", "fa-solid fa-headphones-simple", phuKien);
        createSubCategory("Loa", "fa-solid fa-volume-high", phuKien);
        createSubCategory("Thiết bị nhà thông minh", "fa-solid fa-house-signal", phuKien);
        createSubCategory("Camera", "fa-solid fa-video", phuKien);
        createSubCategory("Chuột, bàn phím", "fa-solid fa-keyboard", phuKien);
        createSubCategory("Thiết bị mạng", "fa-solid fa-wifi", phuKien);
        
        System.out.println("DataSeeder: Seeded Accessories categories.");
    }

    private void seedProductsData() {
        // Force seed every time (or just when running) to ensure images are updated
        // seedFullCatalog(); 
        
        // BETTER: Check if catalog needs reset or just run it. 
        // User wants "DELETE OLD DATA".
        // seedFullCatalog() now has deleteAll() inside it.
        seedFullCatalog();
    }

    private void seedFullCatalog() {
        // CANNOT DELETE ALL because of Foreign Keys (Orders)
        // productRepository.deleteAll(); 
        
        // Instead, we update existing ones or create new ones
        
        // Phones
        com.hutech.demo.model.Category phone = getOrCreateCategory("Phone");
        if (phone.getIcon() == null) { phone.setIcon("fa-solid fa-mobile-screen-button"); categoryRepository.save(phone); }
        
        createOrUpdateProduct("iPhone 15 Pro Max", 34990000, "Titan Nature", "https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg", phone, true);
        createOrUpdateProduct("Samsung Galaxy S24 Ultra", 33990000, "Grey Titanium", "https://cdn.tgdd.vn/Products/Images/42/307172/samsung-galaxy-s24-ultra-grey-thumbnew-600x600.jpg", phone, true);
        createOrUpdateProduct("OPPO Reno10 5G", 9990000, "Blue", "https://cdn.tgdd.vn/Products/Images/42/309795/oppo-reno10-blue-thumbnew-600x600.jpg", phone, false);
        createOrUpdateProduct("Xiaomi Redmi Note 13", 4890000, "Gold", "https://cdn.tgdd.vn/Products/Images/42/309831/xiaomi-redmi-note-13-gold-thumbnew-600x600.jpg", phone, false);

        // Laptops
        com.hutech.demo.model.Category laptop = getOrCreateCategory("Laptop");
        if (laptop.getIcon() == null) { laptop.setIcon("fa-solid fa-laptop"); categoryRepository.save(laptop); }
        createOrUpdateProduct("MacBook Air M2", 27090000, "Gray", "https://cdn.tgdd.vn/Products/Images/44/309458/macbook-air-15-inch-m2-2023-gray-thumb-600x600.jpg", laptop, true);
        
        // Tablets
        com.hutech.demo.model.Category tablet = getOrCreateCategory("Tablet");
        if (tablet.getIcon() == null) { tablet.setIcon("fa-solid fa-tablet-screen-button"); categoryRepository.save(tablet); }
        
        // Watches
        com.hutech.demo.model.Category watch = getOrCreateCategory("Watch");
        if (watch.getIcon() == null) { watch.setIcon("fa-solid fa-clock"); categoryRepository.save(watch); }
        
        // Test Randoms
        com.hutech.demo.model.Category other = getOrCreateCategory("Other");
        if (other.getIcon() == null) { other.setIcon("fa-solid fa-layer-group"); categoryRepository.save(other); }
        createOrUpdateProduct("dasdasdas", 123000, "Test", "https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg", other, false); 
        createOrUpdateProduct("Sản phẩm test", 500000, "Test", "https://cdn.tgdd.vn/Products/Images/42/307172/samsung-galaxy-s24-ultra-grey-thumbnew-600x600.jpg", other, false);
        
        // Finally run the generic fixer for any other products not listed above
        fixProductImages();
    }

    private com.hutech.demo.model.Category getOrCreateCategory(String name) {
        return categoryRepository.findByName(name)
            .orElseGet(() -> {
                com.hutech.demo.model.Category c = new com.hutech.demo.model.Category();
                c.setName(name);
                return categoryRepository.save(c);
            });
    }

    private void createOrUpdateProduct(String name, double price, String description, String imageUrl, com.hutech.demo.model.Category category, boolean isPromotional) {
        com.hutech.demo.model.Product existing = productRepository.findAll().stream()
                .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        
        if (existing != null) {
            existing.setPrice(price);
            existing.setDescription(description);
            existing.setImage(imageUrl);
            existing.setCategory(category);
            existing.setPromotional(isPromotional); // Update promotional flag
            productRepository.save(existing);
        } else {
            com.hutech.demo.model.Product product = new com.hutech.demo.model.Product();
            product.setName(name);
            product.setPrice(price);
            product.setDescription(description);
            product.setImage(imageUrl);
            product.setCategory(category);
            product.setPromotional(isPromotional); // Set promotional flag
            productRepository.save(product);
        }
    }
    




    private void createSubCategory(String name, String icon, com.hutech.demo.model.Category parent) {
        com.hutech.demo.model.Category cat = new com.hutech.demo.model.Category();
        cat.setName(name);
        cat.setIcon(icon);
        cat.setParent(parent);
        categoryRepository.save(cat);
    }

    private void seedDiscounts() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) return;

        Random random = new Random();
        boolean updated = false;

        for (Product product : products) {
            // If discount is 0, assign a random discount for demo purposes
            if (product.getDiscount() == 0) {
                // 50% chance to be on sale
                if (random.nextBoolean()) {
                    int discount = (random.nextInt(5) + 1) * 10; // 10, 20, 30, 40, 50%
                    product.setDiscount(discount);
                    updated = true;
                }
            }
        }

        if (updated) {
            productRepository.saveAll(products);
            System.out.println("DataSeeder: Updated products with random discounts for demo.");
        }
    }
}
