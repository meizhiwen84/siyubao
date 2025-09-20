package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.bean.DeepSeekRequestMessage;
import cn.laobayou.siyubao.bean.RoleType;
import cn.laobayou.siyubao.bean.UserQuestionContent;
import cn.laobayou.siyubao.bean.XianluEnum;
import cn.laobayou.siyubao.service.DeepSeekService;
import cn.laobayou.siyubao.service.SiyubaoConfig;
import cn.laobayou.siyubao.service.UserStant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 恩施聊天内空生成
 */
@Slf4j
@Controller
public class ZhijieFahaomaController {
    //恩施头像
    private static String xinaluPic="https://p26.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-i-0813c001_owEAQCKfa5HIKfA9IRtnlAFPgABD6FgAAG0ErC.jpeg?from=4010531038";
    //恩施名称
    private static String xinaluName="恩施百晓通";
    //===============================================

    private static String title="云客";//聊天工具名称



    private static String myPic=xinaluPic;//自己的头像
    private static String myName=xinaluName;//自己的名称

    private int 几大=2;
    private int 几小=1;
    private int 几天=3;
    private String 么的时候="下个月";
    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private SiyubaoConfig siyubaoConfig;
    
    @Autowired
    private UserStant userStant;

    private static String welcomeMsg="你好，欢迎来xianlu旅游！ 目前xianlu旅游限时特惠优惠多多，您这边大概几个人，什么时候出行呢？可以留个联系方式，给你发行程报价参考下！";

    public static String getWelcomeMsg(String xianlu){
        String xianluName = XianluEnum.getNameByCode(xianlu);
        String rs=welcomeMsg.replaceAll("xianlu",xianluName);
         return rs;
    }

    /**
     * 封装聊天内容的数据结构
     * 双方聊天是多对多的关系
     *
     * 一对一
     * 一对多
     * 多对一
     * 多对多
     *
     * 定义一个聊天发送方的标识：1：客人 2：自己
     * 定义一个每条发送消息之后，对方需要回复的消息条数：1-10，3表示对方回复3条消息
     *
     *
     * @param modelMap
     * @return
     */

    @RequestMapping("/fahaoma")
    public String gen(ModelMap modelMap,@RequestParam String xianlu,@RequestParam String haoma,@RequestParam String xianshiname,String platform) throws IOException {
        LocalTime now = LocalTime.now(java.time.ZoneId.of("Asia/Shanghai"));

        Map<String, String> xianluNameAndPic = userStant.getXianluNameAndPic(xianlu,platform);

        modelMap.addAttribute("title", xianlu+"直接甩号码云客通");
        modelMap.addAttribute("message", title);
        modelMap.addAttribute("myPic", xianluNameAndPic.get("xianluPic"));
        modelMap.addAttribute("myName",(xianshiname!=null&&xianshiname.equals("true"))?xianluNameAndPic.get("xianluName"):"");
        modelMap.addAttribute("userName", userStant.getRandomUserName());
        modelMap.addAttribute("userPic", userStant.getRandomUserPic());

        List<ChatMessage> chatMessageList=generateChatMessage(now,xianlu,haoma);
        modelMap.addAttribute("msgList", chatMessageList);


        return "siyubao_cq";
    }

    private List<ChatMessage> generateChatMessage(LocalTime now, String xianlu, String haoma) throws IOException {
        LocalTime localTimeBefore = now;

        List<ChatMessage> chatMessageList=new ArrayList();
        ChatMessage m1=new ChatMessage();
        m1.setMsg(getWelcomeMsg(xianlu));
        m1.setDateTimeStr(userStant.getTimeStr(localTimeBefore.getHour())+":"+userStant.getTimeStr(localTimeBefore.getMinute()));
        m1.setMsgType(2);
        chatMessageList.add(m1);

        /**
         * 中间两句话
         */
//        ChatMessage hmcmu1=new ChatMessage();
//        hmcmu1.setMsg("没有");
//        hmcmu1.setDateTimeStr(userStant.getTimeStr(now.getHour())+":"+userStant.getTimeStr(now.getMinute()));
//        hmcmu1.setMsgType(1);
//        chatMessageList.add(hmcmu1);
//
//
//        ChatMessage mu2=new ChatMessage();
//        mu2.setMsg("我们是当地正规旅信社，没有中间商赚差价，你的微[爱心]是多少，可以把我们公司资质先发给您，然后给你做一个详细的行程规划，让你少走弯路");
//        mu2.setDateTimeStr(userStant.getTimeStr(now.getHour())+":"+userStant.getTimeStr(now.getMinute()));
//        mu2.setMsgType(2);
//        chatMessageList.add(mu2);
        /**
         * 结束
         */


        //发送号码
        ChatMessage hmcm=new ChatMessage();
//        m1.setContentType(2);
//        m1.setMsg("./avatar/avatar_"+33+".jpg");
//        hmcm.setMsg(userStant.getRandomOkResponse()+"13915948326");
        hmcm.setMsg(haoma);
        hmcm.setDateTimeStr(userStant.getTimeStr(now.getHour())+":"+userStant.getTimeStr(now.getMinute()));
        hmcm.setMsgType(1);
        chatMessageList.add(hmcm);

        ChatMessage m2=new ChatMessage();
        m2.setMsg(siyubaoConfig.getFinalword());
        m2.setDateTimeStr(userStant.getTimeStr(now.getHour())+":"+userStant.getTimeStr(now.getMinute()));
        m2.setMsgType(2);

        chatMessageList.add(m2);
        return chatMessageList;
    }

}
