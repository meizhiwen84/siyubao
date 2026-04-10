package cn.laobayou.siyubao.config;

import cn.laobayou.siyubao.bean.MembershipPlan;
import cn.laobayou.siyubao.repository.MembershipPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MembershipSeed {
    private final MembershipPlanRepository repository;
    private final SeedModeService seedModeService;
    private static final AtomicBoolean SEEDED = new AtomicBoolean(false);
    private static final Logger log = LoggerFactory.getLogger(MembershipSeed.class);

    public MembershipSeed(MembershipPlanRepository repository, SeedModeService seedModeService) {
        this.repository = repository;
        this.seedModeService = seedModeService;
    }

    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void seed() {
        if (!SEEDED.compareAndSet(false, true)) return;
        SeedMode mode = seedModeService.mode();
        if (mode == SeedMode.OFF) return;

        ensure("FREE", "免费用户", 0, 0, 1, 3, true, 0);

        if (mode == SeedMode.DEV) {
            ensureDefaults();
            return;
        }

        try {
            if (repository.count() > 1) return;
        } catch (Exception e) {
            log.warn("Plan seed count failed: {}", e.getMessage());
            return;
        }
        ensureDefaults();
    }

    private void ensureDefaults() {
        ensure("P_MONTH", "个人会员·月卡", 3900, 30, 1, 0, false, 10);
        ensure("P_QUARTER", "个人会员·季卡", 6900, 90, 1, 0, false, 11);
        ensure("P_YEAR", "个人会员·年卡", 9900, 365, 1, 0, false, 12);
        ensure("P_LIFE", "个人会员·终身", 12900, 36500, 1, 0, false, 13);

        ensure("T_5", "团队会员·5设备", 6900, 30, 5, 0, false, 20);
        ensure("T_10", "团队会员·10设备", 9900, 30, 10, 0, false, 21);
        ensure("T_LIFE", "团队会员·终身", 23800, 36500, 10, 0, false, 22);
    }

    private void ensure(String code, String name, int priceCents, int durationDays, int deviceLimit, int dailyFreeLimit, boolean watermark, int sortOrder) {
        try {
            MembershipPlan p = repository.findByCode(code).orElse(null);
            if (p == null) {
                p = new MembershipPlan();
                p.setCode(code);
                p.setEnabled(true);
            }
            p.setName(name);
            p.setPriceCents(priceCents);
            p.setDurationDays(durationDays);
            p.setDeviceLimit(deviceLimit);
            p.setDailyFreeLimit(dailyFreeLimit);
            p.setWatermark(watermark);
            p.setSortOrder(sortOrder);
            repository.save(p);
            return;
        } catch (Throwable e) {
            log.warn("Plan seed save failed for code={}: {}", code, e.getMessage());
        }

        try {
            MembershipPlan exist = repository.findByCode(code).orElse(null);
            if (exist == null) return;
            exist.setEnabled(true);
            exist.setName(name);
            exist.setPriceCents(priceCents);
            exist.setDurationDays(durationDays);
            exist.setDeviceLimit(deviceLimit);
            exist.setDailyFreeLimit(dailyFreeLimit);
            exist.setWatermark(watermark);
            exist.setSortOrder(sortOrder);
            repository.save(exist);
        } catch (Throwable e) {
            log.warn("Plan seed update failed for code={}: {}", code, e.getMessage());
        }
    }
}
