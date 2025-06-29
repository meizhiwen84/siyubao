package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.bean.UserStant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.util.DateUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

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
        LocalTime now = LocalTime.now();



        modelMap.addAttribute("message", title);
        modelMap.addAttribute("myPic", myPic);
        modelMap.addAttribute("myName", myName);
        modelMap.addAttribute("userName", UserStant.getRandomUserName());
        modelMap.addAttribute("userPic", UserStant.getRandomUserPic());
        modelMap.addAttribute("hellomsg", UserStant.HELLOMSG);

        List<ChatMessage> chatMessageList=generateChatMessage(now);
        modelMap.addAttribute("msgList", chatMessageList);
        return "siyubao_cq";
    }

    private List<ChatMessage> generateChatMessage(LocalTime now){
        /**
         * 1、随机生成用户问几个问题，2-4个问题，每个问题不能问重复的
         * 2、每个问题调用deepseek去获取答案，
         * 3、到回复最后一个问题发送过后，马上发送一条"要求用户留联系方式的问题"
         */
//        String dateTimeStr=DateUtils.format(new Date(), "HH:mm", Locale.CHINA);
        //最原始的第一句话的时间
        LocalTime localTimeBefore = now.minusMinutes(RandomUtils.nextInt(10, 30));

        List<ChatMessage> chatMessageList=new ArrayList();
        ChatMessage m1=new ChatMessage();
        m1.setMsg(UserStant.HELLOMSG);
        m1.setDateTimeStr(UserStant.getTimeStr(localTimeBefore.getHour())+":"+UserStant.getTimeStr(localTimeBefore.getMinute()));
        m1.setMsgType(2);
        chatMessageList.add(m1);

        LocalTime rt=generateUserMessage1(chatMessageList, localTimeBefore);
        generateUserMessage2(chatMessageList, rt,now);

        ChatMessage m2=new ChatMessage();
        m2.setMsg(UserStant.FINALMSG);
        m2.setDateTimeStr(UserStant.getTimeStr(now.getHour())+":"+UserStant.getTimeStr(now.getMinute()));
        m2.setMsgType(2);
        chatMessageList.add(m2);
        return chatMessageList;
    }

    /**
     * 生成用户和自己的第一句话的对话
     * @return
     */
    private LocalTime generateUserMessage1(List<ChatMessage> chatMessageList, LocalTime localTimeBefore){
        LocalTime tempTime = localTimeBefore.plusMinutes(RandomUtils.nextInt(1, 3));//加3分钟

        ChatMessage m1=new ChatMessage();
        m1.setMsg(UserStant.getRandomFamilySizeResponse());
        m1.setDateTimeStr(UserStant.getTimeStr(tempTime.getHour())+":"+UserStant.getTimeStr(tempTime.getMinute()));
        m1.setMsgType(1);
        chatMessageList.add(m1);

        tempTime = tempTime.plusMinutes(RandomUtils.nextInt(0, 1));//加1分钟
        ChatMessage m2=new ChatMessage();
        m2.setMsg(UserStant.getSendLiuziMsg());
        m2.setDateTimeStr(UserStant.getTimeStr(tempTime.getHour())+":"+UserStant.getTimeStr(tempTime.getMinute()));
        m2.setMsgType(2);
        chatMessageList.add(m2);

        return tempTime;
    }

    /**
     * 生成用户和自己的第二句话的对话
     * @return
     */
    private void generateUserMessage2(List<ChatMessage> chatMessageList, LocalTime localTimeBefore,LocalTime now){
        String dateTimeStr=DateUtils.format(new Date(), "HH:mm", Locale.CHINA);
        LocalTime tempTime = localTimeBefore.plusMinutes(RandomUtils.nextInt(4, 6));//加4分钟

        ChatMessage m1=new ChatMessage();
        m1.setMsg(UserStant.getRandomOkResponse());
        m1.setDateTimeStr(UserStant.getTimeStr(tempTime.getHour())+":"+UserStant.getTimeStr(tempTime.getMinute()));
        m1.setMsgType(1);
        chatMessageList.add(m1);

//        ChatMessage mm1=new ChatMessage();
//        mm1.setMsg("好的，您发了我加您");
//        mm1.setDateTimeStr(UserStant.getTimeStr(tempTime.getHour())+":"+UserStant.getTimeStr(tempTime.getMinute()));
//        mm1.setMsgType(2);
//        chatMessageList.add(mm1);

        ChatMessage m2=new ChatMessage();
        m2.setMsg("Hyd19850721");//====================填微信
        m2.setDateTimeStr(UserStant.getTimeStr(now.getHour())+":"+UserStant.getTimeStr(now.getMinute()));
        m2.setMsgType(1);
        chatMessageList.add(m2);

    }

}
