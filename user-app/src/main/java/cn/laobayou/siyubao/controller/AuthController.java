package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.service.LocalDeviceService;
import cn.laobayou.siyubao.service.LocalUserSessionService;
import cn.laobayou.siyubao.service.RemoteAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RemoteAdminService remoteAdminService;
    private final LocalDeviceService localDeviceService;
    private final LocalUserSessionService localUserSessionService;

    public AuthController(RemoteAdminService remoteAdminService, LocalDeviceService localDeviceService, LocalUserSessionService localUserSessionService) {
        this.remoteAdminService = remoteAdminService;
        this.localDeviceService = localDeviceService;
        this.localUserSessionService = localUserSessionService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        Map<String, Object> r = new HashMap<>();
        try {
            String username = str(body, "username");
            String password = str(body, "password");
            log.info("用户注册请求 - 用户名: {}", username);
            Map<String, Object> resp = remoteAdminService.publicRegister(username, password);
            if (!isSuccess(resp)) throw new RuntimeException(msg(resp, "注册失败"));
            log.info("用户注册成功 - 用户名: {}", username);
            r.put("success", true);
            r.putAll(resp);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            log.error("用户注册失败", e);
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> body) {
        Map<String, Object> r = new HashMap<>();
        try {
            String username = str(body, "username");
            String password = str(body, "password");
            String deviceId = localDeviceService.getOrCreateDeviceId();
            log.info("用户登录请求 - 用户名: {}, 设备ID: {}", username, deviceId);
            Map<String, Object> resp = remoteAdminService.publicLogin(username, password, deviceId);
            if (!isSuccess(resp)) throw new RuntimeException(msg(resp, "登录失败"));
            String token = objStr(resp.get("token"));
            Map<String, Object> user = objMap(resp.get("user"));
            Long userId = objLong(user.get("id"));
            String u = objStr(user.get("username"));
            String userNo = objStr(user.get("userNo"));
            localUserSessionService.save(token, userId, u, userNo);
            log.info("用户登录成功 - 用户名: {}, 用户编号: {}", u, userNo);
            r.put("success", true);
            r.put("token", token);
            r.put("user", user);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            log.error("用户登录失败", e);
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> r = new HashMap<>();
        try {
            log.info("用户登出请求");
            localUserSessionService.clear();
            log.info("用户登出成功");
            r.put("success", true);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            log.error("用户登出失败", e);
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        Map<String, Object> r = new HashMap<>();
        try {
            String token = localUserSessionService.token();
            if (token == null || token.isEmpty()) throw new RuntimeException("未登录");
            Map<String, Object> resp = remoteAdminService.me(token);
            if (!isSuccess(resp)) throw new RuntimeException(msg(resp, "未登录"));
            r.put("success", true);
            r.putAll(resp);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.status(401).body(r);
        }
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat() {
        Map<String, Object> r = new HashMap<>();
        try {
            String token = localUserSessionService.token();
            if (token == null || token.isEmpty()) throw new RuntimeException("未登录");
            Map<String, Object> resp = remoteAdminService.heartbeat(token);
            r.put("success", true);
            r.putAll(resp);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            log.warn("心跳检测失败", e);
            r.put("success", false);
            r.put("message", e.getMessage());
            r.put("kicked", true);
            return ResponseEntity.status(401).body(r);
        }
    }

    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> plans() {
        Map<String, Object> r = new HashMap<>();
        try {
            log.debug("获取套餐列表请求");
            Map<String, Object> resp = remoteAdminService.publicPlans();
            if (!isSuccess(resp)) throw new RuntimeException(msg(resp, "获取套餐失败"));
            r.put("success", true);
            r.putAll(resp);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            log.error("获取套餐列表失败", e);
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private boolean isSuccess(Map<String, Object> resp) {
        Object v = resp == null ? null : resp.get("success");
        return v instanceof Boolean ? (Boolean) v : "true".equalsIgnoreCase(String.valueOf(v));
    }

    private String msg(Map<String, Object> resp, String def) {
        Object m = resp == null ? null : resp.get("message");
        return m == null ? def : String.valueOf(m);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new HashMap<>();
    }

    private String objStr(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private Long objLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
