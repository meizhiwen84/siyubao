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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
        log.debug("获取套餐列表请求");
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
        log.debug("获取套餐列表成功，共 {} 个套餐", list.size());
        return ResponseEntity.ok(r);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            String ip = clientIpService.ip(request);
            log.info("用户注册请求 - 用户名: {}, IP: {}", str(body, "username"), ip);
            rateLimitService.checkAndConsume(ip);

            String username = str(body, "username");
            String password = str(body, "password");
            AppUser user = authService.register(username, password);
            
            // 生成并设置用户编号
            String userNo = userNoService.generateUniqueUserNo();
            user.setUserNo(userNo);
            user = authService.saveUser(user);

            log.info("用户注册成功 - 用户名: {}, 用户ID: {}, 用户编号: {}", username, user.getId(), userNo);
            r.put("success", true);
            r.put("userId", user.getId());
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            log.error("用户注册失败", e);
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
            log.info("用户登录请求 - 用户名: {}, IP: {}, 设备ID: {}", username, ip, deviceId);
            loginRateLimitService.checkAndConsume(ip, username);
            DeviceSession s = authService.login(username, password, deviceId);
            AppUser user = authService.findUser(s.getUserId()).orElseThrow(() -> new RuntimeException("用户不存在"));

            log.info("用户登录成功 - 用户名: {}, 用户ID: {}, 用户编号: {}", username, user.getId(), user.getUserNo());
            r.put("success", true);
            r.put("token", s.getToken());
            r.put("user", userInfo(user));
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            log.error("用户登录失败 - 用户名: {}", str(body, "username"), e);
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
