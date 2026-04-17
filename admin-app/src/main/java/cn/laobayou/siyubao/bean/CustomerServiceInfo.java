package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_service_info")
@Data
public class CustomerServiceInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wechat_qr_code_url", length = 512)
    private String wechatQrCodeUrl;

    @Column(name = "official_account_qr_code_url", length = 512)
    private String officialAccountQrCodeUrl;

    @Column(name = "email", length = 256)
    private String email;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime = LocalDateTime.now();
}
