package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.bean.MembershipPlan;
import cn.laobayou.siyubao.repository.MembershipPlanRepository;
import cn.laobayou.siyubao.service.AdminOpLogService;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/plans")
public class AdminPlanController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final MembershipPlanRepository repository;
    private final AdminOpLogService opLogService;

    public AdminPlanController(AuthContextService authContextService, AuthService authService, MembershipPlanRepository repository, AdminOpLogService opLogService) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.repository = repository;
        this.opLogService = opLogService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);
            List<MembershipPlan> list = repository.findAllByOrderBySortOrderAscIdAsc();
            r.put("success", true);
            r.put("data", list);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.status(401).body(r);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            String code = str(body, "code").toUpperCase();
            if (code.isEmpty()) throw new RuntimeException("code不能为空");
            if (code.length() > 32) throw new RuntimeException("code过长");
            if (repository.findByCode(code).isPresent()) throw new RuntimeException("code已存在");

            MembershipPlan p = new MembershipPlan();
            p.setCode(code);
            p.setName(reqStr(body, "name", 64));
            p.setPriceCents(reqInt(body, "priceCents", 0, 99999999));
            p.setDurationDays(reqInt(body, "durationDays", 0, 36500));
            p.setDeviceLimit(reqInt(body, "deviceLimit", 1, 1000));
            p.setDailyFreeLimit(reqInt(body, "dailyFreeLimit", 0, 10000));
            p.setWatermark(reqBool(body, "watermark"));
            p.setEnabled(reqBool(body, "enabled"));
            p.setSortOrder(reqInt(body, "sortOrder", 0, 99999));
            p.setCreateTime(LocalDateTime.now());
            MembershipPlan saved = repository.save(p);
            opLogService.log(request, admin, "PLAN_CREATE", "PLAN", String.valueOf(saved.getId()), saved);

            r.put("success", true);
            r.put("data", saved);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            MembershipPlan p = repository.findById(id).orElseThrow(() -> new RuntimeException("套餐不存在"));
            String name = str(body, "name");
            if (!name.isEmpty()) p.setName(reqStr(body, "name", 64));
            if (body != null && body.containsKey("priceCents")) p.setPriceCents(reqInt(body, "priceCents", 0, 99999999));
            if (body != null && body.containsKey("durationDays")) p.setDurationDays(reqInt(body, "durationDays", 0, 36500));
            if (body != null && body.containsKey("deviceLimit")) p.setDeviceLimit(reqInt(body, "deviceLimit", 1, 1000));
            if (body != null && body.containsKey("dailyFreeLimit")) p.setDailyFreeLimit(reqInt(body, "dailyFreeLimit", 0, 10000));
            if (body != null && body.containsKey("watermark")) p.setWatermark(reqBool(body, "watermark"));
            if (body != null && body.containsKey("enabled")) p.setEnabled(reqBool(body, "enabled"));
            if (body != null && body.containsKey("sortOrder")) p.setSortOrder(reqInt(body, "sortOrder", 0, 99999));
            MembershipPlan saved = repository.save(p);
            opLogService.log(request, admin, "PLAN_UPDATE", "PLAN", String.valueOf(saved.getId()), body == null ? null : body);
            r.put("success", true);
            r.put("data", saved);
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
        return v == null ? "" : String.valueOf(v).trim();
    }

    private String reqStr(Map<String, Object> body, String key, int maxLen) {
        String s = str(body, key);
        if (s.isEmpty()) throw new RuntimeException(key + "不能为空");
        if (s.length() > maxLen) throw new RuntimeException(key + "过长");
        return s;
    }

    private boolean reqBool(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return "true".equalsIgnoreCase(String.valueOf(v));
    }

    private int reqInt(Map<String, Object> body, String key, int min, int max) {
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
