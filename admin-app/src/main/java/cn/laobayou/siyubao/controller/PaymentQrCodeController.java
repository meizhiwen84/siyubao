package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.bean.PaymentQrCode;
import cn.laobayou.siyubao.repository.PaymentQrCodeRepository;
import cn.laobayou.siyubao.service.AdminOpLogService;
import cn.laobayou.siyubao.service.AuthContextService;
import cn.laobayou.siyubao.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PaymentQrCodeController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final PaymentQrCodeRepository qrCodeRepository;
    private final AdminOpLogService opLogService;

    public PaymentQrCodeController(
            AuthContextService authContextService,
            AuthService authService,
            PaymentQrCodeRepository qrCodeRepository,
            AdminOpLogService opLogService
    ) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.qrCodeRepository = qrCodeRepository;
        this.opLogService = opLogService;
    }

    private AppUser requireAdmin(HttpServletRequest request) {
        AppUser u = authService.findUser(authContextService.requireSession(request).getUserId()).orElse(null);
        if (u == null) throw new RuntimeException("用户不存在");
        if (!Boolean.TRUE.equals(u.getEnabled())) throw new RuntimeException("账号已被禁用");
        if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("无权限");
        return u;
    }

    @GetMapping("/admin/qr-codes/list")
    public ResponseEntity<Map<String, Object>> list(HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);
            List<PaymentQrCode> list = qrCodeRepository.findAllByOrderBySortOrderAsc();
            r.put("success", true);
            r.put("data", list);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/admin/qr-codes/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            String platform = reqString(body, "platform");
            String name = reqString(body, "name");
            String imageUrl = reqString(body, "imageUrl");
            Boolean enabled = body.containsKey("enabled") ? (Boolean) body.get("enabled") : true;
            Integer sortOrder = body.containsKey("sortOrder") ? reqInt(body, "sortOrder") : 0;

            if (!"wechat".equals(platform) && !"alipay".equals(platform)) {
                throw new RuntimeException("支付平台不合法");
            }

            PaymentQrCode qrCode = new PaymentQrCode();
            qrCode.setPlatform(platform);
            qrCode.setName(name);
            qrCode.setImageUrl(imageUrl);
            qrCode.setEnabled(enabled);
            qrCode.setSortOrder(sortOrder);
            qrCode = qrCodeRepository.save(qrCode);

            Map<String, Object> detail = new HashMap<>();
            detail.put("id", qrCode.getId());
            detail.put("platform", platform);
            detail.put("name", name);
            opLogService.log(request, admin, "QR_CODE_CREATE", "PAYMENT_QR_CODE", String.valueOf(qrCode.getId()), detail);

            r.put("success", true);
            r.put("data", qrCode);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/admin/qr-codes/{id}/update")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            PaymentQrCode qrCode = qrCodeRepository.findById(id).orElseThrow(() -> new RuntimeException("二维码不存在"));

            if (body.containsKey("name")) qrCode.setName(reqString(body, "name"));
            if (body.containsKey("imageUrl")) qrCode.setImageUrl(reqString(body, "imageUrl"));
            if (body.containsKey("enabled")) qrCode.setEnabled((Boolean) body.get("enabled"));
            if (body.containsKey("sortOrder")) qrCode.setSortOrder(reqInt(body, "sortOrder"));

            qrCode = qrCodeRepository.save(qrCode);

            Map<String, Object> detail = new HashMap<>();
            detail.put("id", qrCode.getId());
            opLogService.log(request, admin, "QR_CODE_UPDATE", "PAYMENT_QR_CODE", String.valueOf(qrCode.getId()), detail);

            r.put("success", true);
            r.put("data", qrCode);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/admin/qr-codes/{id}/delete")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            qrCodeRepository.deleteById(id);

            Map<String, Object> detail = new HashMap<>();
            detail.put("id", id);
            opLogService.log(request, admin, "QR_CODE_DELETE", "PAYMENT_QR_CODE", String.valueOf(id), detail);

            r.put("success", true);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @GetMapping("/payment/config")
    public ResponseEntity<Map<String, Object>> paymentConfig() {
        Map<String, Object> r = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        List<PaymentQrCode> wechatList = qrCodeRepository.findByPlatformAndEnabledTrueOrderBySortOrderAsc("wechat");
        List<PaymentQrCode> alipayList = qrCodeRepository.findByPlatformAndEnabledTrueOrderBySortOrderAsc("alipay");
        
        data.put("wechatQr", getRandomQr(wechatList));
        data.put("alipayQr", getRandomQr(alipayList));
        
        r.put("success", true);
        r.put("data", data);
        return ResponseEntity.ok(r);
    }

    private Map<String, Object> getRandomQr(List<PaymentQrCode> list) {
        if (list == null || list.isEmpty()) return null;
        Random random = new Random();
        int index = random.nextInt(list.size());
        PaymentQrCode qr = list.get(index);
        Map<String, Object> m = new HashMap<>();
        m.put("id", qr.getId());
        m.put("name", qr.getName());
        m.put("url", qr.getImageUrl());
        return m;
    }

    private String reqString(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        if (v == null) throw new RuntimeException(key + "不能为空");
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) throw new RuntimeException(key + "不能为空");
        return s;
    }

    private Integer reqInt(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        if (v == null) throw new RuntimeException(key + "不能为空");
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(String.valueOf(v).trim());
    }
}
