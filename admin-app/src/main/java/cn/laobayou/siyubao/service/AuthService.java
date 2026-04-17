package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.bean.MembershipPlan;
import cn.laobayou.siyubao.repository.AppUserRepository;
import cn.laobayou.siyubao.repository.DeviceSessionRepository;
import cn.laobayou.siyubao.repository.MembershipPlanRepository;
import cn.laobayou.siyubao.repository.UserSubscriptionRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    private static final SecureRandom RND = new SecureRandom();

    private final AppUserRepository userRepository;
    private final MembershipPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final MembershipService membershipService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(
            AppUserRepository userRepository,
            MembershipPlanRepository planRepository,
            UserSubscriptionRepository subscriptionRepository,
            DeviceSessionRepository deviceSessionRepository,
            MembershipService membershipService
    ) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.membershipService = membershipService;
    }

    @Transactional
    public AppUser register(String username, String password) {
        String u = normUsername(username);
        if (u.isEmpty()) throw new RuntimeException("用户名不能为空");
        if (u.length() < 3 || u.length() > 20) throw new RuntimeException("用户名长度需在3-20之间");
        if (password == null || password.trim().isEmpty()) throw new RuntimeException("密码不能为空");
        if (password.length() < 6 || password.length() > 64) throw new RuntimeException("密码长度需在6-64之间");
        if (userRepository.findByUsername(u).isPresent()) throw new RuntimeException("用户名已存在");

        AppUser user = new AppUser();
        user.setUsername(u);
        user.setPasswordHash(encoder.encode(password));
        user.setEnabled(true);
        user.setRole("USER");
        user.setCreateTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public DeviceSession login(String username, String password, String deviceId) {
        String u = normUsername(username);
        AppUser user = userRepository.findByUsername(u).orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled())) throw new RuntimeException("账号已被禁用");
        if (!encoder.matches(password == null ? "" : password, user.getPasswordHash())) throw new RuntimeException("用户名或密码错误");

        MembershipPlan plan = membershipService.resolvePlan(user.getId());
        int limit = plan.getDeviceLimit() == null ? 1 : Math.max(1, plan.getDeviceLimit());

        String d = deviceId == null ? "" : deviceId.trim();
        if (d.isEmpty()) d = "unknown";

        DeviceSession s = new DeviceSession();
        s.setUserId(user.getId());
        s.setDeviceId(d);
        s.setToken(newToken());
        s.setCreateTime(LocalDateTime.now());
        s.setLastSeenTime(LocalDateTime.now());
        s.setRevoked(false);
        DeviceSession saved = deviceSessionRepository.save(s);

        List<DeviceSession> actives = deviceSessionRepository.findAllByUserIdAndRevokedFalseOrderByLastSeenTimeDescIdDesc(user.getId());
        if (actives.size() > limit) {
            List<Long> revoke = actives.subList(limit, actives.size()).stream().map(DeviceSession::getId).collect(java.util.stream.Collectors.toList());
            deviceSessionRepository.revokeIds(revoke);
        }

        return saved;
    }

    public Optional<AppUser> findUser(Long userId) {
        return userId == null ? Optional.empty() : userRepository.findById(userId);
    }

    @Transactional
    public AppUser saveUser(AppUser user) {
        return userRepository.save(user);
    }

    @Transactional
    public String resetPassword(Long userId, String newPassword) {
        if (userId == null || userId <= 0) throw new RuntimeException("用户不存在");
        AppUser u = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        String p = (newPassword == null ? "" : newPassword.trim());
        if (p.isEmpty()) {
            p = randomPassword(12);
        }
        if (p.length() < 6 || p.length() > 64) throw new RuntimeException("密码长度需在6-64之间");
        u.setPasswordHash(encoder.encode(p));
        userRepository.save(u);
        return p;
    }

    private String normUsername(String username) {
        String t = username == null ? "" : username.trim();
        return t.replaceAll("\\s+", "");
    }

    private String newToken() {
        byte[] b = new byte[24];
        RND.nextBytes(b);
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) {
            sb.append(Character.forDigit((x >> 4) & 0xF, 16));
            sb.append(Character.forDigit(x & 0xF, 16));
        }
        return sb.toString();
    }

    private String randomPassword(int len) {
        int n = Math.max(8, Math.min(len, 32));
        final char[] chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#%_-".toCharArray();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(chars[RND.nextInt(chars.length)]);
        }
        return sb.toString();
    }
}
