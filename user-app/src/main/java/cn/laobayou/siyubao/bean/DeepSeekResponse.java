package cn.laobayou.siyubao.bean;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekResponse {

    private String id;

    private String questionId;

    //模型名称
    private String model;

    //创建时间
    @JSONField(name = "created_at")
    private String createdAt;

    //响应内容
    private DeepSeekRequestMessage message;

    //
    private boolean done;

    //
    @JSONField(name = "done_reason")
    private String doneReason;

    //
    private Integer[] context;

    //
    @JSONField(name = "total_duration")
    private Long totalDuration;

    //
    @JSONField(name = "load_duration")
    private Long loadDuration;

    //
    @JSONField(name = "prompt_eval_count")
    private Long promptEvalCount;

    //
    @JSONField(name = "prompt_eval_duration")
    private Long promptEvalDuration;

    //
    @JSONField(name = "eval_count")
    private Long evalCount;

    //
    @JSONField(name = "eval_duration")
    private Long evalDuration;
}