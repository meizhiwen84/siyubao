package cn.laobayou.siyubao.service;

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

    @Value("${xianlu.questionFile}")
    private String questionFile;

    @Value("${xianlu.englishName}")
    private String englishName;
}
