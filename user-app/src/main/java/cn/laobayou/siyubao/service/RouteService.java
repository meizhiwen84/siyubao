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
    
    private static final String AVATAR_DIR = "src/main/resources/static/avatar/";
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};
    @Autowired
    private FileUploadConfig fileUploadConfig;

    /**
     * 获取所有线路
     */
    public List<Route> getAllRoutes(String cardKey) {
        return routeRepository.findAllByCardKey(cardKey);
    }
    
    /**
     * 根据ID获取线路
     */
    public Optional<Route> getRouteById(Long id, String cardKey) {
        return routeRepository.findByIdAndCardKey(id, cardKey);
    }

    public Optional<Route> getRouteByValue(String routeValue, String cardKey) {
        return routeRepository.findByRouteValueAndCardKey(routeValue, cardKey);
    }

    public String getWelcomeMessageByRouteValue(String routeValue, String cardKey) {
        Optional<Route> opt = routeRepository.findByRouteValueAndCardKey(routeValue, cardKey);
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
    public List<Route> getRoutesByStatus(Boolean status, String cardKey) {
        return routeRepository.findByStatusAndCardKey(status, cardKey);
    }
    
    /**
     * 保存线路
     */
    public Route saveRoute(String cardKey, Route route) {
        route.setCardKey(cardKey);
        return routeRepository.save(route);
    }
    
    /**
     * 创建新线路
     */
    @Transactional
    public Route createRoute(String cardKey, String routeName, String routeValue) {
        if (routeRepository.existsByRouteNameAndCardKey(routeName, cardKey)) {
            throw new RuntimeException("线路名称已存在");
        }
        
        Route route = new Route();
        route.setCardKey(cardKey);
        route.setRouteName(routeName);
        route.setRouteValue(routeValue);
        route.setStatus(true);
        route.setCreateTime(LocalDateTime.now());
        route.setUpdateTime(LocalDateTime.now());
        
        return routeRepository.save(route);
    }
    
    /**
     * 更新线路基本信息
     */
    @Transactional
    public Route updateRoute(String cardKey, Long id, String routeName, String routeValue) {
        Optional<Route> routeOpt = routeRepository.findByIdAndCardKey(id, cardKey);
        if (routeOpt.isPresent()) {
            Route route = routeOpt.get();
            
            // 检查线路名称是否已被其他线路使用
            if (!route.getRouteName().equals(routeName) && routeRepository.existsByRouteNameAndCardKey(routeName, cardKey)) {
                throw new RuntimeException("线路名称已存在");
            }
            
            route.setRouteName(routeName);
            route.setRouteValue(routeValue);
            route.setUpdateTime(LocalDateTime.now());
            return routeRepository.save(route);
        }
        throw new RuntimeException("线路不存在");
    }

    /**
     * 更新线路状态
     */
    @Transactional
    public Route updateRouteStatus(String cardKey, Long id, Boolean status) {
        Optional<Route> routeOpt = routeRepository.findByIdAndCardKey(id, cardKey);
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
    public Route updateWelcomeMessage(String cardKey, Long id, String welcomeMessage) {
        Optional<Route> routeOpt = routeRepository.findByIdAndCardKey(id, cardKey);
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
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        
        // 验证平台
        if (!isValidPlatform(platform)) {
            throw new RuntimeException("不支持的平台：" + platform);
        }
        
        // 验证文件格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidImageFile(originalFilename)) {
            throw new RuntimeException("只支持图片格式：jpg, jpeg, png, gif");
        }
        
        // 获取平台头像目录
        String platformPath = fileUploadConfig.getPlatformAvatarPath(platform);
        Path avatarDir = Paths.get(platformPath).normalize();
        Files.createDirectories(avatarDir);
        
        // 生成唯一文件名
        String extension = getFileExtension(originalFilename);
        String filename = platform + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        
        // 保存文件
        Path filePath = avatarDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // 返回访问URL
        return fileUploadConfig.getPlatformAvatarUrl(platform, filename);
    }
    
    /**
     * 更新线路头像
     */
    @Transactional
    public Route updateRouteAvatar(String cardKey, Long id, String platform, String avatarPath) {
        Optional<Route> routeOpt = routeRepository.findByIdAndCardKey(id, cardKey);
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
    public Route clearRouteAvatar(String cardKey, Long id, String platform) {
        Optional<Route> routeOpt = routeRepository.findByIdAndCardKey(id, cardKey);
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
    public void deleteRoute(String cardKey, Long id) {
        routeRepository.deleteByIdAndCardKey(id, cardKey);
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
