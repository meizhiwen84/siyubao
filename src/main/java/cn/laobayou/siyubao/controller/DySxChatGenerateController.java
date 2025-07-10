package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.bean.XianluEnum;
import cn.laobayou.siyubao.service.DeepSeekService;
import cn.laobayou.siyubao.service.SiyubaoConfig;
import cn.laobayou.siyubao.service.UserStant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 抖音私信聊天内空生成
 *
 *   dychat.txt 聊天内容文件里的内容，尾部添加"22" ，标示这一行是我自己发出的信息
 */
@Slf4j
@Controller
public class DySxChatGenerateController {
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

    @RequestMapping("/generateDyChat")
    public String gen(ModelMap modelMap,@RequestParam String xianlu,@RequestParam String xianshiname) throws IOException {
        LocalTime now = LocalTime.now();

        Map<String, String> xianluNameAndPic = userStant.getXianluNameAndPic(xianlu);

        modelMap.addAttribute("title", xianlu+"-dy截图生成聊天");
        modelMap.addAttribute("message", title);
        modelMap.addAttribute("myPic", xianluNameAndPic.get("xianluPic"));
        modelMap.addAttribute("myName",(xianshiname!=null&&xianshiname.equals("true"))?xianluNameAndPic.get("xianluName"):"");
        modelMap.addAttribute("userName", userStant.getRandomUserName());
        modelMap.addAttribute("userPic", userStant.getRandomUserPic());

        List<ChatMessage> chatMessageList=generateChatMessage(now,xianlu);
        modelMap.addAttribute("msgList", chatMessageList);


        return "siyubao_cq";
    }

    private List<ChatMessage> generateChatMessage(LocalTime now, String xianlu) throws IOException {
        /**
         * 首先从文件中读取复制出来的聊天内容
         *
         */
        List<String> cc = Files.readAllLines(Paths.get("/Users/meizhiwen/dev/siyubao/src/main/resources/static/chatcontent/dychat.txt"));

        //总共有几句话对话
        int chatCnt = cc.size();//5

        LocalTime localTimeBefore = now;
        localTimeBefore = now.minusMinutes(RandomUtils.nextInt(3*chatCnt, 4*chatCnt));//最原始第一句话的时间

        List<ChatMessage> chatMessageList=new ArrayList();

        for (int i = 0; i < cc.size(); i++) {
            //循环每一句话生成聊天内容
            String ct=cc.get(i);

            ChatMessage m1=new ChatMessage();
            m1.setMsgType(1);

            //判断是否包含以22结尾
            if(ct.endsWith("22")){
                //表示是我的消息
                m1.setMsgType(2);
                ct=ct.substring(0,ct.length()-2);
            }
            m1.setMsg(ct);

            m1.setDateTimeStr(userStant.getTimeStr(localTimeBefore.getHour())+":"+userStant.getTimeStr(localTimeBefore.getMinute()));

            chatMessageList.add(m1);

            //判断是不是倒数第二条消息，如果是，表示下一条消息就是最后一条消息了。就需要将时间改成当前的时间
            if(i==(cc.size()-2) || i==(cc.size()-3)){
                //最后一条消息
                localTimeBefore=now;
            }else if(i<(cc.size()-3)){
                //前面的消息，就将消息的生成时间往前加几分钟
                localTimeBefore = localTimeBefore.plusMinutes(RandomUtils.nextInt(1, 3));//减3分钟
            }
        }

        return chatMessageList;
    }

}
