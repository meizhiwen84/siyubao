package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.service.LocalUserSessionService;
import cn.laobayou.siyubao.service.RemoteAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final RemoteAdminService remoteAdminService;
    private final LocalUserSessionService localUserSessionService;

    public PaymentController(
            RemoteAdminService remoteAdminService,
            LocalUserSessionService localUserSessionService
    ) {
        this.remoteAdminService = remoteAdminService;
        this.localUserSessionService = localUserSessionService;
    }

    private String requireToken() {
        String t = localUserSessionService.token();
        if (t == null || t.trim().isEmpty()) {
            throw new RuntimeException("未登录");
        }
        return t;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        Map<String, Object> r = new HashMap<>();
        try {
            Map<String, Object> resp = remoteAdminService.paymentConfig();
            r.put("success", true);
            r.putAll(resp);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<Map<String, Object>> myOrders() {
        Map<String, Object> r = new HashMap<>();
        try {
            String token = requireToken();
            Map<String, Object> resp = remoteAdminService.paymentMyOrders(token);
            r.put("success", true);
            r.putAll(resp);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        Map<String, Object> r = new HashMap<>();
        try {
            String token = requireToken();
            Map<String, Object> resp = remoteAdminService.paymentSubmit(token, body);
            r.put("success", true);
            r.putAll(resp);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }
}
