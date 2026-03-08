package com.hutech.demo.runner;

import com.hutech.demo.model.Product;
import com.hutech.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class DataCleanup implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) {
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            String img = p.getImage();
            boolean broken = img == null
                    || img.isBlank()
                    || img.contains("cdn.tgdd.vn")
                    || img.contains("picsum.photos");
            if (broken) {
                try {
                    productRepository.delete(p);
                    System.out.println("DataCleanup: Deleted product with broken image — " + p.getName());
                } catch (Exception e) {
                    System.out.println("DataCleanup: Cannot delete '" + p.getName() + "' (has orders). Skipped.");
                }
            }
        }
    }
}
