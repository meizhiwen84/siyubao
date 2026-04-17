package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.service.LocalUserSessionService;
import cn.laobayou.siyubao.service.RemoteAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CustomerServiceController {
    private final LocalUserSessionService localUserSessionService;
    private final RemoteAdminService remoteAdminService;

    public CustomerServiceController(
            LocalUserSessionService localUserSessionService,
            RemoteAdminService remoteAdminService
    ) {
        this.localUserSessionService = localUserSessionService;
        this.remoteAdminService = remoteAdminService;
    }

    @GetMapping("/customer-service/info")
    public ResponseEntity<Map<String, Object>> getCustomerServiceInfo() {
        Map<String, Object> r = new HashMap<>();
        try {
            Map<String, Object> adminResp = remoteAdminService.customerServiceInfo();
            r.putAll(adminResp);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/feedback/submit")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @RequestBody Map<String, Object> body
    ) {
        Map<String, Object> r = new HashMap<>();
        try {
            String token = localUserSessionService.token();
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("未登录");
            }
            Map<String, Object> adminResp = remoteAdminService.feedbackSubmit(token, body);
            r.putAll(adminResp);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }
}
