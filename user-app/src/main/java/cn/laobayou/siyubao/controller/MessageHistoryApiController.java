package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessageHistory;
import cn.laobayou.siyubao.service.ChatMessageHistoryService;
import cn.laobayou.siyubao.service.LocalUserSessionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MessageHistoryApiController {
    private final ChatMessageHistoryService service;
    private final LocalUserSessionService localUserSessionService;

    public MessageHistoryApiController(ChatMessageHistoryService service, LocalUserSessionService localUserSessionService) {
        this.service = service;
        this.localUserSessionService = localUserSessionService;
    }

    @GetMapping("/api/history/list")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String line,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Map<String, Object> result = new HashMap<>();
        Long userId = localUserSessionService.userId();
        if (userId == null || userId <= 0) {
            result.put("success", false);
            result.put("message", "未登录");
            return ResponseEntity.status(401).body(result);
        }

        String ln = sanitize(line, 64);
        String pf = sanitize(platform, 8);
        String ph = sanitize(phone, 32);
        if (pf != null && !Arrays.asList("dy", "xhs", "sph").contains(pf)) {
            result.put("success", false);
            result.put("message", "平台参数不合法");
            return ResponseEntity.badRequest().body(result);
        }

        Page<ChatMessageHistory> p = service.search(userId, ln, pf, ph, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        result.put("success", true);
        result.put("total", p.getTotalElements());
        result.put("pages", p.getTotalPages());
        result.put("page", Math.max(page, 0));
        result.put("size", Math.min(Math.max(size, 1), 100));
        result.put("data", p.getContent());
        return ResponseEntity.ok(result);
    }

    private String sanitize(String s, int maxLen) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() > maxLen) t = t.substring(0, maxLen);
        return t;
    }
}
