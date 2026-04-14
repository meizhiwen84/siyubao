package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_order",
        indexes = {
                @Index(name = "idx_payment_user_id", columnList = "user_id"),
                @Index(name = "idx_payment_status", columnList = "status"),
                @Index(name = "idx_payment_order_no", columnList = "order_no", unique = true)
        }
)
@Data
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "order_no", nullable = false, length = 64, unique = true)
    private String orderNo;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform;

    @Column(name = "transaction_id", nullable = false, length = 128)
    private String transactionId;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING";

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "process_time")
    private LocalDateTime processTime;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "qr_code_id")
    private Long qrCodeId;

    @Column(name = "qr_code_name", length = 128)
    private String qrCodeName;

    @Column(name = "qr_code_url", length = 512)
    private String qrCodeUrl;
}
