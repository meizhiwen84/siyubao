package cn.laobayou.siyubao.bean;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chat_message_history")
public class ChatMessageHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "line")
    private String line;

    @Column(name = "platform")
    private String platform;

    @Column(name = "phone")
    private String phone;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_pic")
    private String userPic;

    @Column(name = "chat_message", columnDefinition = "TEXT")
    private String chatMessage;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
