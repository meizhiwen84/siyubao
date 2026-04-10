package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.Route;
import cn.laobayou.siyubao.service.LocalUserSessionService;
import cn.laobayou.siyubao.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 线路管理控制器
 */
@Controller
@RequestMapping("/route")
public class RouteController {
    
    @Autowired
    private RouteService routeService;

    @Autowired
    private LocalUserSessionService localUserSessionService;
    
    /**
     * 线路管理页面
     */
    @GetMapping("/manage")
    public String managePage() {
        return "redirect:/app/route";
    }
    
    /**
     * 获取所有线路（API）
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoutes() {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = requireUserId();
            List<Route> routes = routeService.getAllRoutes(userId);
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
    public ResponseEntity<Map<String, Object>> getRoute(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = requireUserId();
            Optional<Route> route = routeService.getRouteById(id, userId);
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
            @RequestParam String routeValue) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = requireUserId();
            Route route = routeService.createRoute(userId, routeName, routeValue);
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
            @RequestParam String routeValue) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = requireUserId();
            Route route = routeService.updateRoute(userId, id, routeName, routeValue);
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
            @RequestParam Boolean status) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = requireUserId();
            Route route = routeService.updateRouteStatus(userId, id, status);
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
            @RequestParam String welcomeMessage) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = requireUserId();
            Route route = routeService.updateWelcomeMessage(userId, id, welcomeMessage);
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
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 上传文件
            String avatarPath = routeService.uploadAvatar(file, platform);
            
            // 更新线路头像
            Long userId = requireUserId();
            Route route = routeService.updateRouteAvatar(userId, id, platform, avatarPath);
            
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
            @PathVariable String platform) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 清除线路头像
            Long userId = requireUserId();
            Route route = routeService.clearRouteAvatar(userId, id, platform);
            
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
    public ResponseEntity<Map<String, Object>> deleteRoute(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = requireUserId();
            routeService.deleteRoute(userId, id);
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
     */

    private Long requireUserId() {
        Long uid = localUserSessionService.userId();
        if (uid == null || uid <= 0) throw new RuntimeException("未登录");
        return uid;
    }
}
