package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.repository.LocalDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class LocalDeviceService {
    private final LocalDeviceRepository repository;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public LocalDeviceService(LocalDeviceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public synchronized String getOrCreateDeviceId() {
        String cur = repository.getDeviceId();
        if (cur != null && !cur.trim().isEmpty()) return cur.trim();
        String id = "dev-" + UUID.randomUUID().toString().replace("-", "");
        repository.upsert(id, LocalDateTime.now().format(TS));
        return id;
    }
}

