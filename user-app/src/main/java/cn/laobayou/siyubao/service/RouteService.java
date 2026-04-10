package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.Route;
import cn.laobayou.siyubao.config.FileUploadConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 线路服务类
 */
@Service
public class RouteService {
    
    @Autowired
    private RouteRepository routeRepository;
    
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};
    @Autowired
    private FileUploadConfig fileUploadConfig;

    /**
     * 获取所有线路
     */
    public List<Route> getAllRoutes(Long userId) {
        return routeRepository.findAllByUserId(requireUserId(userId));
    }
    
    /**
     * 根据ID获取线路
     */
    public Optional<Route> getRouteById(Long id, Long userId) {
        return routeRepository.findByIdAndUserId(id, requireUserId(userId));
    }

    public Optional<Route> getRouteByValue(String routeValue, Long userId) {
        return routeRepository.findByRouteValueAndUserId(routeValue, requireUserId(userId));
    }

    public String getWelcomeMessageByRouteValue(String routeValue, Long userId) {
        Optional<Route> opt = routeRepository.findByRouteValueAndUserId(routeValue, requireUserId(userId));
        if (opt.isPresent()) {
            String m = opt.get().getWelcomeMessage();
            if (m != null && !m.trim().isEmpty()) {
                return m;
            }
        }
        return "";
    }
    
    /**
     * 根据状态获取线路
     */
    public List<Route> getRoutesByStatus(Boolean status, Long userId) {
        return routeRepository.findByStatusAndUserId(status, requireUserId(userId));
    }
    
    /**
     * 保存线路
     */
    public Route saveRoute(Long userId, Route route) {
        route.setUserId(requireUserId(userId));
        return routeRepository.save(route);
    }
    
    /**
     * 创建新线路
     */
    @Transactional
    public Route createRoute(Long userId, String routeName, String routeValue) {
        Long uid = requireUserId(userId);
        String rn = normText(routeName);
        String rv = normText(routeValue);
        if (rn.isEmpty()) throw new RuntimeException("线路名称不能为空");
        if (rv.isEmpty()) throw new RuntimeException("线路值不能为空");

        if (routeRepository.existsByRouteNameAndUserId(rn, uid)) {
            throw new RuntimeException("线路名称已存在");
        }
        
        Route route = new Route();
        route.setUserId(uid);
        route.setRouteName(rn);
        route.setRouteValue(rv);
        route.setStatus(true);
        route.setCreateTime(LocalDateTime.now());
        route.setUpdateTime(LocalDateTime.now());
        
        return routeRepository.save(route);
    }
    
    /**
     * 更新线路基本信息
     */
    @Transactional
    public Route updateRoute(Long userId, Long id, String routeName, String routeValue) {
        Long uid = requireUserId(userId);
        String rn = normText(routeName);
        String rv = normText(routeValue);
        if (rn.isEmpty()) throw new RuntimeException("线路名称不能为空");
        if (rv.isEmpty()) throw new RuntimeException("线路值不能为空");

        Optional<Route> routeOpt = routeRepository.findByIdAndUserId(id, uid);
        if (routeOpt.isPresent()) {
            Route route = routeOpt.get();
            
            // 检查线路名称是否已被其他线路使用
            if (!route.getRouteName().equals(rn) && routeRepository.existsByRouteNameAndUserId(rn, uid)) {
                throw new RuntimeException("线路名称已存在");
            }
            
            route.setRouteName(rn);
            route.setRouteValue(rv);
            route.setUpdateTime(LocalDateTime.now());
            return routeRepository.save(route);
        }
        throw new RuntimeException("线路不存在");
    }

    /**
     * 更新线路状态
     */
    @Transactional
    public Route updateRouteStatus(Long userId, Long id, Boolean status) {
        Optional<Route> routeOpt = routeRepository.findByIdAndUserId(id, requireUserId(userId));
        if (routeOpt.isPresent()) {
            Route route = routeOpt.get();
            route.setStatus(status);
            route.setUpdateTime(LocalDateTime.now());
            return routeRepository.save(route);
        }
        throw new RuntimeException("线路不存在");
    }
    
    /**
     * 更新线路欢迎语
     */
    @Transactional
    public Route updateWelcomeMessage(Long userId, Long id, String welcomeMessage) {
        Optional<Route> routeOpt = routeRepository.findByIdAndUserId(id, requireUserId(userId));
        if (routeOpt.isPresent()) {
            Route route = routeOpt.get();
            route.setWelcomeMessage(welcomeMessage);
            route.setUpdateTime(LocalDateTime.now());
            return routeRepository.save(route);
        }
        throw new RuntimeException("线路不存在");
    }
    
    /**
     * 上传头像文件
     */
    public String uploadAvatar(MultipartFile file, String platform) throws IOException {
        String originalFilename = file == null ? null : file.getOriginalFilename();
        byte[] bytes = file == null ? null : file.getBytes();
        return uploadAvatarBytes(bytes, originalFilename, platform);
    }

    public String uploadAvatarBytes(byte[] bytes, String originalFilename, String platform) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new RuntimeException("文件不能为空");
        }

        if (!isValidPlatform(platform)) {
            throw new RuntimeException("不支持的平台：" + platform);
        }

        if (bytes.length > 10 * 1024 * 1024) throw new RuntimeException("图片过大");
        String ext = detectImageExt(bytes);
        if (ext == null) throw new RuntimeException("只支持图片格式：jpg, jpeg, png, gif, webp");

        String platformPath = fileUploadConfig.getPlatformAvatarPath(platform);
        Path avatarDir = Paths.get(platformPath).normalize();
        Files.createDirectories(avatarDir);

        String filename = platform + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        Path filePath = avatarDir.resolve(filename);
        Files.write(filePath, bytes);

        return fileUploadConfig.getPlatformAvatarUrl(platform, filename);
    }

    private String detectImageExt(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return ".png";
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) return ".jpg";
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38) return ".gif";
        if (bytes.length >= 12 &&
                bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 &&
                bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return ".webp";
        return null;
    }
    
    /**
     * 更新线路头像
     */
    @Transactional
    public Route updateRouteAvatar(Long userId, Long id, String platform, String avatarPath) {
        Optional<Route> routeOpt = routeRepository.findByIdAndUserId(id, requireUserId(userId));
        if (routeOpt.isPresent()) {
            Route route = routeOpt.get();
            
            switch (platform.toLowerCase()) {
                case "douyin":
                    route.setDouyinAvatar(avatarPath);
                    break;
                case "shipin":
                    route.setShipinAvatar(avatarPath);
                    break;
                case "xiaohongshu":
                    route.setXiaohongshuAvatar(avatarPath);
                    break;
                default:
                    throw new RuntimeException("不支持的平台类型");
            }
            
            route.setUpdateTime(LocalDateTime.now());
            return routeRepository.save(route);
        }
        throw new RuntimeException("线路不存在");
    }
    
    /**
     * 清除线路头像
     */
    @Transactional
    public Route clearRouteAvatar(Long userId, Long id, String platform) {
        Optional<Route> routeOpt = routeRepository.findByIdAndUserId(id, requireUserId(userId));
        if (routeOpt.isPresent()) {
            Route route = routeOpt.get();
            
            switch (platform.toLowerCase()) {
                case "douyin":
                    route.setDouyinAvatar(null);
                    break;
                case "shipin":
                    route.setShipinAvatar(null);
                    break;
                case "xiaohongshu":
                    route.setXiaohongshuAvatar(null);
                    break;
                default:
                    throw new RuntimeException("不支持的平台类型");
            }
            
            route.setUpdateTime(LocalDateTime.now());
            return routeRepository.save(route);
        }
        throw new RuntimeException("线路不存在");
    }
    
    /**
     * 删除线路
     */
    @Transactional
    public void deleteRoute(Long userId, Long id) {
        routeRepository.deleteByIdAndUserId(id, requireUserId(userId));
    }

    private Long requireUserId(Long userId) {
        if (userId == null) throw new RuntimeException("未登录");
        if (userId <= 0) throw new RuntimeException("未登录");
        return userId;
    }

    private String normText(String s) {
        return s == null ? "" : s.trim();
    }
    
    /**
     * 验证是否为有效的平台
     */
    private boolean isValidPlatform(String platform) {
        return "douyin".equals(platform) || "shipin".equals(platform) || "xiaohongshu".equals(platform);
    }
    
    /**
     * 验证是否为有效的图片文件
     */
    private boolean isValidImageFile(String filename) {
        String lowerFilename = filename.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }
}
