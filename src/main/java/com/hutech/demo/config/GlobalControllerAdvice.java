package com.hutech.demo.config;

import com.hutech.demo.model.Category;
import com.hutech.demo.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalControllerAdvice {

    @Autowired
    private CategoryRepository categoryRepository;

    @ModelAttribute("rootCategories")
    public List<Category> getRootCategories() {
        try {
            return categoryRepository.findByParentIsNull();
        } catch (Exception ex) {
            log.warn("Khong tai duoc rootCategories, fallback danh sach rong: {}", ex.getMessage());
            return List.of();
        }
    }

    @ModelAttribute("megamenuCategories")
    public List<Category> getMegaMenuCategories() {
        try {
            Category phuKien = categoryRepository.findAll().stream()
                    .filter(c -> "Phụ kiện".equals(c.getName()))
                    .findFirst()
                    .orElse(null);
            if (phuKien != null) {
                return phuKien.getChildren();
            }
            return List.of();
        } catch (Exception ex) {
            log.warn("Khong tai duoc megamenuCategories, fallback danh sach rong: {}", ex.getMessage());
            return List.of();
        }
    }
}
