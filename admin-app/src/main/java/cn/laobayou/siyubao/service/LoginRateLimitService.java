package cn.laobayou.siyubao.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimitService {
    private static class Bucket {
        LocalDateTime windowStart;
        int count;

        Bucket(LocalDateTime windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void checkAndConsume(String ip, String username) {
        String k = (ip == null ? "unknown" : ip.trim()) + "|" + norm(username);
        LocalDateTime now = LocalDateTime.now();
        Bucket b = buckets.get(k);
        if (b == null || Duration.between(b.windowStart, now).getSeconds() >= 60) {
            b = new Bucket(now, 0);
        }
        b.count += 1;
        buckets.put(k, b);
        if (b.count > 8) {
            throw new RuntimeException("登录过于频繁，请稍后再试");
        }
    }

    private String norm(String s) {
        String t = s == null ? "" : s.trim().toLowerCase();
        if (t.length() > 64) t = t.substring(0, 64);
        return t;
    }
}
