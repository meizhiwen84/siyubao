package cn.laobayou.siyubao.bean;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "local_user_session")
@Data
public class LocalUserSession {
    @Id
    private Long id;

    @Column(name = "token", nullable = false, length = 80)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "user_no", length = 32)
    private String userNo;

    @Column(name = "update_time", nullable = false, length = 32)
    private String updateTime;
}

