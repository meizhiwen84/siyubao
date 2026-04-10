package cn.laobayou.siyubao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AdminApiAuthInterceptor adminApiAuthInterceptor;

    public WebMvcConfig(AdminApiAuthInterceptor adminApiAuthInterceptor) {
        this.adminApiAuthInterceptor = adminApiAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminApiAuthInterceptor)
                .addPathPatterns("/api/admin/**");
    }
}
