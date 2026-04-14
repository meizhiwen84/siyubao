package cn.laobayou.siyubao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AdminApiAuthInterceptor adminApiAuthInterceptor;

    @Value("${siyubao.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${siyubao.upload.url-prefix:/uploads}")
    private String urlPrefix;

    public WebMvcConfig(AdminApiAuthInterceptor adminApiAuthInterceptor) {
        this.adminApiAuthInterceptor = adminApiAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminApiAuthInterceptor)
                .addPathPatterns("/api/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保是绝对路径
        String absoluteDir = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        String location = "file:" + absoluteDir;
        if (!location.endsWith("/")) {
            location += "/";
        }
        String pattern = urlPrefix + "/**";
        if (!pattern.startsWith("/")) {
            pattern = "/" + pattern;
        }
        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }
}
