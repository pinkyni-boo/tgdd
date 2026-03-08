package com.hutech.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /product-images/** to the uploads directory in the project root
        // Use System.getProperty("user.dir") to strictly match the running application's root
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
        String uploadPath = uploadDir.toUri().toString();
        
        System.out.println("WebConfig Serving Images from: " + uploadPath);
        
        registry.addResourceHandler("/product-images/**")
                .addResourceLocations(uploadPath);
    }
}
