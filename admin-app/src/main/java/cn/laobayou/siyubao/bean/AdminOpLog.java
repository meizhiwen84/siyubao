package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "admin_op_log",
        indexes = {
                @Index(name = "idx_admin_op_log_time", columnList = "create_time"),
                @Index(name = "idx_admin_op_log_admin", columnList = "admin_user_id,create_time")
        }
)
@Data
public class AdminOpLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(name = "admin_username", nullable = false, length = 64)
    private String adminUsername;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
}
