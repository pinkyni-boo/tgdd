package com.hutech.demo.service;

import com.hutech.demo.model.Category;
import com.hutech.demo.model.Product;
import com.hutech.demo.repository.CategoryRepository;
import com.hutech.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public void addCategory(Category category) {
        categoryRepository.save(category);
    }

    public void updateCategory(@NotNull Category category) {
        Category existingCategory = categoryRepository.findById(category.getId())
                .orElseThrow(() -> new IllegalStateException("Category with ID " + category.getId() + " does not exist."));
        existingCategory.setName(category.getName());
        existingCategory.setParent(category.getParent());
        existingCategory.setIcon(category.getIcon());
        categoryRepository.save(existingCategory);
    }

    public void deleteCategoryById(Long id) {
        Category target = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Category with ID " + id + " does not exist."));

        Category parent = target.getParent();

        // Move products to parent category (or null if deleting a root category).
        List<Product> products = productRepository.findByCategoryId(id);
        for (Product product : products) {
            product.setCategory(parent);
        }
        if (!products.isEmpty()) {
            productRepository.saveAll(products);
        }

        // Re-parent direct children so they are not deleted with this category.
        List<Category> children = categoryRepository.findByParent(target);
        for (Category child : children) {
            child.setParent(parent);
        }
        if (!children.isEmpty()) {
            categoryRepository.saveAll(children);
        }

        // Clear inverse side to avoid stale cascade state before delete.
        target.setChildren(new ArrayList<>());
        categoryRepository.save(target);

        categoryRepository.delete(target);
        categoryRepository.flush();
    }
}
