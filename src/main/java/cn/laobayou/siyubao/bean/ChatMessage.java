package cn.laobayou.siyubao.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class ChatMessage {

    private String msg;

    private String dateTimeStr;

    private int msgType;//1表示用户消息 2表示我的消息

    private int contentType=1;//1:广本2:图片
}
