package cn.laobayou.siyubao.bean;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
@Getter@Setter
public class SiyubaoJsonRequest {

    private String shortcode="";//获取消息的短码

    private String short_code_password="";
}
