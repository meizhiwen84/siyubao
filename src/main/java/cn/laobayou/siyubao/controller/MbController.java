package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.bean.UserStant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.util.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 恩施聊天内空生成
 */
@Slf4j
@Controller
public class MbController {
    //恩施头像
    private static String xinaluPic="https://p26.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-i-0813c001_owEAQCKfa5HIKfA9IRtnlAFPgABD6FgAAG0ErC.jpeg?from=4010531038";
    //恩施名称
    private static String xinaluName="恩施百晓通";
    //===============================================

    private static String title="云客宝";//聊天工具名称



    private static String myPic=xinaluPic;//自己的头像
    private static String myName=xinaluName;//自己的名称

    private int 几大=2;
    private int 几小=1;
    private int 几天=3;
    private String 么的时候="下个月";


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

    @RequestMapping("/list")
    public String gen(ModelMap modelMap){
        modelMap.addAttribute("message", title);
        modelMap.addAttribute("myPic", myPic);
        modelMap.addAttribute("myName", myName);
        modelMap.addAttribute("userName", UserStant.getRandomUserName());
        modelMap.addAttribute("userPic", UserStant.getRandomUserPic());
        modelMap.addAttribute("hellomsg", UserStant.HELLOMSG);

        List<ChatMessage> chatMessageList=generateChatMessage();
        modelMap.addAttribute("msgList", chatMessageList);
        return "siyubao_cq";
    }

    private List<ChatMessage> generateChatMessage(){
        String dateTimeStr=DateUtils.format(new Date(), "HH:mm", Locale.CHINA);
        List<ChatMessage> chatMessageList=new ArrayList();
        ChatMessage m1=new ChatMessage();
        m1.setMsg(UserStant.HELLOMSG);
        m1.setDateTimeStr(dateTimeStr);
        m1.setMsgType(2);
        chatMessageList.add(m1);

        generateUserMessage1(chatMessageList);
        generateUserMessage2(chatMessageList);

        ChatMessage m2=new ChatMessage();
        m2.setMsg(UserStant.FINALMSG);
        m2.setDateTimeStr(dateTimeStr);
        m2.setMsgType(2);
        chatMessageList.add(m2);
        return chatMessageList;
    }

    /**
     * 生成用户和自己的第一句话的对话
     * @return
     */
    private void generateUserMessage1(List<ChatMessage> chatMessageList){
        String dateTimeStr=DateUtils.format(new Date(), "HH:mm", Locale.CHINA);
        ChatMessage m1=new ChatMessage();
        m1.setMsg("大概"+几大+"大人"+几小+"个小孩");
        m1.setDateTimeStr(dateTimeStr);
        m1.setMsgType(1);
        chatMessageList.add(m1);

        ChatMessage m2=new ChatMessage();
        m2.setMsg("我安排个管家给你发送行程资料（景点+行程报价+优惠），你留个薇♥方便吗？你先参考下！因为平台有敏感词限制，发不过来具体价格，感谢您的理解");
        m2.setDateTimeStr(dateTimeStr);
        m2.setMsgType(2);
        chatMessageList.add(m2);
    }

    /**
     * 生成用户和自己的第二句话的对话
     * @return
     */
    private void generateUserMessage2(List<ChatMessage> chatMessageList){
        String dateTimeStr=DateUtils.format(new Date(), "HH:mm", Locale.CHINA);
        ChatMessage m1=new ChatMessage();
        m1.setMsg("您加我");
        m1.setDateTimeStr(dateTimeStr);
        m1.setMsgType(1);
        chatMessageList.add(m1);

        ChatMessage m2=new ChatMessage();
        m2.setMsg("");//====================填微信
        m2.setDateTimeStr(dateTimeStr);
        m2.setMsgType(1);
        chatMessageList.add(m2);

    }

}
