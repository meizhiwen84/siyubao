package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "membership_plan",
        indexes = {
                @Index(name = "idx_plan_code", columnList = "code", unique = true)
        }
)
@Data
public class MembershipPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "device_limit", nullable = false)
    private Integer deviceLimit;

    @Column(name = "daily_free_limit", nullable = false)
    private Integer dailyFreeLimit;

    @Column(name = "watermark", nullable = false)
    private Boolean watermark;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
}

