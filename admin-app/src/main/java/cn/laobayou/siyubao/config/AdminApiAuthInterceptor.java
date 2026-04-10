package cn.laobayou.siyubao.config;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AdminApiAuthInterceptor implements HandlerInterceptor {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AdminApiAuthInterceptor(AuthContextService authContextService, AuthService authService, ObjectMapper objectMapper) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            DeviceSession s = authContextService.requireSession(request);
            AppUser u = authService.findUser(s.getUserId()).orElseThrow(() -> new RuntimeException("用户不存在"));
            if (!Boolean.TRUE.equals(u.getEnabled())) throw new RuntimeException("账号已被禁用");
            if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("无权限");
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> r = new HashMap<>();
            r.put("success", false);
            r.put("message", e.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(r));
            return false;
        }
    }
}
