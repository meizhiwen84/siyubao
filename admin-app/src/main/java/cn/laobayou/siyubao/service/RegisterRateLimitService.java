package cn.laobayou.siyubao.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegisterRateLimitService {
    private final Map<String, LocalDateTime> minuteLast = new ConcurrentHashMap<>();
    private final Map<String, DayCount> dayCount = new ConcurrentHashMap<>();
    private final AppSettingService settingService;

    public RegisterRateLimitService(AppSettingService settingService) {
        this.settingService = settingService;
    }

    public void checkAndConsume(String ip) {
        String k = ip == null ? "" : ip.trim();
        if (k.isEmpty()) k = "unknown";

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime last = minuteLast.get(k);
        int minInterval = Math.max(1, settingService.getInt("register.minute_interval_seconds", 60));
        if (last != null && java.time.Duration.between(last, now).getSeconds() < minInterval) {
            throw new RuntimeException("注册过于频繁，请稍后再试");
        }

        LocalDate today = LocalDate.now();
        DayCount dc = dayCount.get(k);
        if (dc == null || !today.equals(dc.day)) {
            dc = new DayCount(today, 0);
        }
        int dayLimit = Math.max(1, settingService.getInt("register.day_limit", 3));
        if (dc.count >= dayLimit) {
            throw new RuntimeException("今日注册次数已达上限");
        }
        dc.count++;
        dayCount.put(k, dc);
        minuteLast.put(k, now);
    }

    private static final class DayCount {
        private final LocalDate day;
        private int count;

        private DayCount(LocalDate day, int count) {
            this.day = day;
            this.count = count;
        }
    }
}
