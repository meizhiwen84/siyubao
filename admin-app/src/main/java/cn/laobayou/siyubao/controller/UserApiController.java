package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.bean.MembershipPlan;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import cn.laobayou.siyubao.service.MembershipService;
import cn.laobayou.siyubao.service.UsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserApiController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final MembershipService membershipService;
    private final UsageService usageService;

    public UserApiController(AuthContextService authContextService, AuthService authService, MembershipService membershipService, UsageService usageService) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.membershipService = membershipService;
        this.usageService = usageService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            DeviceSession s = authContextService.requireSession(request);
            AppUser u = authService.findUser(s.getUserId()).orElseThrow(() -> new RuntimeException("用户不存在"));
            MembershipPlan p = membershipService.resolvePlan(u.getId());
            r.put("success", true);
            r.put("user", userInfo(u));
            r.put("plan", planInfo(p));
            r.put("todayUsed", usageService.todayCount(u.getId()));
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.status(401).body(r);
        }
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            authContextService.requireSession(request);
            r.put("success", true);
            r.put("kicked", false);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("kicked", true);
            r.put("message", e.getMessage());
            return ResponseEntity.status(401).body(r);
        }
    }

    @PostMapping("/generate/consume")
    public ResponseEntity<Map<String, Object>> consume(HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            DeviceSession s = authContextService.requireSession(request);
            AppUser u = authService.findUser(s.getUserId()).orElseThrow(() -> new RuntimeException("用户不存在"));
            MembershipPlan p = membershipService.resolvePlan(u.getId());
            boolean allowed = usageService.allowedToGenerate(u.getId(), p);
            if (!allowed) throw new RuntimeException("今日生成次数已用完");
            usageService.incrementToday(u.getId());
            r.put("success", true);
            r.put("watermark", Boolean.TRUE.equals(p.getWatermark()));
            r.put("planCode", p.getCode());
            r.put("todayUsed", usageService.todayCount(u.getId()));
            r.put("dailyLimit", p.getDailyFreeLimit());
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    private Map<String, Object> userInfo(AppUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("role", u.getRole());
        m.put("enabled", u.getEnabled());
        return m;
    }

    private Map<String, Object> planInfo(MembershipPlan p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("code", p.getCode());
        m.put("name", p.getName());
        m.put("priceCents", p.getPriceCents());
        m.put("durationDays", p.getDurationDays());
        m.put("deviceLimit", p.getDeviceLimit());
        m.put("dailyFreeLimit", p.getDailyFreeLimit());
        m.put("watermark", p.getWatermark());
        return m;
    }
}

