package cn.laobayou.siyubao.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SiyubaoJsonResponse {

    private Integer msgtype=3;//2表示进入私信会话页，不是聊天的消息 3:表示正常用户的聊天和回复的消息

    private Integer isme=1;//0表示用户消息 1 表示自己的消息

    private Integer is_aichat=0;//是否ai回复的  1：是ai自动回复的 0:不是ai自动回复的

    private String msg;//消息的内容

    private String send_time;//消息的时间

    private String avatar;// 作者名字

    private String author;//作者图像
}
