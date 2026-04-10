package cn.laobayou.siyubao.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupInfoLogger {
    private static final Logger log = LoggerFactory.getLogger(StartupInfoLogger.class);
    private final Environment env;
    private final SeedModeService seedModeService;

    public StartupInfoLogger(Environment env, SeedModeService seedModeService) {
        this.env = env;
        this.seedModeService = seedModeService;
    }

    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onReady() {
        String port = env.getProperty("server.port");
        String dbUrl = env.getProperty("spring.datasource.url");
        String adminUser = env.getProperty("sxjw.admin.init-username");
        boolean forceReset = Boolean.parseBoolean(env.getProperty("sxjw.admin.force-reset", "false"));
        SeedMode mode = seedModeService.mode();
        log.info("Admin app ready: port={}, dbUrl={}, seedMode={}, initAdmin={}, forceReset={}", port, dbUrl, mode, adminUser, forceReset);
    }
}
