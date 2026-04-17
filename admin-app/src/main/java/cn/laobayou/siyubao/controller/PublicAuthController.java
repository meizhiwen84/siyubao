package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.bean.MembershipPlan;
import cn.laobayou.siyubao.repository.MembershipPlanRepository;
import cn.laobayou.siyubao.service.AuthService;
import cn.laobayou.siyubao.service.AppSettingService;
import cn.laobayou.siyubao.service.ClientIpService;
import cn.laobayou.siyubao.service.LoginRateLimitService;
import cn.laobayou.siyubao.service.RegisterRateLimitService;
import cn.laobayou.siyubao.service.UserNoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
public class PublicAuthController {
    private final AuthService authService;
    private final RegisterRateLimitService rateLimitService;
    private final MembershipPlanRepository planRepository;
    private final AppSettingService settingService;
    private final ClientIpService clientIpService;
    private final LoginRateLimitService loginRateLimitService;
    private final UserNoService userNoService;

    public PublicAuthController(AuthService authService, RegisterRateLimitService rateLimitService, MembershipPlanRepository planRepository, AppSettingService settingService, ClientIpService clientIpService, LoginRateLimitService loginRateLimitService, UserNoService userNoService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.planRepository = planRepository;
        this.settingService = settingService;
        this.clientIpService = clientIpService;
        this.loginRateLimitService = loginRateLimitService;
        this.userNoService = userNoService;
    }

    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> plans() {
        Map<String, Object> r = new HashMap<>();
        List<MembershipPlan> list = planRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc();
        r.put("success", true);
        r.put("plans", list.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("code", p.getCode());
            m.put("name", p.getName());
            m.put("priceCents", p.getPriceCents());
            m.put("durationDays", p.getDurationDays());
            m.put("deviceLimit", p.getDeviceLimit());
            m.put("dailyFreeLimit", p.getDailyFreeLimit());
            m.put("watermark", p.getWatermark());
            m.put("sortOrder", p.getSortOrder());
            return m;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(r);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            String ip = clientIpService.ip(request);
            rateLimitService.checkAndConsume(ip);

            String username = str(body, "username");
            String password = str(body, "password");
            AppUser user = authService.register(username, password);
            
            // 生成并设置用户编号
            String userNo = userNoService.generateUniqueUserNo();
            user.setUserNo(userNo);
            user = authService.saveUser(user);

            r.put("success", true);
            r.put("userId", user.getId());
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            String ip = clientIpService.ip(request);
            String username = str(body, "username");
            String password = str(body, "password");
            String deviceId = str(body, "deviceId");
            loginRateLimitService.checkAndConsume(ip, username);
            DeviceSession s = authService.login(username, password, deviceId);
            AppUser user = authService.findUser(s.getUserId()).orElseThrow(() -> new RuntimeException("用户不存在"));

            r.put("success", true);
            r.put("token", s.getToken());
            r.put("user", userInfo(user));
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
        m.put("userNo", u.getUserNo());
        m.put("username", u.getUsername());
        m.put("role", u.getRole());
        m.put("enabled", u.getEnabled());
        return m;
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
