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

//    @Value("${xianlu.englishName}")
//    private String chatConetentFile;

    /**
     * 恩施抖音号名字
     */
    @Value("${tihuan.enshi.name}")
    private String enshiName;
    /**
     * 恩施抖音号头像
     */
    @Value("${tihuan.enshi.imgPic}")
    private String enshiPic;
    /**
     * 重庆抖音号名称
     */
    @Value("${tihuan.cq.name}")
    private String cqName;
    /**
     * 重庆抖音号头像
     */
    @Value("${tihuan.cq.imgPic}")
    private String cqPic;

    /**
     * 四川抖音号名称
     */
    @Value("${tihuan.sc.name}")
    private String scName;
    /**
     * 四川 抖音号头像
     */
    @Value("${tihuan.sc.imgPic}")
    private String scPic;

    /**
     * 四川抖音号名称2
     */
    @Value("${tihuan.sc.name2}")
    private String scName2;
    /**
     * 四川 抖音号头像1
     */
    @Value("${tihuan.sc.imgPic2}")
    private String scPic2;

    /**
     * 四川抖音号名称3
     */
    @Value("${tihuan.sc.name3}")
    private String scName3;
    /**
     * 四川 抖音号头像1
     */
    @Value("${tihuan.sc.imgPic3}")
    private String scPic3;

    /**
     * 四川抖音号名称4
     */
    @Value("${tihuan.sc.name4}")
    private String scName4;
    /**
     * 四川 抖音号头像1
     */
    @Value("${tihuan.sc.imgPic4}")
    private String scPic4;

    /**
     * 四川视频号名称4
     */
    @Value("${tihuan.sc.name5}")
    private String scName5;
    /**
     * 四川 视频号头像1
     */
    @Value("${tihuan.sc.imgPic5}")
    private String scPic5;
    /**
     * 四川 视频号的欢迎语
     */
    @Value("${tihuan.sc.welcomword}")
    private String scWelcomword;

    /**
     * 新疆抖音号名称
     */
    @Value("${tihuan.xj.name}")
    private String xjName;
    /**
     * 新疆 抖音号头像
     */
    @Value("${tihuan.xj.imgPic}")
    private String xjPic;

    /**
     * 新疆抖音号名称
     */
    @Value("${tihuan.bj.name}")
    private String bjName;
    /**
     * 新疆 抖音号头像
     */
    @Value("${tihuan.bj.imgPic}")
    private String bjPic;

    /**
     * 新疆抖音号名称
     */
    @Value("${tihuan.bj.name2}")
    private String bjName2;
    /**
     * 新疆 抖音号头像
     */
    @Value("${tihuan.bj.imgPic2}")
    private String bjPic2;

    /**
     * 内蒙 抖音号名称
     */
    @Value("${tihuan.lm.name}")
    private String lmName;
    /**
     * 内蒙 抖音号头像
     */
    @Value("${tihuan.lm.imgPic}")
    private String lmPic;

    /**
     * 内蒙 抖音号名称
     */
    @Value("${tihuan.gz.name}")
    private String gzName;
    /**
     * 内蒙 抖音号头像
     */
    @Value("${tihuan.gz.imgPic}")
    private String gzPic;

    /**
     * 云南 抖音号名称
     */
    @Value("${tihuan.yn.name}")
    private String ynName;
    /**
     * 云南 抖音号头像
     */
    @Value("${tihuan.yn.imgPic}")
    private String ynPic;

    /**
     * 云南 抖音号名称
     */
    @Value("${tihuan.yn.name2}")
    private String ynName2;
    /**
     * 云南 抖音号头像
     */
    @Value("${tihuan.yn.imgPic2}")
    private String ynPic2;
    /**
     * 四川 视频号的欢迎语
     */
    @Value("${tihuan.yn.welcomword}")
    private String ynWelcomword;

    /**
     * 云南 抖音号名称
     */
    @Value("${tihuan.cs.name}")
    private String csName;
    /**
     * 云南 抖音号头像
     */
    @Value("${tihuan.cs.imgPic}")
    private String csPic;
    /**
     * 四川 视频号的欢迎语
     */
    @Value("${tihuan.cs.welcomword}")
    private String csWelcomword;

    /**
     * 雪乡 抖音号名称
     */
    @Value("${tihuan.xx.name}")
    private String xxName;
    /**
     * 雪乡 抖音号头像
     */
    @Value("${tihuan.xx.imgPic}")
    private String xxPic;
    /**
     * 雪乡 视频号的欢迎语
     */
    @Value("${tihuan.xx.welcomword}")
    private String xxWelcomword;

}
