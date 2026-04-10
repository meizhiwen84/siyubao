package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.repository.AppUserRepository;
import cn.laobayou.siyubao.repository.DeviceSessionRepository;
import cn.laobayou.siyubao.service.AdminOpLogService;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/devices")
public class AdminDeviceController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final AppUserRepository userRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final AdminOpLogService opLogService;

    public AdminDeviceController(
            AuthContextService authContextService,
            AuthService authService,
            AppUserRepository userRepository,
            DeviceSessionRepository deviceSessionRepository,
            AdminOpLogService opLogService
    ) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.opLogService = opLogService;
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request
    ) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);

            String kw = keyword == null ? null : keyword.trim();
            if (kw != null && kw.isEmpty()) kw = null;

            List<DeviceSession> sessions;
            if (userId != null && userId > 0) {
                sessions = deviceSessionRepository.findAllByUserIdOrderByLastSeenTimeDescIdDesc(userId);
            } else if (kw != null) {
                String kwp = "%" + kw.toLowerCase() + "%";
                List<Long> ids = userRepository.search(kwp, org.springframework.data.domain.PageRequest.of(0, 200))
                        .getContent()
                        .stream()
                        .map(AppUser::getId)
                        .collect(Collectors.toList());
                sessions = ids.isEmpty() ? Collections.emptyList() : deviceSessionRepository.findAllByUserIdInOrderByLastSeenTimeDescIdDesc(ids);
            } else {
                sessions = deviceSessionRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastSeenTime", "id"));
            }

            Set<Long> uidSet = sessions.stream().map(DeviceSession::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, AppUser> users = uidSet.isEmpty()
                    ? new HashMap<>()
                    : userRepository.findAllById(uidSet).stream().collect(Collectors.toMap(AppUser::getId, x -> x));

            List<Map<String, Object>> data = new ArrayList<>();
            for (DeviceSession s : sessions) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", s.getId());
                m.put("userId", s.getUserId());
                AppUser u = s.getUserId() == null ? null : users.get(s.getUserId());
                if (u != null) m.put("username", u.getUsername());
                m.put("deviceId", s.getDeviceId());
                String token = s.getToken();
                if (token != null) {
                    String t = token.trim();
                    if (t.length() > 6) m.put("tokenSuffix", t.substring(t.length() - 6));
                    else if (!t.isEmpty()) m.put("tokenSuffix", t);
                }
                m.put("createTime", s.getCreateTime());
                m.put("lastSeenTime", s.getLastSeenTime());
                m.put("revoked", s.getRevoked());
                data.add(m);
            }

            r.put("success", true);
            r.put("data", data);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.status(401).body(r);
        }
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Map<String, Object>> revoke(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            if (id == null || id <= 0) throw new RuntimeException("id不合法");
            deviceSessionRepository.revokeId(id);
            Map<String, Object> detail = new HashMap<>();
            detail.put("sessionId", id);
            opLogService.log(request, admin, "DEVICE_REVOKE", "DEVICE_SESSION", String.valueOf(id), detail);
            r.put("success", true);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/revoke-others")
    public ResponseEntity<Map<String, Object>> revokeOthers(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            Long userId = reqLong(body, "userId");
            Long keepId = reqLong(body, "keepId");
            deviceSessionRepository.revokeOthers(userId, keepId);
            Map<String, Object> detail = new HashMap<>();
            detail.put("userId", userId);
            detail.put("keepId", keepId);
            opLogService.log(request, admin, "DEVICE_REVOKE_OTHERS", "USER", String.valueOf(userId), detail);
            r.put("success", true);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/revoke-all")
    public ResponseEntity<Map<String, Object>> revokeAll(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            Long userId = reqLong(body, "userId");
            deviceSessionRepository.revokeAllByUserId(userId);
            Map<String, Object> detail = new HashMap<>();
            detail.put("userId", userId);
            opLogService.log(request, admin, "DEVICE_REVOKE_ALL", "USER", String.valueOf(userId), detail);
            r.put("success", true);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    private AppUser requireAdmin(HttpServletRequest request) {
        DeviceSession s = authContextService.requireSession(request);
        AppUser u = authService.findUser(s.getUserId()).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!Boolean.TRUE.equals(u.getEnabled())) throw new RuntimeException("账号已被禁用");
        if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("无权限");
        return u;
    }

    private Long reqLong(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        if (v == null) throw new RuntimeException(key + "不能为空");
        if (v instanceof Number) return ((Number) v).longValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) throw new RuntimeException(key + "不能为空");
        return Long.parseLong(s);
    }
}
