package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_setting")
@Data
public class AppSetting {
    @Id
    @Column(name = "k", length = 128)
    private String key;

    @Column(name = "v", columnDefinition = "TEXT")
    private String value;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime = LocalDateTime.now();
}
