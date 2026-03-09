package com.hutech.demo.runner;

import com.hutech.demo.model.Category;
import com.hutech.demo.model.Product;
import com.hutech.demo.repository.CategoryRepository;
import com.hutech.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Order(30)
@RequiredArgsConstructor
public class TabProductsSeeder implements CommandLineRunner {

    private static final int MIN_PRODUCTS_PER_TAB = 10;
    private static final int MAX_GENERATION_ATTEMPTS = 80;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        ensureStaticTabProducts();
        ensureRootCategoryTabsProducts();
    }

    private void ensureStaticTabProducts() {
        ensureTab(
                "dien-thoai",
                "Phone",
                "fa-solid fa-mobile-screen-button",
                List.of("dien thoai", "phone"),
                List.of(
                        "Samsung Galaxy A35 5G",
                        "iPhone 14 128GB",
                        "Xiaomi Redmi Note 13 Pro",
                        "OPPO Reno11 F 5G",
                        "vivo V30e 5G",
                        "realme 12 Plus 5G",
                        "Nokia G42 5G",
                        "HONOR X8b",
                        "Tecno Pova 6 Pro",
                        "Infinix Note 40 Pro"
                )
        );

        ensureTab(
                "apple",
                "Apple",
                "fa-brands fa-apple",
                List.of("apple", "iphone", "ipad", "macbook"),
                List.of(
                        "iPhone 15 128GB",
                        "iPhone 15 Pro 256GB",
                        "iPad Air M2 11 inch",
                        "iPad Gen 10 WiFi",
                        "MacBook Air M3 13 inch",
                        "MacBook Pro M3 14 inch",
                        "Apple Watch SE 2024",
                        "Apple Watch Series 9",
                        "AirPods Pro 2 USB-C",
                        "AirPods 3 Lightning"
                )
        );

        ensureTab(
                "laptop",
                "Laptop",
                "fa-solid fa-laptop",
                List.of("laptop", "notebook", "macbook"),
                List.of(
                        "Laptop Asus Vivobook 15",
                        "Laptop Acer Aspire 7",
                        "Laptop Dell Inspiron 15",
                        "Laptop HP Pavilion 14",
                        "Laptop Lenovo IdeaPad Slim 3",
                        "Laptop MSI Modern 14",
                        "Laptop Gigabyte G5",
                        "Laptop Huawei MateBook D16",
                        "Laptop LG Gram 14",
                        "MacBook Air M2 15 inch"
                )
        );

        ensureTab(
                "phu-kien",
                "Phụ kiện",
                "fa-solid fa-headphones",
                List.of("phu kien", "sac", "cap", "tai nghe", "loa", "camera", "chuot", "ban phim"),
                List.of(
                        "Tai nghe Bluetooth Havit W50",
                        "Tai nghe chụp tai Sony WH-CH520",
                        "Sạc nhanh Anker 33W",
                        "Cáp Type-C to C 100W",
                        "Sạc dự phòng 20000mAh",
                        "Loa Bluetooth JBL Go 4",
                        "Chuột không dây Logitech M331",
                        "Bàn phím cơ DareU EK87",
                        "Camera an ninh Xiaomi C300",
                        "Bộ chuyển đổi USB-C Hub 6 in 1"
                )
        );

        ensureTab(
                "dong-ho",
                "Đồng hồ",
                "fa-solid fa-clock",
                List.of("dong ho", "watch"),
                List.of(
                        "Đồng hồ thông minh Amazfit Bip 5",
                        "Smart Watch Huawei Watch Fit 3",
                        "Đồng hồ Garmin Forerunner 165",
                        "Đồng hồ Casio MTP-V300",
                        "Đồng hồ nữ Elio EL120",
                        "Watch Kieslect Calling KR",
                        "Smart Watch Redmi Watch 4",
                        "Đồng hồ thể thao Mibro Lite 3",
                        "Đồng hồ thông minh Samsung Galaxy Watch 6",
                        "Đồng hồ OPPO Watch Free"
                )
        );

        ensureTab(
                "pc-may-in",
                "PC, Máy in",
                "fa-solid fa-desktop",
                List.of("pc", "may in", "desktop", "mini pc"),
                List.of(
                        "PC Gaming Ryzen 5 RTX 4060",
                        "PC Văn phòng Intel i5 12400",
                        "Mini PC Intel NUC i7",
                        "Máy in Canon LBP2900",
                        "Máy in HP LaserJet M211dw",
                        "PC All-in-one Asus A3402",
                        "PC Mini Lenovo ThinkCentre",
                        "Máy in Brother DCP-T420W",
                        "PC Đồ họa i7 RTX 4070",
                        "Máy in Epson EcoTank L3250"
                )
        );
    }

    private void ensureRootCategoryTabsProducts() {
        List<Category> rootCategories = categoryRepository.findByParentIsNull();
        for (Category root : rootCategories) {
            if (root == null || root.getId() == null || root.getName() == null || root.getName().isBlank()) {
                continue;
            }

            int current = countProductsInCategoryTree(root);
            if (current >= MIN_PRODUCTS_PER_TAB) {
                continue;
            }

            int need = MIN_PRODUCTS_PER_TAB - current;
            for (int i = 1; i <= need; i++) {
                String name = root.getName() + " Mẫu " + String.format("%02d", current + i);
                if (productRepository.existsByNameIgnoreCase(name)) {
                    continue;
                }
                productRepository.save(buildProduct(name, root, root.getName()));
            }
        }
    }

    private void ensureTab(String slug,
                           String categoryName,
                           String iconClass,
                           List<String> keywords,
                           List<String> preferredNames) {
        Category category = findOrCreateCategory(categoryName, iconClass);
        int currentCount = countProductsByKeywords(keywords);
        if (currentCount >= MIN_PRODUCTS_PER_TAB) {
            return;
        }

        int needed = MIN_PRODUCTS_PER_TAB - currentCount;
        int added = 0;
        int idx = 0;
        int attempts = 0;

        while (added < needed && attempts < MAX_GENERATION_ATTEMPTS) {
            attempts++;
            String candidateName;
            if (idx < preferredNames.size()) {
                candidateName = preferredNames.get(idx++);
            } else {
                candidateName = fallbackName(slug, categoryName, attempts);
            }

            if (candidateName == null || candidateName.isBlank()) {
                continue;
            }
            if (productRepository.existsByNameIgnoreCase(candidateName)) {
                continue;
            }

            productRepository.save(buildProduct(candidateName, category, categoryName));
            added++;
        }
    }

    private Category findOrCreateCategory(String name, String iconClass) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setIcon(iconClass);
            category.setParent(null);
            return categoryRepository.save(category);
        });
    }

    private int countProductsByKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return productRepository.findAll().size();
        }

        int count = 0;
        for (Product product : productRepository.findAll()) {
            if (matchesKeywords(product, keywords)) {
                count++;
            }
        }
        return count;
    }

    private boolean matchesKeywords(Product product, List<String> keywords) {
        if (product == null) {
            return false;
        }
        String name = normalize(product.getName());
        String categoryName = product.getCategory() == null ? "" : normalize(product.getCategory().getName());
        String parentName = "";
        if (product.getCategory() != null && product.getCategory().getParent() != null) {
            parentName = normalize(product.getCategory().getParent().getName());
        }

        for (String key : keywords) {
            if (name.contains(key) || categoryName.contains(key) || parentName.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private int countProductsInCategoryTree(Category root) {
        Set<Long> ids = new HashSet<>();
        collectCategoryIds(root, ids);
        if (ids.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Product product : productRepository.findAll()) {
            if (product.getCategory() != null && ids.contains(product.getCategory().getId())) {
                count++;
            }
        }
        return count;
    }

    private void collectCategoryIds(Category category, Set<Long> ids) {
        if (category == null || category.getId() == null || ids.contains(category.getId())) {
            return;
        }
        ids.add(category.getId());
        List<Category> children = categoryRepository.findByParent(category);
        for (Category child : children) {
            collectCategoryIds(child, ids);
        }
    }

    private Product buildProduct(String name, Category category, String hint) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setDescription("San pham tu dong bo sung cho tab " + hint);
        product.setPrice(randomPriceByHint(hint));
        product.setDiscount(randomDiscount());
        product.setPromotional(false);
        product.setPromotionQuantity(0);
        product.setOriginalPrice(product.getPrice());
        product.setImage(imageByHint(hint));
        return product;
    }

    private String fallbackName(String slug, String categoryName, int seq) {
        String normalizedSlug = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedSlug) {
            case "dien-thoai" -> "Phone mẫu tự động " + String.format("%02d", seq);
            case "apple" -> "Apple mẫu tự động " + String.format("%02d", seq);
            case "laptop" -> "Laptop mẫu tự động " + String.format("%02d", seq);
            case "phu-kien" -> "Phụ kiện mẫu tự động " + String.format("%02d", seq);
            case "dong-ho" -> "Đồng hồ mẫu tự động " + String.format("%02d", seq);
            case "pc-may-in" -> "PC mẫu tự động " + String.format("%02d", seq);
            default -> categoryName + " mẫu tự động " + String.format("%02d", seq);
        };
    }

    private double randomPriceByHint(String hint) {
        String normalized = normalize(hint);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (normalized.contains("phone") || normalized.contains("dien thoai") || normalized.contains("apple")) {
            return random.nextInt(4_990_000, 34_990_001);
        }
        if (normalized.contains("laptop")) {
            return random.nextInt(9_990_000, 44_990_001);
        }
        if (normalized.contains("dong ho") || normalized.contains("watch")) {
            return random.nextInt(390_000, 8_990_001);
        }
        if (normalized.contains("pc") || normalized.contains("may in") || normalized.contains("desktop")) {
            return random.nextInt(3_990_000, 39_990_001);
        }
        return random.nextInt(99_000, 5_990_001);
    }

    private double randomDiscount() {
        return ThreadLocalRandom.current().nextInt(0, 26);
    }

    private String imageByHint(String hint) {
        String normalized = normalize(hint);
        if (normalized.contains("apple")) {
            return "https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg";
        }
        if (normalized.contains("phone") || normalized.contains("dien thoai")) {
            return "https://cdn.tgdd.vn/Products/Images/42/307172/samsung-galaxy-s24-ultra-grey-thumbnew-600x600.jpg";
        }
        if (normalized.contains("laptop")) {
            return "https://cdn.tgdd.vn/Products/Images/44/309458/macbook-air-15-inch-m2-2023-gray-thumb-600x600.jpg";
        }
        if (normalized.contains("dong ho") || normalized.contains("watch")) {
            return "https://cdn.tgdd.vn/Products/Images/7077/244296/garmin-forerunner-55-day-silicone-den-tn-1-2-600x600.jpg";
        }
        if (normalized.contains("pc") || normalized.contains("desktop")) {
            return "https://cdn.tgdd.vn/Products/Images/5698/357648/minipc-singpc-nuc-u512u695-w-ultra-5-125u-thumb-638960494675053851-600x600.jpg";
        }
        if (normalized.contains("may in")) {
            return "https://cdn.tgdd.vn/Products/Images/4728/289205/canon-lbp2900-600x600.jpg";
        }
        return "https://cdn.tgdd.vn/Products/Images/86/302384/chuot-bluetooth-silent-logitech-m240-den-thumb-600x600.jpg";
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
