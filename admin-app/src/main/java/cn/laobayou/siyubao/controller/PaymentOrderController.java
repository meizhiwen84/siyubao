package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.*;
import cn.laobayou.siyubao.repository.*;
import cn.laobayou.siyubao.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api")
public class PaymentOrderController {
    private final AuthContextService authContextService;
    private final AuthService authService;
    private final PaymentOrderRepository orderRepository;
    private final MembershipPlanRepository planRepository;
    private final AppUserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final AdminOpLogService opLogService;
    private final WeComWebhookService weComWebhookService;
    private final AppSettingService appSettingService;

    private final Map<Long, Long> userRateLimit = new ConcurrentHashMap<>();
    private static final long SUBMIT_COOLDOWN_MS = 60000;

    public PaymentOrderController(
            AuthContextService authContextService,
            AuthService authService,
            PaymentOrderRepository orderRepository,
            MembershipPlanRepository planRepository,
            AppUserRepository userRepository,
            UserSubscriptionRepository subscriptionRepository,
            AdminOpLogService opLogService,
            WeComWebhookService weComWebhookService,
            AppSettingService appSettingService
    ) {
        this.authContextService = authContextService;
        this.authService = authService;
        this.orderRepository = orderRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.opLogService = opLogService;
        this.weComWebhookService = weComWebhookService;
        this.appSettingService = appSettingService;
    }

    @PostMapping("/payment/submit")
    public ResponseEntity<Map<String, Object>> submitOrder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            DeviceSession session = authContextService.requireSession(request);
            Long userId = session.getUserId();
            log.info("提交支付订单请求 - 用户ID: {}", userId);

            Long lastSubmit = userRateLimit.get(userId);
            long now = System.currentTimeMillis();
            if (lastSubmit != null && now - lastSubmit < SUBMIT_COOLDOWN_MS) {
                log.warn("支付订单操作过于频繁 - 用户ID: {}", userId);
                r.put("success", false);
                r.put("message", "操作过于频繁，请稍后再试");
                return ResponseEntity.badRequest().body(r);
            }

            long pendingCount = orderRepository.countPendingByUserId(userId);
            if (pendingCount >= 3) {
                log.warn("用户待处理订单过多 - 用户ID: {}, 待处理数量: {}", userId, pendingCount);
                r.put("success", false);
                r.put("message", "您有太多待处理订单，请联系客服");
                return ResponseEntity.badRequest().body(r);
            }

            Long planId = reqLong(body, "planId");
            String platform = reqString(body, "platform");
            String transactionId = reqString(body, "transactionId");
            
            // 获取二维码信息
            Long qrCodeId = body.containsKey("qrCodeId") ? reqLong(body, "qrCodeId") : null;
            String qrCodeName = body.containsKey("qrCodeName") ? (String) body.get("qrCodeName") : null;
            String qrCodeUrl = body.containsKey("qrCodeUrl") ? (String) body.get("qrCodeUrl") : null;

            if (!"wechat".equals(platform) && !"alipay".equals(platform)) {
                log.warn("支付平台不合法 - 用户ID: {}, 平台: {}", userId, platform);
                throw new RuntimeException("支付平台不合法");
            }

            MembershipPlan plan = planRepository.findById(planId).orElseThrow(() -> new RuntimeException("套餐不存在"));
            if (!Boolean.TRUE.equals(plan.getEnabled())) {
                log.warn("套餐未启用 - 用户ID: {}, 套餐ID: {}", userId, planId);
                throw new RuntimeException("套餐未启用");
            }

            Optional<PaymentOrder> existing = orderRepository.findByOrderNo(transactionId);
            if (existing.isPresent()) {
                log.warn("交易号已提交过 - 用户ID: {}, 交易号: {}", userId, transactionId);
                throw new RuntimeException("该交易号已提交过");
            }

            String orderNo = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("创建支付订单 - 用户ID: {}, 套餐: {}, 订单号: {}, 交易号: {}", 
                    userId, plan.getName(), orderNo, transactionId);

            PaymentOrder order = new PaymentOrder();
            order.setUserId(userId);
            order.setPlanId(planId);
            order.setOrderNo(orderNo);
            order.setPlatform(platform);
            order.setTransactionId(transactionId);
            order.setAmountCents(plan.getPriceCents() == null ? 0 : plan.getPriceCents());
            order.setStatus("PENDING");
            order.setCreateTime(LocalDateTime.now());
            order.setQrCodeId(qrCodeId);
            order.setQrCodeName(qrCodeName);
            order.setQrCodeUrl(qrCodeUrl);
            orderRepository.save(order);

            userRateLimit.put(userId, now);

            AppUser user = userRepository.findById(userId).orElse(null);
            String username = user != null ? user.getUsername() : "未知用户";
            String amount = plan.getPriceCents() != null && plan.getPriceCents() > 0 
                    ? "￥" + (plan.getPriceCents() / 100.0) 
                    : "免费";
            String adminBaseUrl = appSettingService.getString("admin_base_url", "http://localhost:8080/admin");
            String approveUrl = adminBaseUrl + "/#/orders";

            log.info("发送企业微信通知 - 订单号: {}, 用户: {}, 套餐: {}, 金额: {}", orderNo, username, plan.getName(), amount);
            weComWebhookService.sendPaymentNotification(
                    username,
                    plan.getName(),
                    amount,
                    "wechat".equals(platform) ? "微信支付" : "支付宝",
                    transactionId,
                    orderNo,
                    approveUrl,
                    qrCodeName,
                    qrCodeUrl
            );

            log.info("支付订单提交成功 - 用户ID: {}, 订单号: {}", userId, orderNo);
            r.put("success", true);
            r.put("data", orderInfo(order, plan, user));
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            log.error("支付订单提交失败", e);
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @GetMapping("/payment/my-orders")
    public ResponseEntity<Map<String, Object>> myOrders(HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            DeviceSession session = authContextService.requireSession(request);
            List<PaymentOrder> orders = orderRepository.findByUserIdOrderByCreateTimeDesc(session.getUserId());
            List<Map<String, Object>> data = new ArrayList<>();
            for (PaymentOrder order : orders) {
                MembershipPlan plan = planRepository.findById(order.getPlanId()).orElse(null);
                data.add(orderInfo(order, plan, null));
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

    @GetMapping("/admin/orders/list")
    public ResponseEntity<Map<String, Object>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        Map<String, Object> r = new HashMap<>();
        try {
            requireAdmin(request);
            int p = Math.max(page, 0);
            int s = Math.min(Math.max(size, 1), 100);
            String kw = keyword == null ? null : keyword.trim();
            if (kw != null && kw.isEmpty()) kw = null;
            
            // 当 status 为空字符串时，视为 null（查询全部）
            String searchStatus = (status == null || status.trim().isEmpty()) ? null : status;

            Page<PaymentOrder> res;
            if (kw != null) {
                res = orderRepository.search(searchStatus, kw, PageRequest.of(p, s));
            } else {
                res = orderRepository.findByStatusOrderByCreateTimeDesc(searchStatus, PageRequest.of(p, s));
            }

            List<PaymentOrder> orders = res.getContent();
            List<Long> userIds = orders.stream().map(PaymentOrder::getUserId).filter(Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
            List<Long> planIds = orders.stream().map(PaymentOrder::getPlanId).filter(Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());

            Map<Long, AppUser> userMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                for (AppUser u : userRepository.findAllById(userIds)) {
                    if (u != null) userMap.put(u.getId(), u);
                }
            }

            Map<Long, MembershipPlan> planMap = new HashMap<>();
            if (!planIds.isEmpty()) {
                for (MembershipPlan msp : planRepository.findAllById(planIds)) {
                    if (msp != null) planMap.put(msp.getId(), msp);
                }
            }

            List<Map<String, Object>> data = new ArrayList<>();
            for (PaymentOrder order : orders) {
                data.add(orderInfo(order, planMap.get(order.getPlanId()), userMap.get(order.getUserId())));
            }

            r.put("success", true);
            r.put("data", data);
            r.put("total", res.getTotalElements());
            r.put("pages", res.getTotalPages());
            r.put("page", p);
            r.put("size", s);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/admin/orders/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveOrder(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            PaymentOrder order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("订单不存在"));
            if (!"PENDING".equals(order.getStatus())) {
                throw new RuntimeException("订单已处理");
            }

            MembershipPlan plan = planRepository.findById(order.getPlanId()).orElseThrow(() -> new RuntimeException("套餐不存在"));

            Optional<UserSubscription> activeOpt = subscriptionRepository.findActiveByUserId(order.getUserId());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now;
            LocalDateTime endTime = null;
            
            int newDays = plan.getDurationDays() == null ? 0 : Math.max(0, plan.getDurationDays());
            
            if (activeOpt.isPresent()) {
                UserSubscription old = activeOpt.get();
                // 如果旧订阅还没过期，保留剩余时长
                if (old.getEndTime() != null && old.getEndTime().isAfter(now)) {
                    startTime = old.getEndTime();
                    if (newDays > 0) {
                        endTime = old.getEndTime().plusDays(newDays);
                    }
                } else {
                    // 旧订阅已过期，从现在开始
                    if (newDays > 0) {
                        endTime = now.plusDays(newDays);
                    }
                }
                old.setStatus("EXPIRED");
                subscriptionRepository.save(old);
            } else {
                // 没有活跃订阅，从现在开始
                if (newDays > 0) {
                    endTime = now.plusDays(newDays);
                }
            }

            UserSubscription sub = new UserSubscription();
            sub.setUserId(order.getUserId());
            sub.setPlanId(plan.getId());
            sub.setStartTime(startTime);
            sub.setEndTime(endTime);
            sub.setStatus("ACTIVE");
            sub.setCreateTime(now);
            subscriptionRepository.save(sub);

            order.setStatus("APPROVED");
            order.setProcessTime(now);
            order.setProcessedBy(admin.getId());
            orderRepository.save(order);

            Map<String, Object> detail = new HashMap<>();
            detail.put("orderId", order.getId());
            detail.put("userId", order.getUserId());
            detail.put("planId", plan.getId());
            opLogService.log(request, admin, "ORDER_APPROVE", "PAYMENT_ORDER", String.valueOf(order.getId()), detail);

            r.put("success", true);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/admin/orders/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectOrder(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> r = new HashMap<>();
        try {
            AppUser admin = requireAdmin(request);
            PaymentOrder order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("订单不存在"));
            if (!"PENDING".equals(order.getStatus())) {
                throw new RuntimeException("订单已处理");
            }

            String remark = body != null && body.get("remark") != null ? String.valueOf(body.get("remark")) : null;

            order.setStatus("REJECTED");
            order.setRemark(remark);
            order.setProcessTime(LocalDateTime.now());
            order.setProcessedBy(admin.getId());
            orderRepository.save(order);

            Map<String, Object> detail = new HashMap<>();
            detail.put("orderId", order.getId());
            detail.put("userId", order.getUserId());
            opLogService.log(request, admin, "ORDER_REJECT", "PAYMENT_ORDER", String.valueOf(order.getId()), detail);

            r.put("success", true);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }

    private Long reqLong(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        if (v == null) throw new RuntimeException(key + "不能为空");
        if (v instanceof Number) return ((Number) v).longValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) throw new RuntimeException(key + "不能为空");
        return Long.parseLong(s);
    }

    private String reqString(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        if (v == null) throw new RuntimeException(key + "不能为空");
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) throw new RuntimeException(key + "不能为空");
        return s;
    }

    private AppUser requireAdmin(HttpServletRequest request) {
        DeviceSession s = authContextService.requireSession(request);
        AppUser u = authService.findUser(s.getUserId()).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!Boolean.TRUE.equals(u.getEnabled())) throw new RuntimeException("账号已被禁用");
        if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("无权限");
        return u;
    }

    private Map<String, Object> orderInfo(PaymentOrder o, MembershipPlan p, AppUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", o.getId());
        m.put("userId", o.getUserId());
        m.put("planId", o.getPlanId());
        m.put("orderNo", o.getOrderNo());
        m.put("platform", o.getPlatform());
        m.put("transactionId", o.getTransactionId());
        m.put("amountCents", o.getAmountCents());
        m.put("status", o.getStatus());
        m.put("remark", o.getRemark());
        m.put("processTime", o.getProcessTime());
        m.put("processedBy", o.getProcessedBy());
        m.put("createTime", o.getCreateTime());
        m.put("qrCodeId", o.getQrCodeId());
        m.put("qrCodeName", o.getQrCodeName());
        m.put("qrCodeUrl", o.getQrCodeUrl());
        if (p != null) {
            Map<String, Object> plan = new HashMap<>();
            plan.put("id", p.getId());
            plan.put("name", p.getName());
            plan.put("priceCents", p.getPriceCents());
            plan.put("durationDays", p.getDurationDays());
            m.put("plan", plan);
        }
        if (u != null) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", u.getId());
            user.put("username", u.getUsername());
            m.put("user", user);
        }
        return m;
    }
}
