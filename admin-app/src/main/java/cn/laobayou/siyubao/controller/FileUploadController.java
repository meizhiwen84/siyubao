package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.service.AppSettingService;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileUploadController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final AppSettingService appSettingService;

    @Value("${siyubao.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${siyubao.upload.url-prefix:/uploads}")
    private String urlPrefix;

    public FileUploadController(
            AuthContextService authContextService,
            AuthService authService,
            AppSettingService appSettingService
    ) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.appSettingService = appSettingService;
    }

    private AppUser requireAdmin(HttpServletRequest request) {
        AppUser u = authService.findUser(authContextService.requireSession(request).getUserId()).orElse(null);
        if (u == null) throw new RuntimeException("用户不存在");
        if (!Boolean.TRUE.equals(u.getEnabled())) throw new RuntimeException("账号已被禁用");
        if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("无权限");
        return u;
    }

    @PostMapping("/admin/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);

            if (file.isEmpty()) {
                throw new RuntimeException("文件不能为空");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("仅支持图片文件");
            }

            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            if (ext.isEmpty()) {
                ext = ".png";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM");
            String datePath = sdf.format(new Date());
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String filename = uuid + ext;

            // 确保目录是绝对路径
            Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            String relativePath = datePath + "/" + filename;
            Path fullPath = baseDir.resolve(datePath).resolve(filename);

            // 创建目录
            Files.createDirectories(fullPath.getParent());
            
            // 保存文件
            file.transferTo(fullPath.toFile());

            // 构建URL
            String relativeUrl = urlPrefix + "/" + relativePath.replace("\\", "/");
            
            // 尝试获取配置的管理端域名
            String adminBaseUrl = appSettingService.getString("admin_base_url", "").trim();
            String fullUrl = relativeUrl;
            if (!adminBaseUrl.isEmpty()) {
                // 确保域名格式正确
                if (!adminBaseUrl.startsWith("http://") && !adminBaseUrl.startsWith("https://")) {
                    adminBaseUrl = "https://" + adminBaseUrl;
                }
                if (adminBaseUrl.endsWith("/")) {
                    adminBaseUrl = adminBaseUrl.substring(0, adminBaseUrl.length() - 1);
                }
                if (relativeUrl.startsWith("/")) {
                    fullUrl = adminBaseUrl + relativeUrl;
                } else {
                    fullUrl = adminBaseUrl + "/" + relativeUrl;
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("url", fullUrl);
            data.put("filename", filename);

            r.put("success", true);
            r.put("data", data);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }
}
