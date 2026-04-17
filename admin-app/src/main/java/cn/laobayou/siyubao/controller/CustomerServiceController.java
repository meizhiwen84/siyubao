package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.CustomerServiceInfo;
import cn.laobayou.siyubao.bean.DeviceSession;
import cn.laobayou.siyubao.bean.Feedback;
import cn.laobayou.siyubao.repository.CustomerServiceInfoRepository;
import cn.laobayou.siyubao.repository.FeedbackRepository;
import cn.laobayou.siyubao.repository.AppUserRepository;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CustomerServiceController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final CustomerServiceInfoRepository csInfoRepository;
    private final FeedbackRepository feedbackRepository;
    private final AppUserRepository appUserRepository;

    public CustomerServiceController(
            AuthContextService authContextService,
            AuthService authService,
            CustomerServiceInfoRepository csInfoRepository,
            FeedbackRepository feedbackRepository,
            AppUserRepository appUserRepository
    ) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.csInfoRepository = csInfoRepository;
        this.feedbackRepository = feedbackRepository;
        this.appUserRepository = appUserRepository;
    }

    private AppUser requireAdmin(HttpServletRequest request) {
        AppUser u = authService.findUser(authContextService.requireSession(request).getUserId()).orElse(null);
        if (u == null) throw new RuntimeException("用户不存在");
        if (!Boolean.TRUE.equals(u.getEnabled())) throw new RuntimeException("账号已被禁用");
        if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("无权限");
        return u;
    }

    // ========== 客服信息管理 ==========

    @GetMapping("/customer-service/info")
    public ResponseEntity<Map<String, Object>> getCustomerServiceInfo() {
        Map<String, Object> r = new HashMap<>();
        try {
            CustomerServiceInfo info = csInfoRepository.findAll().stream().findFirst().orElse(null);
            Map<String, Object> data = new HashMap<>();
            if (info != null) {
                data.put("id", info.getId());
                data.put("wechatQrCodeUrl", info.getWechatQrCodeUrl());
                data.put("officialAccountQrCodeUrl", info.getOfficialAccountQrCodeUrl());
                data.put("email", info.getEmail());
            }
            r.put("success", true);
            r.put("data", data);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/admin/customer-service/update")
    public ResponseEntity<Map<String, Object>> updateCustomerServiceInfo(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);
            
            CustomerServiceInfo info = csInfoRepository.findAll().stream().findFirst().orElseGet(CustomerServiceInfo::new);
            
            if (body.containsKey("wechatQrCodeUrl")) {
                info.setWechatQrCodeUrl((String) body.get("wechatQrCodeUrl"));
            }
            if (body.containsKey("officialAccountQrCodeUrl")) {
                info.setOfficialAccountQrCodeUrl((String) body.get("officialAccountQrCodeUrl"));
            }
            if (body.containsKey("email")) {
                info.setEmail((String) body.get("email"));
            }
            info.setUpdateTime(LocalDateTime.now());
            
            info = csInfoRepository.save(info);
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", info.getId());
            data.put("wechatQrCodeUrl", info.getWechatQrCodeUrl());
            data.put("officialAccountQrCodeUrl", info.getOfficialAccountQrCodeUrl());
            data.put("email", info.getEmail());
            
            r.put("success", true);
            r.put("data", data);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    // ========== 反馈管理 ==========

    @PostMapping("/feedback/submit")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        Map<String, Object> r = new HashMap<>();
        try {
            DeviceSession session = authContextService.requireSession(request);
            Long userId = session.getUserId();
            
            String content = (String) body.get("content");
            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("内容不能为空");
            }
            
            String contact = body.containsKey("contact") ? (String) body.get("contact") : null;
            
            Feedback feedback = new Feedback();
            feedback.setUserId(userId);
            feedback.setContent(content.trim());
            feedback.setContact(contact);
            feedback = feedbackRepository.save(feedback);
            
            r.put("success", true);
            r.put("data", feedback);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @GetMapping("/admin/feedbacks/list")
    public ResponseEntity<Map<String, Object>> listFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);
            
            int p = Math.max(page, 0);
            int s = Math.min(Math.max(size, 1), 100);
            Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createTime"));
            
            Page<Feedback> feedbackPage = feedbackRepository.findAll(pageable);
            
            // 构建返回数据，包含用户信息
            List<Map<String, Object>> data = new ArrayList<>();
            for (Feedback feedback : feedbackPage.getContent()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", feedback.getId());
                item.put("content", feedback.getContent());
                item.put("contact", feedback.getContact());
                item.put("createTime", feedback.getCreateTime());
                
                // 添加用户信息
                AppUser user = appUserRepository.findById(feedback.getUserId()).orElse(null);
                if (user != null) {
                    item.put("userNo", user.getUserNo());
                    item.put("username", user.getUsername());
                }
                
                data.add(item);
            }
            
            r.put("success", true);
            r.put("data", data);
            r.put("total", feedbackPage.getTotalElements());
            r.put("pages", feedbackPage.getTotalPages());
            r.put("page", p);
            r.put("size", s);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }
}
