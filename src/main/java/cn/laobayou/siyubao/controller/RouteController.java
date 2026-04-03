package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.Route;
import cn.laobayou.siyubao.bean.CardKey;
import cn.laobayou.siyubao.service.CardKeyService;
import cn.laobayou.siyubao.service.RouteService;
import cn.laobayou.siyubao.config.FileUploadConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpSession;

/**
 * 线路管理控制器
 */
@Controller
@RequestMapping("/route")
public class RouteController {
    
    @Autowired
    private RouteService routeService;

    @Autowired
    private CardKeyService cardKeyService;
    
    /**
     * 线路管理页面
     */
    @GetMapping("/manage")
    public String managePage(Model model, HttpSession session) {
        String cardKey = currentCardKey(session);
        List<Route> routes = routeService.getAllRoutes(cardKey);
        model.addAttribute("routes", routes);
        return "route-manage";
    }
    
    /**
     * 获取所有线路（API）
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoutes(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cardKey = currentCardKey(session);
            List<Route> routes = routeService.getAllRoutes(cardKey);
            result.put("success", true);
            result.put("data", routes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 根据ID获取线路（API）
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoute(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cardKey = currentCardKey(session);
            Optional<Route> route = routeService.getRouteById(id, cardKey);
            if (route.isPresent()) {
                result.put("success", true);
                result.put("data", route.get());
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("message", "线路不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 创建新线路（API）
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createRoute(
            @RequestParam String routeName,
            @RequestParam String routeValue,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cardKey = currentCardKey(session);
            Route route = routeService.createRoute(cardKey, routeName, routeValue);
            result.put("success", true);
            result.put("data", route);
            result.put("message", "线路创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 更新线路基本信息（API）
     */
    @PostMapping("/api/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateRoute(
            @RequestParam Long id,
            @RequestParam String routeName,
            @RequestParam String routeValue,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cardKey = currentCardKey(session);
            Route route = routeService.updateRoute(cardKey, id, routeName, routeValue);
            result.put("success", true);
            result.put("data", route);
            result.put("message", "线路更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 更新线路状态（API）
     */
    @PostMapping("/api/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateRouteStatus(
            @PathVariable Long id,
            @RequestParam Boolean status,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cardKey = currentCardKey(session);
            Route route = routeService.updateRouteStatus(cardKey, id, status);
            result.put("success", true);
            result.put("data", route);
            result.put("message", "状态更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 更新线路欢迎语（API）
     */
    @PostMapping("/api/{id}/welcome-message")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateWelcomeMessage(
            @PathVariable Long id,
            @RequestParam String welcomeMessage,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cardKey = currentCardKey(session);
            Route route = routeService.updateWelcomeMessage(cardKey, id, welcomeMessage);
            result.put("success", true);
            result.put("data", route);
            result.put("message", "欢迎语更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 上传头像（API）
     */
    @PostMapping("/api/{id}/avatar/{platform}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @PathVariable Long id,
            @PathVariable String platform,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 上传文件
            String avatarPath = routeService.uploadAvatar(file, platform);
            
            // 更新线路头像
            String cardKey = currentCardKey(session);
            Route route = routeService.updateRouteAvatar(cardKey, id, platform, avatarPath);
            
            result.put("success", true);
            result.put("data", route);
            result.put("avatarPath", avatarPath);
            result.put("message", "头像上传成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 清除头像（API）
     */
    @DeleteMapping("/api/{id}/avatar/{platform}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearAvatar(
            @PathVariable Long id,
            @PathVariable String platform,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 清除线路头像
            String cardKey = currentCardKey(session);
            Route route = routeService.clearRouteAvatar(cardKey, id, platform);
            
            result.put("success", true);
            result.put("data", route);
            result.put("message", "头像清除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 删除线路（API）
     */
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteRoute(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cardKey = currentCardKey(session);
            routeService.deleteRoute(cardKey, id);
            result.put("success", true);
            result.put("message", "线路删除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 批量上传头像（API）
     */
    @PostMapping("/api/{id}/avatars")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAvatars(
            @PathVariable Long id,
            @RequestParam(value = "douyinFile", required = false) MultipartFile douyinFile,
            @RequestParam(value = "shipinFile", required = false) MultipartFile shipinFile,
            @RequestParam(value = "xiaohongshuFile", required = false) MultipartFile xiaohongshuFile,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cardKey = currentCardKey(session);
            Optional<Route> routeOpt = routeService.getRouteById(id, cardKey);
            if (!routeOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "线路不存在");
                return ResponseEntity.notFound().build();
            }
            
            Route route = routeOpt.get();
            
            // 上传抖音头像
            if (douyinFile != null && !douyinFile.isEmpty()) {
                String avatarPath = routeService.uploadAvatar(douyinFile, "douyin");
                route = routeService.updateRouteAvatar(cardKey, id, "douyin", avatarPath);
            }
            
            // 上传视频号头像
            if (shipinFile != null && !shipinFile.isEmpty()) {
                String avatarPath = routeService.uploadAvatar(shipinFile, "shipin");
                route = routeService.updateRouteAvatar(cardKey, id, "shipin", avatarPath);
            }
            
            // 上传小红书头像
            if (xiaohongshuFile != null && !xiaohongshuFile.isEmpty()) {
                String avatarPath = routeService.uploadAvatar(xiaohongshuFile, "xiaohongshu");
                route = routeService.updateRouteAvatar(cardKey, id, "xiaohongshu", avatarPath);
            }
            
            result.put("success", true);
            result.put("data", route);
            result.put("message", "头像上传成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    private String currentCardKey(HttpSession session) {
        Optional<CardKey> opt = cardKeyService.currentSessionCard(session);
        if (!opt.isPresent() || !cardKeyService.isValid(opt.get())) {
            throw new RuntimeException("卡密无效或未登录");
        }
        return opt.get().getCode();
    }
}
