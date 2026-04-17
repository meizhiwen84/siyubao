package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.repository.LocalUserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class LocalUserSessionService {
    private final LocalUserSessionRepository repository;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public LocalUserSessionService(LocalUserSessionRepository repository) {
        this.repository = repository;
    }

    public synchronized String token() {
        String t = repository.getToken();
        return t == null ? null : t.trim();
    }

    public synchronized Long userId() {
        return repository.getUserId();
    }

    public synchronized String username() {
        String u = repository.getUsername();
        return u == null ? null : u.trim();
    }

    public synchronized String userNo() {
        String u = repository.getUserNo();
        return u == null ? null : u.trim();
    }

    @Transactional
    public synchronized void save(String token, Long userId, String username, String userNo) {
        String t = token == null ? "" : token.trim();
        String u = username == null ? "" : username.trim();
        if (t.isEmpty()) throw new RuntimeException("token不能为空");
        if (u.isEmpty()) throw new RuntimeException("username不能为空");
        if (userId == null) throw new RuntimeException("userId不能为空");
        repository.deleteByIdOne();
        repository.insert(t, userId, u, userNo, LocalDateTime.now().format(TS));
    }

    @Transactional
    public synchronized void clear() {
        repository.clearAll();
    }
}

