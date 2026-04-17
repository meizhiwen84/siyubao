package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "feedback",
        indexes = {
                @Index(name = "idx_feedback_user_id", columnList = "user_id"),
                @Index(name = "idx_feedback_create_time", columnList = "create_time")
        }
)
@Data
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "contact", length = 256)
    private String contact;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
}
