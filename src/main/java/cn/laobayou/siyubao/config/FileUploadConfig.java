package cn.laobayou.siyubao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 文件上传配置类
 * 配置静态资源访问路径和文件上传目录
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    /**
     * 头像上传基础路径
     */
    public static final String AVATAR_BASE_PATH = "src/main/resources/static/avatar/routes/";
    
    /**
     * 头像访问URL前缀
     */
    public static final String AVATAR_URL_PREFIX = "/avatar/routes/";

    /**
     * 配置静态资源处理器
     * 使文件可以通过URL直接访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保目录存在
        createDirectoriesIfNotExist();
        
        // 配置头像资源访问路径
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("classpath:/static/avatar/");
    }

    /**
     * 创建必要的目录
     */
    private void createDirectoriesIfNotExist() {
        String[] platforms = {"douyin", "shipin", "xiaohongshu"};
        
        for (String platform : platforms) {
            File dir = new File(AVATAR_BASE_PATH + platform);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    /**
     * 获取平台头像上传路径
     */
    public static String getPlatformAvatarPath(String platform) {
        return AVATAR_BASE_PATH + platform + "/";
    }

    /**
     * 获取平台头像访问URL
     */
    public static String getPlatformAvatarUrl(String platform, String filename) {
        return AVATAR_URL_PREFIX + platform + "/" + filename;
    }
}