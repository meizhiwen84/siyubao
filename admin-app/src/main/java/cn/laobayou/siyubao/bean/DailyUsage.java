package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "daily_usage",
        indexes = {
                @Index(name = "idx_daily_usage_user_date", columnList = "user_id,day", unique = true)
        }
)
@Data
public class DailyUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "count", nullable = false)
    private Integer count;
}

