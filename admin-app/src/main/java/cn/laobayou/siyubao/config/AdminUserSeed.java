package cn.laobayou.siyubao.config;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AdminUserSeed {
    private static final AtomicBoolean SEEDED = new AtomicBoolean(false);
    private static final Logger log = LoggerFactory.getLogger(AdminUserSeed.class);
    private final AppUserRepository userRepository;
    private final SeedModeService seedModeService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${sxjw.admin.init-username:admin}")
    private String initUsername;

    @Value("${sxjw.admin.init-password:admin123456}")
    private String initPassword;

    @Value("${sxjw.admin.force-reset:false}")
    private boolean forceReset;

    public AdminUserSeed(AppUserRepository userRepository, SeedModeService seedModeService) {
        this.userRepository = userRepository;
        this.seedModeService = seedModeService;
    }

    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void seed() {
        if (!SEEDED.compareAndSet(false, true)) return;
        try {
            SeedMode mode = seedModeService.mode();
            if (mode == SeedMode.OFF) return;

            String u = norm(initUsername);
            String p = initPassword == null ? "" : initPassword;
            if (u.isEmpty()) u = "admin";
            if (p.trim().isEmpty()) p = "admin123456";

            boolean hasAdmin = userRepository.existsByRole("ADMIN");
            if (hasAdmin) {
                if (mode != SeedMode.DEV || !forceReset) return;
                log.warn("Admin seed: DEV mode with force-reset enabled; will reset password for init-username={}", u);
            } else {
                log.info("Admin seed: no ADMIN found; will create init-username={}", u);
            }

            AppUser admin = userRepository.findByUsername(u).orElse(null);
            if (admin == null) {
                admin = new AppUser();
                admin.setUsername(u);
                admin.setCreateTime(LocalDateTime.now());
            }
            admin.setEnabled(true);
            admin.setRole("ADMIN");
            admin.setPasswordHash(encoder.encode(p));
            userRepository.save(admin);
        } catch (Throwable e) {
            log.warn("Admin seed failed: {}", e.getMessage());
        }
    }

    private String norm(String s) {
        String t = s == null ? "" : s.trim();
        return t.replaceAll("\\s+", "");
    }
}
