package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AdminOpLog;
import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.repository.AdminOpLogRepository;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final AdminOpLogRepository repository;

    public AdminLogController(AuthContextService authContextService, AuthService authService, AdminOpLogRepository repository) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.repository = repository;
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);
            int p = Math.max(page, 0);
            int s = Math.min(Math.max(size, 1), 100);
            String kw = keyword == null ? null : keyword.trim();
            if (kw != null && kw.isEmpty()) kw = null;
            if (kw != null) kw = "%" + kw.toLowerCase() + "%";
            Page<AdminOpLog> res = repository.search(kw, PageRequest.of(p, s));
            r.put("success", true);
            r.put("data", res.getContent());
            r.put("total", res.getTotalElements());
            r.put("pages", res.getTotalPages());
            r.put("page", p);
            r.put("size", s);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.status(401).body(r);
        }
    }

    private AppUser requireAdmin(HttpServletRequest request) {
        DeviceSession s = authContextService.requireSession(request);
        AppUser u = authService.findUser(s.getUserId()).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!Boolean.TRUE.equals(u.getEnabled())) throw new RuntimeException("账号已被禁用");
        if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("无权限");
        return u;
    }
}
