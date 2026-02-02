package cn.laobayou.siyubao.config;

import cn.laobayou.siyubao.interceptor.CardKeySessionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcCardConfig implements WebMvcConfigurer {
    @Autowired
    private CardKeySessionInterceptor cardKeySessionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cardKeySessionInterceptor)
                .addPathPatterns("/chat-preview", "/generateDyChat")
                .excludePathPatterns("/card-verify", "/card-config", "/api/card/**", "/", "/index");
    }
}

