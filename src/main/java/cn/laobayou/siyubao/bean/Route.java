package cn.laobayou.siyubao.bean;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 线路实体类
 * 用于管理各平台的线路信息和头像
 */
@Entity
@Table(name = "routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 线路名称
     */
    @Column(name = "route_name", nullable = false, unique = true, length = 100)
    private String routeName;
    
    /**
     * 线路值
     */
    @Column(name = "route_value", nullable = false, length = 500)
    private String routeValue;
    
    /**
     * 抖音头像路径
     */
    @Column(name = "douyin_avatar", length = 500)
    private String douyinAvatar;
    
    /**
     * 视频号头像路径
     */
    @Column(name = "shipin_avatar", length = 500)
    private String shipinAvatar;
    
    /**
     * 小红书头像路径
     */
    @Column(name = "xiaohongshu_avatar", length = 500)
    private String xiaohongshuAvatar;
    
    /**
     * 欢迎语
     */
    @Column(name = "welcome_message", length = 1000)
    private String welcomeMessage = "欢迎来到我们的平台！";
    
    /**
     * 状态：true-启用，false-停用
     */
    @Column(name = "status", nullable = false)
    private Boolean status = true;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    public Route(String routeName, String routeValue) {
        this.routeName = routeName;
        this.routeValue = routeValue;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}