package cn.laobayou.siyubao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 文件上传配置类
 * 配置静态资源访问路径和文件上传目录
 * 上传的文件是存储在 xxx/douyin/ddd.jpg
 * 前端访问路径为：http://localhost/files/xxx/douyin/add.jpg  会映射到磁盘绝对路径里面
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    /**
     * 头像上传基础路径
     */
    public static final String AVATAR_BASE_PATH = "./avatar/routes/";
    
    /**
     * 头像访问URL前缀
     */
    public static final String AVATAR_URL_PREFIX = "/avatar/routes/";

    @Value("${file.upload-path}")
    private String uploadPath;

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
        
        // 重新配置静态资源映射（因为在application.yml中禁用了默认映射）
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");

        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + normalizedUploadPath());

        // 配置其他静态资源
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 设置缓存时间为1小时
    }

    /**
     * 创建必要的目录
     */
    private void createDirectoriesIfNotExist() {
        try {
            Path target = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path legacy = Paths.get("./upload").toAbsolutePath().normalize();
            if (!target.equals(legacy) && !Files.exists(target) && Files.exists(legacy)) {
                Files.createDirectories(target.getParent());
                try {
                    Files.move(legacy, target);
                } catch (Exception ignored) {
                    try {
                        Files.walk(legacy).forEach(p -> {
                            try {
                                Path rel = legacy.relativize(p);
                                Path dst = target.resolve(rel).normalize();
                                if (Files.isDirectory(p)) {
                                    Files.createDirectories(dst);
                                } else {
                                    Files.createDirectories(dst.getParent());
                                    Files.copy(p, dst, StandardCopyOption.REPLACE_EXISTING);
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

        String[] platforms = {"douyin", "shipin", "xiaohongshu"};
        
        for (String platform : platforms) {
            File dir = new File(normalizedUploadPath(), platform);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    private String normalizedUploadPath() {
        Path p = Paths.get(uploadPath).toAbsolutePath().normalize();
        String s = p.toString();
        if (!s.endsWith(File.separator)) {
            s = s + File.separator;
        }
        return s;
    }

    /**
     * 获取平台头像上传路径目录
     */
    public String getPlatformAvatarPath(String platform) {
        return normalizedUploadPath() + platform + File.separator;
    }

    /**
     * 获取平台头像访问URL ,用于前端访问的。
     */
    public String getPlatformAvatarUrl(String platform, String filename) {
        return "/files/" + platform + "/" + filename;
    }
}
