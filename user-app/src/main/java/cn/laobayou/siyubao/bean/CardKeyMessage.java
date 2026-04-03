package cn.laobayou.siyubao.bean;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "card_key_message")
public class CardKeyMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_key", nullable = false)
    private String cardKey;

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

    @Lob
    @Column(name = "chat_essage")
    private String chatMessage;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
