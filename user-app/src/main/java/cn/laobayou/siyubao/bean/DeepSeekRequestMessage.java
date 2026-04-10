package cn.laobayou.siyubao.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekRequestMessage {

    private String role;//user assistant

    private String content;
}
