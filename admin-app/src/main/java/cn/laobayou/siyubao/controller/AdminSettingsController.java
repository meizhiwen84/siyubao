package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.service.AdminOpLogService;
import cn.laobayou.siyubao.service.AppSettingService;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final AppSettingService settingService;
    private final AdminOpLogService opLogService;

    public AdminSettingsController(
            AuthContextService authContextService,
            AuthService authService,
            AppSettingService settingService,
            AdminOpLogService opLogService
    ) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.settingService = settingService;
        this.opLogService = opLogService;
    }

    @GetMapping("/get")
    public ResponseEntity<Map<String, Object>> get(HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);
            Map<String, Object> data = new HashMap<>();
            data.put("minuteIntervalSeconds", settingService.getInt("register.minute_interval_seconds", 60));
            data.put("dayLimit", settingService.getInt("register.day_limit", 3));
            data.put("wecomWebhookUrl", settingService.getString("wecom_webhook_url", ""));
            data.put("adminBaseUrl", settingService.getString("admin_base_url", ""));
            r.put("success", true);
            r.put("data", data);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.status(401).body(r);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            int minute = intVal(body, "minuteIntervalSeconds", 1, 3600);
            int day = intVal(body, "dayLimit", 1, 1000);
            String wecomWebhookUrl = str(body, "wecomWebhookUrl");
            String adminBaseUrl = str(body, "adminBaseUrl");

            settingService.set("register.minute_interval_seconds", String.valueOf(minute));
            settingService.set("register.day_limit", String.valueOf(day));
            settingService.set("wecom_webhook_url", wecomWebhookUrl);
            settingService.set("admin_base_url", adminBaseUrl);

            Map<String, Object> detail = new HashMap<>();
            detail.put("minuteIntervalSeconds", minute);
            detail.put("dayLimit", day);
            opLogService.log(request, admin, "SETTINGS_UPDATE", "SETTINGS", "all", detail);

            r.put("success", true);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    private AppUser requireAdmin(HttpServletRequest request) {
        DeviceSession s = authContextService.requireSession(request);
        AppUser u = authService.findUser(s.getUserId()).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!Boolean.TRUE.equals(u.getEnabled())) throw new RuntimeException("账号已被禁用");
        if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("无权限");
        return u;
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private int intVal(Map<String, Object> body, String key, int min, int max) {
        Object v = body == null ? null : body.get(key);
        int n;
        if (v instanceof Number) n = ((Number) v).intValue();
        else {
            String s = v == null ? "" : String.valueOf(v).trim();
            if (s.isEmpty()) throw new RuntimeException(key + "不能为空");
            n = Integer.parseInt(s);
        }
        if (n < min || n > max) throw new RuntimeException(key + "不合法");
        return n;
    }
}
