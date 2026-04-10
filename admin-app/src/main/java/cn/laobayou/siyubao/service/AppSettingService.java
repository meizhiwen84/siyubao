package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.AppSetting;
import cn.laobayou.siyubao.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AppSettingService {
    private final AppSettingRepository repository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public AppSettingService(AppSettingRepository repository) {
        this.repository = repository;
    }

    public String getString(String key, String defaultValue) {
        String k = normKey(key);
        if (k.isEmpty()) return defaultValue;
        String cached = cache.get(k);
        if (cached != null) return cached;
        Optional<AppSetting> opt = repository.findById(k);
        if (!opt.isPresent()) return defaultValue;
        String v = opt.get().getValue();
        if (v == null) v = "";
        cache.put(k, v);
        return v;
    }

    public int getInt(String key, int defaultValue) {
        String s = getString(key, null);
        if (s == null) return defaultValue;
        String t = s.trim();
        if (t.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(t);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Transactional
    public void set(String key, String value) {
        String k = normKey(key);
        if (k.isEmpty()) throw new RuntimeException("key不能为空");
        AppSetting s = repository.findById(k).orElseGet(AppSetting::new);
        s.setKey(k);
        s.setValue(value == null ? "" : value);
        s.setUpdateTime(LocalDateTime.now());
        repository.save(s);
        cache.put(k, s.getValue());
    }

    private String normKey(String key) {
        String t = key == null ? "" : key.trim();
        if (t.isEmpty()) return "";
        return t.replaceAll("\\s+", "");
    }
}
