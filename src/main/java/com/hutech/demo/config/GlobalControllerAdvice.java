package com.hutech.demo.config;

import com.hutech.demo.model.Category;
import com.hutech.demo.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CategoryRepository categoryRepository;

    @ModelAttribute("rootCategories")
    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    @ModelAttribute("megamenuCategories")
    public List<Category> getMegaMenuCategories() {
        // Find "Phụ kiện" and return its children for specific logic if needed
        // Or simply rely on rootCategories in the layout
        Category phuKien = categoryRepository.findAll().stream()
                .filter(c -> "Phụ kiện".equals(c.getName()))
                .findFirst()
                .orElse(null);

        if (phuKien != null) {
            return phuKien.getChildren();
        }
        return List.of();
    }
}
