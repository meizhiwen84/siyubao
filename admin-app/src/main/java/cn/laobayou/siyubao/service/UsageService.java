package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.MembershipPlan;
import cn.laobayou.siyubao.repository.DailyUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UsageService {
    private final DailyUsageRepository dailyUsageRepository;
    private static final int DEFAULT_FREE_DAILY_LIMIT = 3;

    public UsageService(DailyUsageRepository dailyUsageRepository) {
        this.dailyUsageRepository = dailyUsageRepository;
    }

    public int todayCount(Long userId) {
        if (userId == null) return 0;
        return dailyUsageRepository.findByUserIdAndDay(userId, LocalDate.now()).map(d -> d.getCount() == null ? 0 : d.getCount()).orElse(0);
    }

    @Transactional
    public void incrementToday(Long userId) {
        if (userId == null) return;
        dailyUsageRepository.increment(userId, LocalDate.now());
    }

    public boolean allowedToGenerate(Long userId, MembershipPlan plan) {
        if (userId == null || userId <= 0) return false;
        if (plan == null) return false;
        Integer limit = plan.getDailyFreeLimit();
        if ("FREE".equalsIgnoreCase(plan.getCode())) {
            int freeLimit = (limit == null || limit <= 0) ? DEFAULT_FREE_DAILY_LIMIT : limit;
            int used = todayCount(userId);
            return used < freeLimit;
        }
        if (limit == null || limit <= 0) return true;
        int used = todayCount(userId);
        return used < limit;
    }
}
