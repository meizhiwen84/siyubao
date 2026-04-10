package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.MembershipPlan;
import cn.laobayou.siyubao.bean.UserSubscription;
import cn.laobayou.siyubao.repository.MembershipPlanRepository;
import cn.laobayou.siyubao.repository.UserSubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MembershipService {
    private final MembershipPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    public MembershipService(MembershipPlanRepository planRepository, UserSubscriptionRepository subscriptionRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public MembershipPlan resolvePlan(Long userId) {
        Optional<UserSubscription> subOpt = subscriptionRepository.findActiveByUserId(userId);
        if (subOpt.isPresent()) {
            UserSubscription s = subOpt.get();
            LocalDateTime end = s.getEndTime();
            if (end == null || end.isAfter(LocalDateTime.now())) {
                MembershipPlan p = planRepository.findById(s.getPlanId()).orElse(null);
                if (p != null && Boolean.TRUE.equals(p.getEnabled())) return p;
            }
        }
        return planRepository.findByCode("FREE").orElseGet(() -> {
            MembershipPlan p = new MembershipPlan();
            p.setCode("FREE");
            p.setName("免费用户");
            p.setPriceCents(0);
            p.setDurationDays(0);
            p.setDeviceLimit(1);
            p.setDailyFreeLimit(3);
            p.setWatermark(true);
            p.setSortOrder(0);
            return p;
        });
    }
}

