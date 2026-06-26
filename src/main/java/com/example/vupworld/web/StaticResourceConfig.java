package com.example.vupworld.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 单机包资源策略：公开面只挂 v4 运行时资产。
        String galleryV4Location = Path.of("图库", "v4").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/gallery/v4/**")
            .addResourceLocations(galleryV4Location)
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());

        // 优化图库 - 长缓存
        registry.addResourceHandler("/gallery-optimized/**")
            .addResourceLocations("classpath:/static/gallery-optimized/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());

        // Vendor库 - 长缓存 (Chart.js等本地化第三方库)
        registry.addResourceHandler("/vendor/**")
            .addResourceLocations("classpath:/static/vendor/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());
    }
}
