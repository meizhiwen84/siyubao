package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.bean.MembershipPlan;
import cn.laobayou.siyubao.bean.UserSubscription;
import cn.laobayou.siyubao.repository.MembershipPlanRepository;
import cn.laobayou.siyubao.repository.UserSubscriptionRepository;
import cn.laobayou.siyubao.service.AdminOpLogService;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/subscriptions")
public class AdminSubscriptionController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final MembershipPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final AdminOpLogService opLogService;

    public AdminSubscriptionController(
            AuthContextService authContextService,
            AuthService authService,
            MembershipPlanRepository planRepository,
            UserSubscriptionRepository subscriptionRepository,
            AdminOpLogService opLogService
    ) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.opLogService = opLogService;
    }

    @PostMapping("/grant")
    public ResponseEntity<Map<String, Object>> grant(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            Long userId = reqLong(body, "userId");
            Long planId = reqLong(body, "planId");
            MembershipPlan plan = planRepository.findById(planId).orElseThrow(() -> new RuntimeException("套餐不存在"));
            if (!Boolean.TRUE.equals(plan.getEnabled())) throw new RuntimeException("套餐未启用");

            int overrideDays = body != null && body.get("durationDays") != null ? reqInt(body, "durationDays", 0, 36500) : -1;
            int days = overrideDays >= 0 ? overrideDays : (plan.getDurationDays() == null ? 0 : Math.max(0, plan.getDurationDays()));

            Optional<UserSubscription> activeOpt = subscriptionRepository.findActiveByUserId(userId);
            if (activeOpt.isPresent()) {
                UserSubscription old = activeOpt.get();
                old.setStatus("EXPIRED");
                subscriptionRepository.save(old);
            }

            UserSubscription sub = new UserSubscription();
            sub.setUserId(userId);
            sub.setPlanId(plan.getId());
            LocalDateTime now = LocalDateTime.now();
            sub.setStartTime(now);
            sub.setEndTime(days <= 0 ? null : now.plusDays(days));
            sub.setStatus("ACTIVE");
            sub.setCreateTime(now);
            UserSubscription saved = subscriptionRepository.save(sub);

            Map<String, Object> detail = new HashMap<>();
            detail.put("userId", userId);
            detail.put("planId", plan.getId());
            detail.put("endTime", saved.getEndTime());
            opLogService.log(request, admin, "SUB_GRANT", "SUBSCRIPTION", String.valueOf(saved.getId()), detail);

            r.put("success", true);
            r.put("data", saved);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> active(@RequestParam Long userId, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);
            if (userId == null || userId <= 0) throw new RuntimeException("userId不合法");
            Optional<UserSubscription> opt = subscriptionRepository.findActiveByUserId(userId);
            if (!opt.isPresent()) {
                r.put("success", true);
                r.put("data", null);
                return ResponseEntity.ok(r);
            }
            UserSubscription sub = opt.get();
            MembershipPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
            r.put("success", true);
            r.put("data", subscriptionInfo(sub, plan));
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/renew")
    public ResponseEntity<Map<String, Object>> renew(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            Long userId = reqLong(body, "userId");
            int addDays = reqInt(body, "addDays", 1, 36500);
            UserSubscription sub = subscriptionRepository.findActiveByUserId(userId).orElseThrow(() -> new RuntimeException("用户没有有效订阅"));
            if (sub.getEndTime() == null) throw new RuntimeException("当前为永久订阅，无需续费");
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime base = sub.getEndTime().isAfter(now) ? sub.getEndTime() : now;
            sub.setEndTime(base.plusDays(addDays));
            UserSubscription saved = subscriptionRepository.save(sub);
            MembershipPlan plan = planRepository.findById(saved.getPlanId()).orElse(null);
            r.put("success", true);
            r.put("data", subscriptionInfo(saved, plan));
            Map<String, Object> detail = new HashMap<>();
            detail.put("userId", userId);
            detail.put("addDays", addDays);
            detail.put("endTime", saved.getEndTime());
            opLogService.log(request, admin, "SUB_RENEW", "SUBSCRIPTION", String.valueOf(saved.getId()), detail);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/upgrade")
    public ResponseEntity<Map<String, Object>> upgrade(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            Long userId = reqLong(body, "userId");
            Long planId = reqLong(body, "planId");
            MembershipPlan newPlan = planRepository.findById(planId).orElseThrow(() -> new RuntimeException("套餐不存在"));
            if (!Boolean.TRUE.equals(newPlan.getEnabled())) throw new RuntimeException("套餐未启用");

            Optional<UserSubscription> activeOpt = subscriptionRepository.findActiveByUserId(userId);
            if (activeOpt.isPresent()) {
                UserSubscription old = activeOpt.get();
                old.setStatus("EXPIRED");
                subscriptionRepository.save(old);
            }

            LocalDateTime now = LocalDateTime.now();
            long keepDays = 0;
            LocalDateTime oldEnd = activeOpt.map(UserSubscription::getEndTime).orElse(null);
            if (oldEnd != null && oldEnd.isAfter(now)) {
                keepDays = ceilDaysBetween(now, oldEnd);
            }
            int planDays = newPlan.getDurationDays() == null ? 0 : Math.max(0, newPlan.getDurationDays());

            UserSubscription sub = new UserSubscription();
            sub.setUserId(userId);
            sub.setPlanId(newPlan.getId());
            sub.setStartTime(now);
            if (planDays <= 0) sub.setEndTime(null);
            else sub.setEndTime(now.plusDays(keepDays + planDays));
            sub.setStatus("ACTIVE");
            sub.setCreateTime(now);
            UserSubscription saved = subscriptionRepository.save(sub);

            r.put("success", true);
            r.put("data", subscriptionInfo(saved, newPlan));
            r.put("keptDays", keepDays);
            Map<String, Object> detail = new HashMap<>();
            detail.put("userId", userId);
            detail.put("planId", newPlan.getId());
            detail.put("keptDays", keepDays);
            detail.put("endTime", saved.getEndTime());
            opLogService.log(request, admin, "SUB_UPGRADE", "SUBSCRIPTION", String.valueOf(saved.getId()), detail);
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

    private Long reqLong(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        if (v == null) throw new RuntimeException(key + "不能为空");
        if (v instanceof Number) return ((Number) v).longValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) throw new RuntimeException(key + "不能为空");
        return Long.parseLong(s);
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

    private long ceilDaysBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) return 0;
        if (!to.isAfter(from)) return 0;
        long seconds = Duration.between(from, to).getSeconds();
        long day = 86400L;
        long d = seconds / day;
        if (seconds % day != 0) d += 1;
        return Math.max(0, d);
    }

    private Map<String, Object> subscriptionInfo(UserSubscription s, MembershipPlan p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("userId", s.getUserId());
        m.put("planId", s.getPlanId());
        m.put("startTime", s.getStartTime());
        m.put("endTime", s.getEndTime());
        m.put("status", s.getStatus());
        if (p != null) {
            Map<String, Object> plan = new HashMap<>();
            plan.put("id", p.getId());
            plan.put("code", p.getCode());
            plan.put("name", p.getName());
            plan.put("durationDays", p.getDurationDays());
            plan.put("deviceLimit", p.getDeviceLimit());
            plan.put("dailyFreeLimit", p.getDailyFreeLimit());
            plan.put("watermark", p.getWatermark());
            plan.put("priceCents", p.getPriceCents());
            plan.put("enabled", p.getEnabled());
            plan.put("sortOrder", p.getSortOrder());
            m.put("plan", plan);
        }
        return m;
    }
}
