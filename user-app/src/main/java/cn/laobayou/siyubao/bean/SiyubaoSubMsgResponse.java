package cn.laobayou.siyubao.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SiyubaoSubMsgResponse {

    private ArrayList<SiyubaoJsonResponse> msg_list;
}
