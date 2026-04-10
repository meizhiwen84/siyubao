package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.AdminOpLog;
import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.repository.AdminOpLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminOpLogService {
    private final AdminOpLogRepository repository;
    private final ObjectMapper objectMapper;
    private final ClientIpService clientIpService;

    public AdminOpLogService(AdminOpLogRepository repository, ObjectMapper objectMapper, ClientIpService clientIpService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clientIpService = clientIpService;
    }

    @Transactional
    public void log(HttpServletRequest request, AppUser admin, String action, String targetType, String targetId, Object detail) {
        if (admin == null || admin.getId() == null) return;
        String a = action == null ? "" : action.trim();
        if (a.isEmpty()) return;
        AdminOpLog l = new AdminOpLog();
        l.setAdminUserId(admin.getId());
        l.setAdminUsername(admin.getUsername() == null ? "" : admin.getUsername());
        l.setAction(a);
        l.setTargetType(targetType == null ? null : targetType.trim());
        l.setTargetId(targetId == null ? null : targetId.trim());
        l.setIp(clientIpService.ip(request));
        l.setCreateTime(LocalDateTime.now());
        if (detail != null) {
            l.setDetailJson(toJsonSafe(detail));
        }
        repository.save(l);
    }

    private String toJsonSafe(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            Map<String, Object> m = new HashMap<>();
            m.put("error", "json");
            m.put("value", String.valueOf(obj));
            try {
                return objectMapper.writeValueAsString(m);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
