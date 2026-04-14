package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_qr_code",
        indexes = {
                @Index(name = "idx_payment_qr_platform", columnList = "platform"),
                @Index(name = "idx_payment_qr_enabled", columnList = "enabled")
        }
)
@Data
public class PaymentQrCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
}
