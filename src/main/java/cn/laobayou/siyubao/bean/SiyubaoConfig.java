package cn.laobayou.siyubao.bean;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class SiyubaoConfig {
    @Value("${xianlu.name}")
    private String name;

    @Value("${xianlu.jitianjiwan}")
    private String jitianjiwan;

    @Value("${xianlu.welcomword}")
    private String welcomeword;

    @Value("${xianlu.finalword}")
    private String finalword;
}
