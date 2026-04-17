package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.bean.MembershipPlan;
import cn.laobayou.siyubao.bean.UserSubscription;
import cn.laobayou.siyubao.repository.AppUserRepository;
import cn.laobayou.siyubao.repository.MembershipPlanRepository;
import cn.laobayou.siyubao.repository.UserSubscriptionRepository;
import cn.laobayou.siyubao.service.AdminOpLogService;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final AppUserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final MembershipPlanRepository planRepository;
    private final AdminOpLogService opLogService;

    public AdminUserController(
            AuthContextService authContextService,
            AuthService authService,
            AppUserRepository userRepository,
            UserSubscriptionRepository subscriptionRepository,
            MembershipPlanRepository planRepository,
            AdminOpLogService opLogService
    ) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.opLogService = opLogService;
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
            Page<AppUser> res = userRepository.search(kw, PageRequest.of(p, s));

            List<AppUser> users = res.getContent();
            List<Long> userIds = users.stream().map(AppUser::getId).collect(Collectors.toList());
            Map<Long, UserSubscription> activeSubs = new HashMap<>();
            if (!userIds.isEmpty()) {
                for (UserSubscription sub : subscriptionRepository.findActiveByUserIds(userIds)) {
                    if (sub == null || sub.getUserId() == null) continue;
                    if (!activeSubs.containsKey(sub.getUserId())) activeSubs.put(sub.getUserId(), sub);
                }
            }
            Map<Long, MembershipPlan> planMap = new HashMap<>();
            if (!activeSubs.isEmpty()) {
                List<Long> planIds = activeSubs.values().stream().map(UserSubscription::getPlanId).collect(Collectors.toList());
                for (MembershipPlan plan : planRepository.findAllById(planIds)) {
                    if (plan != null && plan.getId() != null) planMap.put(plan.getId(), plan);
                }
            }

            List<Map<String, Object>> data = new ArrayList<>();
            for (AppUser u : users) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("userNo", u.getUserNo());
                m.put("username", u.getUsername());
                m.put("role", u.getRole());
                m.put("enabled", u.getEnabled());
                m.put("createTime", u.getCreateTime());
                UserSubscription sub = u.getId() == null ? null : activeSubs.get(u.getId());
                if (sub != null) {
                    m.put("planId", sub.getPlanId());
                    m.put("subscriptionEndTime", sub.getEndTime());
                    MembershipPlan plan = sub.getPlanId() == null ? null : planMap.get(sub.getPlanId());
                    if (plan != null) {
                        m.put("planCode", plan.getCode());
                        m.put("planName", plan.getName());
                    }
                } else {
                    m.put("subscriptionEndTime", null);
                }
                data.add(m);
            }
            r.put("success", true);
            r.put("data", data);
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

    @PostMapping("/{id}/enabled")
    public ResponseEntity<Map<String, Object>> setEnabled(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        return setEnabledByUserNoOrId(id, null, body, request);
    }
    
    @PostMapping("/by-userNo/{userNo}/enabled")
    public ResponseEntity<Map<String, Object>> setEnabledByUserNo(@PathVariable String userNo, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        return setEnabledByUserNoOrId(null, userNo, body, request);
    }
    
    private ResponseEntity<Map<String, Object>> setEnabledByUserNoOrId(Long id, String userNo, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            AppUser u;
            if (userNo != null && !userNo.trim().isEmpty()) {
                u = userRepository.findByUserNo(userNo.trim()).orElseThrow(() -> new RuntimeException("用户不存在"));
            } else if (id != null) {
                u = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
            } else {
                throw new RuntimeException("必须提供id或userNo");
            }
            boolean enabled = body != null && Boolean.TRUE.equals(body.get("enabled"));
            u.setEnabled(enabled);
            userRepository.save(u);
            Map<String, Object> detail = new HashMap<>();
            detail.put("userId", u.getId());
            detail.put("userNo", u.getUserNo());
            detail.put("enabled", enabled);
            opLogService.log(request, admin, "USER_SET_ENABLED", "USER", String.valueOf(u.getId()), detail);
            r.put("success", true);
            r.put("data", u);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return resetPasswordByUserNoOrId(id, null, body, request);
    }
    
    @PostMapping("/by-userNo/{userNo}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPasswordByUserNo(@PathVariable String userNo, @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return resetPasswordByUserNoOrId(null, userNo, body, request);
    }
    
    private ResponseEntity<Map<String, Object>> resetPasswordByUserNoOrId(Long id, String userNo, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            AppUser u;
            if (userNo != null && !userNo.trim().isEmpty()) {
                u = userRepository.findByUserNo(userNo.trim()).orElseThrow(() -> new RuntimeException("用户不存在"));
            } else if (id != null) {
                u = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
            } else {
                throw new RuntimeException("必须提供id或userNo");
            }
            String newPassword = body == null ? null : (body.get("newPassword") == null ? null : String.valueOf(body.get("newPassword")));
            String pwd = authService.resetPassword(u.getId(), newPassword);
            Map<String, Object> detail = new HashMap<>();
            detail.put("userId", u.getId());
            detail.put("userNo", u.getUserNo());
            opLogService.log(request, admin, "USER_RESET_PASSWORD", "USER", String.valueOf(u.getId()), detail);
            r.put("success", true);
            r.put("newPassword", pwd);
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
}
