package com.hutech.demo.runner;

import com.hutech.demo.model.Category;
import com.hutech.demo.model.Product;
import com.hutech.demo.repository.CategoryRepository;
import com.hutech.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the 11 homepage flash-sale products and required categories on startup.
 * Safe to run multiple times — skips seeding if products already exist.
 */
@Component
@Order(10)
public class HomepageProductSeeder implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        // Skip if flash-sale products already exist
        if (!productRepository.findByIsPromotionalTrue().isEmpty()) {
            System.out.println("HomepageProductSeeder: Flash Sale products already exist, skipping.");
            return;
        }

        System.out.println("HomepageProductSeeder: Seeding homepage products...");

        // ── 1. Ensure top-level categories exist ─────────────────────────────
        Category dienThoai = findOrCreate("Điện Thoại", "fa-solid fa-mobile-screen-button", null);
        Category laptop    = findOrCreate("Laptop",     "fa-solid fa-laptop",                null);
        Category dongHo    = findOrCreate("Đồng Hồ",   "fa-solid fa-clock",                 null);
        Category phuKien   = findOrCreate("Phụ kiện",  "fa-solid fa-headphones",             null);
        Category pc        = findOrCreate("PC, Máy tính", "fa-solid fa-desktop",             null);

        // ── 2. Ensure relevant sub-categories exist ───────────────────────────
        Category loaCat     = findOrCreate("Loa",       "fa-solid fa-volume-high",      phuKien);
        Category taiNgheCat = findOrCreate("Tai nghe",  "fa-solid fa-headphones-simple", phuKien);

        // ── 3. Seed the 11 flash-sale products ───────────────────────────────
        seed("Samsung Galaxy S24 FE 5G 8GB/256GB",
                12_790_000, 29, true, dienThoai,
                "https://cdn.tgdd.vn/2026/03/timerseo/329785-600x600-1.jpg",
                "Điện thoại Samsung Galaxy S24 FE 5G, RAM 8GB, bộ nhớ 256GB.");

        seed("OPPO Reno13 F 5G 8GB/256GB",
                7_310_000, 25, true, dienThoai,
                "https://cdn.tgdd.vn/2026/03/timerseo/332936.jpg",
                "Điện thoại OPPO Reno13 F 5G, RAM 8GB, bộ nhớ 256GB.");

        seed("Xiaomi Redmi Note 14 Pro 5G 8GB/256GB",
                7_220_000, 22, true, dienThoai,
                "https://cdn.tgdd.vn/2026/03/timerseo/332938.jpg",
                "Điện thoại Xiaomi Redmi Note 14 Pro 5G, RAM 8GB, bộ nhớ 256GB.");

        seed("MacBook Pro 14 inch Nano M5 16GB/512GB",
                42_190_000, 7, true, laptop,
                "https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358093/macbook-pro-14-inch-nano-m5-16gb-512gb-den-638962956060321611-600x600.jpg",
                "Apple MacBook Pro 14 inch chip M5 Nano, RAM 16GB, SSD 512GB.");

        seed("ELIO 28 mm Nữ EL148-04",
                368_000, 50, true, dongHo,
                "https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/332294/elio-el148-04-nu-thumb-638684048531790119-600x600.jpg",
                "Đồng hồ ELIO nữ EL148-04, mặt kính 28mm.");

        seed("HP 15 fd1043TU Core 5 120U (9Z2W9PA)",
                18_990_000, 0, true, laptop,
                "https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341620/hp-15-fd1043tu-core-5-120u-9z2w9pa-040825-020241-865-600x600.jpg",
                "Laptop HP 15 fd1043TU Intel Core 5 120U, RAM 8GB, SSD 256GB.");

        seed("MiniPC SingPC NUC U512U695-W Ultra 5 125U",
                15_790_000, 12, true, pc,
                "https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/5698/357648/minipc-singpc-nuc-u512u695-w-ultra-5-125u-thumb-638960494675053851-600x600.jpg",
                "MiniPC SingPC NUC Intel Ultra 5 125U, RAM 16GB, SSD 512GB.");

        seed("Loa Bluetooth Monster Musicbox MS22150",
                1_155_000, 38, true, loaCat,
                "https://cdn.tgdd.vn/Products/Images/2162/324098/loa-bluetooth-monster-musicbox-ms22150-thumb-600x600.jpg",
                "Loa Bluetooth Monster Musicbox MS22150, âm thanh 360 độ.");

        seed("Tai nghe TWS Havit TW986",
                230_000, 52, true, taiNgheCat,
                "https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/54/333835/tai-nghe-tws-havit-tw986-060125-012911-718-600x600.jpg",
                "Tai nghe TWS Havit TW986, chống ồn, thời lượng pin 6 giờ.");

        seed("Garmin Forerunner 55 42mm dây silicone",
                2_590_000, 13, true, dongHo,
                "https://cdn.tgdd.vn/Products/Images/7077/244296/garmin-forerunner-55-day-silicone-den-tn-1-2-600x600.jpg",
                "Đồng hồ thể thao Garmin Forerunner 55, GPS, theo dõi nhịp tim.");

        seed("G-SHOCK 2100 40.2mm Nữ GMA-P2100ST-4ADR",
                2_983_000, 33, true, dongHo,
                "https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/330397/g-shock-gma-p2100st-4adr-nu-thumb-638815187076008715-600x600.jpg",
                "Đồng hồ G-SHOCK 2100 dây nhựa nữ GMA-P2100ST-4ADR.");

        System.out.println("HomepageProductSeeder: Done seeding 11 flash-sale products.");
    }

    private Category findOrCreate(String name, String icon, Category parent) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            Category c = new Category();
            c.setName(name);
            c.setIcon(icon);
            c.setParent(parent);
            return categoryRepository.save(c);
        });
    }

    private void seed(String name, double price, double discount,
                      boolean isFlashSale, Category category, String image, String description) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setDiscount(discount);
        p.setPromotional(isFlashSale);
        p.setPromotionQuantity(isFlashSale ? 12 : 0);
        if (discount > 0 && discount < 100) {
            double originalPrice = Math.round(price * 10000d / (100d - discount)) / 100d;
            p.setOriginalPrice(originalPrice);
        } else {
            p.setOriginalPrice(price);
        }
        p.setCategory(category);
        p.setImage(image);
        p.setDescription(description);
        productRepository.save(p);
    }
}
