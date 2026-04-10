package cn.laobayou.siyubao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcCardConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/app/avatar/**").addResourceLocations("classpath:/static/avatar/");
        registry.addResourceHandler("/app/siyubao_cq_files/**").addResourceLocations("classpath:/static/siyubao_cq_files/");
        java.nio.file.Path uploadDir = java.nio.file.Paths.get(System.getProperty("user.home"), ".siyubao", "uploads").toAbsolutePath().normalize();
        try {
            java.nio.file.Path legacy = java.nio.file.Paths.get("data", "uploads").toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(uploadDir) && java.nio.file.Files.exists(legacy)) {
                java.nio.file.Files.createDirectories(uploadDir.getParent());
                try {
                    java.nio.file.Files.move(legacy, uploadDir);
                } catch (Exception ignored) {
                    try {
                        java.nio.file.Files.walk(legacy).forEach(p -> {
                            try {
                                java.nio.file.Path rel = legacy.relativize(p);
                                java.nio.file.Path target = uploadDir.resolve(rel).normalize();
                                if (java.nio.file.Files.isDirectory(p)) {
                                    java.nio.file.Files.createDirectories(target);
                                } else {
                                    java.nio.file.Files.createDirectories(target.getParent());
                                    java.nio.file.Files.copy(p, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (Exception ignored2) {
                            }
                        });
                    } catch (Exception ignored2) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        String uploadLocation = uploadDir.toUri().toString();
        if (!uploadLocation.endsWith("/")) uploadLocation = uploadLocation + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(uploadLocation);
    }
}
