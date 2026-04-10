package cn.laobayou.siyubao.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class ClientIpService {
    @Value("${sxjw.trust-proxy:false}")
    private boolean trustProxy;

    public String ip(HttpServletRequest request) {
        if (request == null) return "unknown";
        if (trustProxy) {
            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.trim().isEmpty()) {
                String[] parts = xf.split(",");
                if (parts.length > 0) return parts[0].trim();
            }
            String xr = request.getHeader("X-Real-IP");
            if (xr != null && !xr.trim().isEmpty()) return xr.trim();
        }
        String ra = request.getRemoteAddr();
        return ra == null || ra.trim().isEmpty() ? "unknown" : ra.trim();
    }
}
