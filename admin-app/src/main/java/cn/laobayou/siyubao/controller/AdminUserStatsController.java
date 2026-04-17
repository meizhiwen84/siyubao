package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DailyUsage;
import cn.laobayou.siyubao.bean.MembershipPlan;
import cn.laobayou.siyubao.bean.UserSubscription;
import cn.laobayou.siyubao.repository.AppUserRepository;
import cn.laobayou.siyubao.repository.DailyUsageRepository;
import cn.laobayou.siyubao.repository.MembershipPlanRepository;
import cn.laobayou.siyubao.repository.UserSubscriptionRepository;
import cn.laobayou.siyubao.service.AdminOpLogService;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/user-stats")
public class AdminUserStatsController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final AppUserRepository userRepository;
    private final DailyUsageRepository dailyUsageRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final MembershipPlanRepository planRepository;
    private final AdminOpLogService opLogService;

    public AdminUserStatsController(
            AuthContextService authContextService,
            AuthService authService,
            AppUserRepository userRepository,
            DailyUsageRepository dailyUsageRepository,
            UserSubscriptionRepository subscriptionRepository,
            MembershipPlanRepository planRepository,
            AdminOpLogService opLogService
    ) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.opLogService = opLogService;
    }

    private AppUser requireAdmin(HttpServletRequest request) {
        AppUser u = authService.findUser(authContextService.requireSession(request).getUserId()).orElse(null);
        if (u == null) throw new RuntimeException("用户不存在");
        if (!Boolean.TRUE.equals(u.getEnabled())) throw new RuntimeException("账号已被禁用");
        if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("无权限");
        return u;
    }

    @GetMapping("/daily-usage")
    public ResponseEntity<Map<String, Object>> dailyUsage(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size,
            HttpServletRequest request
    ) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);
            
            Long actualUserId;
            if (userNo != null && !userNo.trim().isEmpty()) {
                AppUser u = userRepository.findByUserNo(userNo.trim()).orElseThrow(() -> new RuntimeException("用户不存在"));
                actualUserId = u.getId();
            } else if (userId != null && userId > 0) {
                actualUserId = userId;
            } else {
                throw new RuntimeException("必须提供userId或userNo");
            }

            int pageNum = Math.max(1, page) - 1;
            int pageSize = Math.max(1, Math.min(100, size));
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "day"));

            Page<DailyUsage> usagePage = dailyUsageRepository.findByUserId(actualUserId, pageable);
            List<DailyUsage> usages = usagePage.getContent();

            List<Map<String, Object>> data = new ArrayList<>();
            for (DailyUsage u : usages) {
                Map<String, Object> m = new HashMap<>();
                m.put("date", u.getDay());
                m.put("count", u.getCount());
                data.add(m);
            }

            r.put("success", true);
            r.put("data", data);
            r.put("total", usagePage.getTotalElements());
            r.put("page", page);
            r.put("size", size);
            r.put("totalPages", usagePage.getTotalPages());
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<Map<String, Object>> subscriptions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);
            
            Long actualUserId;
            if (userNo != null && !userNo.trim().isEmpty()) {
                AppUser u = userRepository.findByUserNo(userNo.trim()).orElseThrow(() -> new RuntimeException("用户不存在"));
                actualUserId = u.getId();
            } else if (userId != null && userId > 0) {
                actualUserId = userId;
            } else {
                throw new RuntimeException("必须提供userId或userNo");
            }

            int pageNum = Math.max(1, page) - 1;
            int pageSize = Math.max(1, Math.min(100, size));
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "startTime", "id"));

            Page<UserSubscription> subPage = subscriptionRepository.findByUserId(actualUserId, pageable);
            List<UserSubscription> subs = subPage.getContent();

            Set<Long> planIds = subs.stream().map(UserSubscription::getPlanId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, MembershipPlan> plans = planIds.isEmpty()
                    ? new HashMap<>()
                    : planRepository.findAllById(planIds).stream().collect(Collectors.toMap(MembershipPlan::getId, x -> x));

            List<Map<String, Object>> data = new ArrayList<>();
            for (UserSubscription s : subs) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", s.getId());
                m.put("planId", s.getPlanId());
                MembershipPlan p = s.getPlanId() == null ? null : plans.get(s.getPlanId());
                if (p != null) {
                    m.put("planName", p.getName());
                }
                m.put("startTime", s.getStartTime());
                m.put("endTime", s.getEndTime());
                m.put("isLifetime", s.getEndTime() == null);
                m.put("status", s.getStatus());
                data.add(m);
            }

            r.put("success", true);
            r.put("data", data);
            r.put("total", subPage.getTotalElements());
            r.put("page", page);
            r.put("size", size);
            r.put("totalPages", subPage.getTotalPages());
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }
}
