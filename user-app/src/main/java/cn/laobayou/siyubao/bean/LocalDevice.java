package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "local_device")
@Data
public class LocalDevice {
    @Id
    private Long id;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "update_time", nullable = false, length = 32)
    private String updateTime;
}

