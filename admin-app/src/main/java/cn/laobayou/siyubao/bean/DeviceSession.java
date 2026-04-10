package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "device_session",
        indexes = {
                @Index(name = "idx_device_session_token", columnList = "token", unique = true),
                @Index(name = "idx_device_session_user", columnList = "user_id"),
                @Index(name = "idx_device_session_last", columnList = "last_seen_time")
        }
)
@Data
public class DeviceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "last_seen_time", nullable = false)
    private LocalDateTime lastSeenTime = LocalDateTime.now();

    @Column(name = "revoked", nullable = false)
    private Boolean revoked = Boolean.FALSE;
}

