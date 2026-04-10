package cn.laobayou.siyubao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SeedModeService {
    @Value("${sxjw.seed.mode:PROD}")
    private String seedMode;

    public SeedMode mode() {
        String m = seedMode == null ? "" : seedMode.trim().toUpperCase();
        if (m.isEmpty()) return SeedMode.PROD;
        try {
            return SeedMode.valueOf(m);
        } catch (Exception e) {
            return SeedMode.PROD;
        }
    }
}
