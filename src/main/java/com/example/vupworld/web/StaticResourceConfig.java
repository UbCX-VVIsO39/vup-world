package com.example.vupworld.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 单机包资源策略：公开面只挂 v4 运行时资产；生成源图、水印备份和生成日志留在本地。
        String galleryV4Location = Path.of("图库", "v4").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/gallery/v4/**")
            .addResourceLocations(galleryV4Location)
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
            .resourceChain(true)
            .addResolver(new GalleryReleaseResourceResolver());

        // 优化图库 - 长缓存
        registry.addResourceHandler("/gallery-optimized/**")
            .addResourceLocations("classpath:/static/gallery-optimized/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());

        // Vendor库 - 长缓存 (Chart.js等本地化第三方库)
        registry.addResourceHandler("/vendor/**")
            .addResourceLocations("classpath:/static/vendor/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());
    }

    private static final class GalleryReleaseResourceResolver implements ResourceResolver {
        @Override
        public Resource resolveResource(
            HttpServletRequest request,
            String requestPath,
            List<? extends Resource> locations,
            ResourceResolverChain chain
        ) {
            if (isBlockedReleaseArtifact(requestPath)) {
                return null;
            }
            return chain.resolveResource(request, requestPath, locations);
        }

        @Override
        public String resolveUrlPath(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
            if (isBlockedReleaseArtifact(resourceUrlPath)) {
                return null;
            }
            return chain.resolveUrlPath(resourceUrlPath, locations);
        }

        private static boolean isBlockedReleaseArtifact(String path) {
            String normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
            if (!normalized.contains("commercial-v1/")) {
                return false;
            }
            String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
            return fileName.endsWith("-source.png")
                || fileName.contains("original-watermark-backup")
                || fileName.equals("asset-plan.json")
                || fileName.equals("generation-log.json");
        }
    }
}
