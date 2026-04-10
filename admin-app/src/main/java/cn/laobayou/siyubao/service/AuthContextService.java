package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.repository.DeviceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthContextService {
    private final DeviceSessionRepository deviceSessionRepository;

    public AuthContextService(DeviceSessionRepository deviceSessionRepository) {
        this.deviceSessionRepository = deviceSessionRepository;
    }

    public String bearerToken(HttpServletRequest request) {
        if (request == null) return null;
        String h = request.getHeader("Authorization");
        if (h == null) return null;
        String t = h.trim();
        if (t.toLowerCase().startsWith("bearer ")) return t.substring(7).trim();
        return null;
    }

    @Transactional
    public DeviceSession requireSession(HttpServletRequest request) {
        String token = bearerToken(request);
        if (token == null || token.isEmpty()) throw new RuntimeException("未登录");
        Optional<DeviceSession> opt = deviceSessionRepository.findByToken(token);
        if (!opt.isPresent()) throw new RuntimeException("未登录");
        DeviceSession s = opt.get();
        if (Boolean.TRUE.equals(s.getRevoked())) throw new RuntimeException("已被挤下线");
        deviceSessionRepository.touch(s.getId(), LocalDateTime.now());
        return s;
    }
}

