package com.hutech.demo.service;

import com.hutech.demo.model.Product;
import com.hutech.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    // Retrieve all products from the database
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Retrieve promotional products (isPromotional = true)
    public List<Product> getPromotionalProducts() {
        return productRepository.findByIsPromotionalTrue();
    }

    // Retrieve products by category
    public List<Product> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    // Retrieve a product by its id
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // Add a new product to the database
    public Product addProduct(Product product, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String newImageFile = saveImage(imageFile);
            product.setImage(newImageFile);
        }
        
        // Auto-generate image if missing (Lazy Mode)
        if (product.getImage() == null || product.getImage().isEmpty()) {
            String productName = product.getName() != null ? product.getName() : "Product";
            String encodedName = java.net.URLEncoder.encode(productName, java.nio.charset.StandardCharsets.UTF_8);
            product.setImage("https://placehold.co/600x600/png?text=" + encodedName);
        }
        
        return productRepository.save(product);
    }

    // Update an existing product
    public Product updateProduct(@NotNull Product product, MultipartFile imageFile) throws IOException {
        Product existingProduct = productRepository.findById(product.getId())
                .orElseThrow(() -> new IllegalStateException("Product with ID " + product.getId() + " does not exist."));
        existingProduct.setName(product.getName());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setDiscount(product.getDiscount());
        existingProduct.setPromotional(product.isPromotional()); // Update flag
        
        if (imageFile != null && !imageFile.isEmpty()) {
             String newImageFile = saveImage(imageFile);
             existingProduct.setImage(newImageFile);
        } else if (product.getImage() != null && !product.getImage().isEmpty()) {
            // User provided a URL or kept existing image string
            existingProduct.setImage(product.getImage());
        }
        
        // Auto-generate if still empty (though update usually preserves old image, but if user cleared it?)
        if (existingProduct.getImage() == null || existingProduct.getImage().isEmpty()) {
             String productName = existingProduct.getName() != null ? existingProduct.getName() : "Product";
             String encodedName = java.net.URLEncoder.encode(productName, java.nio.charset.StandardCharsets.UTF_8);
             existingProduct.setImage("https://placehold.co/600x600/png?text=" + encodedName);
        }

        return productRepository.save(existingProduct);
    }

    // Delete a product by its id
    public void deleteProductById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalStateException("Product with ID " + id + " does not exist.");
        }
        productRepository.deleteById(id);
    }

    private String saveImage(MultipartFile imageFile) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
        
        // Save to "uploads" folder in project root (Absolute Path)
        Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");

        System.out.println("Attempting to save image to: " + uploadPath.toAbsolutePath());

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created uploads directory");
            }

            // Save file
            Files.copy(imageFile.getInputStream(), uploadPath.resolve(fileName));
            System.out.println("Saved image successfully: " + fileName);
            
        } catch (IOException e) {
            System.err.println("Error saving image: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Could not save image file: " + fileName, e);
        }
        return fileName;
    }
    public void fixProductImages() {
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            // Check for missing, local, or placeholder images
            boolean needsUpdate = p.getImage() == null 
                               || !p.getImage().startsWith("http")
                               || p.getImage().contains("placehold.co")
                               || p.getImage().contains("picsum.photos"); // Also update picsum to the new better ones
            
            if (needsUpdate) {
                String url = getAutoImage(p.getName());
                p.setImage(url);
                productRepository.save(p);
            }
        }
    }

    // --- BỘ MÁY TỰ ĐỘNG GẮN ẢNH (SMART AUTO IMAGE) ---
    private String getAutoImage(String productName) {
        if (productName == null) return "https://cdn.tgdd.vn/Products/Images/42/303591/vivo-v27e-thumb-600x600.jpg";
        String name = productName.toLowerCase();
        
        // Link ảnh thật từ Thế Giới Di Động
        String imgIphone = "https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg";
        String imgSamsung = "https://cdn.tgdd.vn/Products/Images/42/307172/samsung-galaxy-s24-ultra-grey-thumbnew-600x600.jpg";
        String imgOppo = "https://cdn.tgdd.vn/Products/Images/42/309795/oppo-reno10-blue-thumbnew-600x600.jpg";
        String imgXiaomi = "https://cdn.tgdd.vn/Products/Images/42/309831/xiaomi-redmi-note-13-gold-thumbnew-600x600.jpg";
        String imgMacbook = "https://cdn.tgdd.vn/Products/Images/44/309458/macbook-air-15-inch-m2-2023-gray-thumb-600x600.jpg";
        String imgIpad = "https://cdn.tgdd.vn/Products/Images/52/299033/ipad-pro-m2-11-inch-wifi-grey-thumb-600x600.jpg";
        String imgWatch = "https://cdn.tgdd.vn/Products/Images/54/314666/apple-watch-s9-gps-41mm-vien-nhom-day-silicone-xanh-den-thumb-1-600x600.jpg";
        String imgMouse = "https://cdn.tgdd.vn/Products/Images/86/302384/chuot-bluetooth-silent-logitech-m240-den-thumb-600x600.jpg";

        // Logic kiểm tra tên
        if (name.contains("iphone")) return imgIphone;
        if (name.contains("samsung") || name.contains("galaxy")) return imgSamsung;
        if (name.contains("oppo")) return imgOppo;
        if (name.contains("xiaomi") || name.contains("redmi")) return imgXiaomi;
        if (name.contains("macbook") || name.contains("laptop")) return imgMacbook;
        if (name.contains("ipad")) return imgIpad;
        if (name.contains("watch")) return imgWatch;
        if (name.contains("chuột") || name.contains("mouse")) return imgMouse;

        // --- QUAN TRỌNG: NẾU TÊN KHÔNG KHỚP (Vd: dasdasdas) ---
        // Random trả về 1 trong các ảnh trên để không bị lỗi hình đám mây
        String[] backupImages = {imgIphone, imgSamsung, imgOppo, imgXiaomi};
        int index = name.length() % backupImages.length;
        return backupImages[index];
    }
}
