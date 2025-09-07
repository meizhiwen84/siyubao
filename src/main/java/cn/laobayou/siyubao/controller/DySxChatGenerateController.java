package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.bean.XianluEnum;
import cn.laobayou.siyubao.service.DeepSeekService;
import cn.laobayou.siyubao.service.SiyubaoConfig;
import cn.laobayou.siyubao.service.UserStant;
import com.alibaba.fastjson.JSON;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @RequestMapping("/reGenerateDyChat")
    public String reGen(ModelMap modelMap,@RequestParam String xianshiname, Boolean xhs,Boolean xhsphone) throws IOException {
        LocalTime now = LocalTime.now();
//        String xianlu="";//

        //读取文件里rechatcontent.txt 用于还原聊天内容的日志记录
        List<String> cc = Files.readAllLines(Paths.get("/Users/meizhiwen/dev/siyubao/src/main/resources/static/rechatcontent/rechatcontent.txt"));
        if(cc.size()>1){
            throw new RuntimeException("文件内容出错");
        }

        //解析这个字符串=======start
//        String input = "线路:cq||用户名称:用户8628897890167||用户头像:./avatar/avatar_11397.jpg||聊天内容:[{"contentType":1,"dateTimeStr":"22:42","msg":"五大一小","msgType":1,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"您好呀，您这边计划什么时候出行呢","msgType":2,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"29号到","msgType":1,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"多少钱","msgType":1,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"好的。您留个微，我发您吧，您看下行程安排和报价","msgType":2,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"13730002886","msgType":1,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"好的，马上安排专属管家添加您一会儿您记得通过一下我哦","msgType":2,"userName":"用户8628897890167"}]";

        // 定义四个变量存储提取的信息
        String xianlu = "";
        String userName = "";
        String userAvatar = "";
        String chatContent = "";

        // 使用正则表达式提取各部分信息
        Pattern pattern = Pattern.compile("线路:(.*?)\\|\\|用户名称:(.*?)\\|\\|用户头像:(.*?)\\|\\|聊天内容:(.*)");
        Matcher matcher = pattern.matcher(cc.get(0));

        if (matcher.find()) {
            xianlu = matcher.group(1);
            userName = matcher.group(2);
            userAvatar = matcher.group(3);
            chatContent = matcher.group(4);
        }

        // 输出提取的信息
        log.info("线路: " + xianlu);
        log.info("用户名称: " + userName);
        log.info("用户头像: " + userAvatar);
        log.info("聊天内容: " + chatContent);
        //解析结束===============end

        Map<String, String> xianluNameAndPic = userStant.getXianluNameAndPic(xianlu);

        modelMap.addAttribute("title", xianlu+"-dy截图生成聊天");
        modelMap.addAttribute("message", title);
        modelMap.addAttribute("myPic", xianluNameAndPic.get("xianluPic"));
        modelMap.addAttribute("myName",(xianshiname!=null&&xianshiname.equals("true"))?xianluNameAndPic.get("xianluName"):"");
//        String userPic=userStant.getRandomUserPic();
        modelMap.addAttribute("userPic", userAvatar);

        List<ChatMessage> chatMessageList=JSON.parseArray(chatContent,ChatMessage.class);

        //generateChatMessage(now,xianlu);
//        log.info("线路:"+xianlu+ "||用户名称:"+userName + "||用户头像:"+ userAvatar + "||聊天内容:"+ JSON.toJSONString(chatMessageList));
        if(JSON.toJSONString(chatMessageList).equals(chatContent)){
            log.info("==================================复现的聊天记录是一样的===========================================");
        }

        List<ChatMessage> xhsChatMessageList=new ArrayList();
        xhsChatMessageList.addAll(chatMessageList);
        //再添加最后一个需要反馈的话术

        ChatMessage m1=new ChatMessage();
        m1.setMsgType(2);

        m1.setMsg("亲;这边管家已经加您了哈;您通过-下哦");

        String fankuiDateTimeStr=userStant.getTimeStr(now.getHour())+":"+userStant.getTimeStr(now.getMinute());

        m1.setDateTimeStr(fankuiDateTimeStr);

        chatMessageList.add(m1);

        log.info("添加反馈后的聊天内容: " + JSON.toJSONString(chatMessageList));

        modelMap.addAttribute("userName", chatMessageList.get(0).getUserName());
        modelMap.addAttribute("msgList", chatMessageList);
        modelMap.addAttribute("xhsMsgList", xhsChatMessageList);
        modelMap.addAttribute("firstDateTimeStr", chatMessageList.get(0).getDateTimeStr());
        modelMap.addAttribute("fankuiDateTimeStr", fankuiDateTimeStr);

        if(xhs!=null&&xhs){
            if(xhsphone!=null&&xhsphone){
                return "chat-interface-v4-fk.html";
            }
        }
        return (xhs!=null&&xhs)?"siyubao_xhs":"siyubao_cq";
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
    public String gen(ModelMap modelMap,@RequestParam String xianlu, String xianshiname, Boolean xhs,Boolean xhsphone) throws IOException {
        LocalTime now = LocalTime.now();

        Map<String, String> xianluNameAndPic = userStant.getXianluNameAndPic(xianlu);

        String titlepre=(xhs!=null&&xhs)?"小红书-":"";
        String messagepre=(xhs!=null&&xhs)?"小红书-":"";

        modelMap.addAttribute("title", titlepre+xianlu+"-dy截图生成聊天");
        modelMap.addAttribute("message", messagepre+title);
        modelMap.addAttribute("myPic", xianluNameAndPic.get("xianluPic"));
        modelMap.addAttribute("myName",(xianshiname!=null&&xianshiname.equals("true"))?xianluNameAndPic.get("xianluName"):"");
        String userPic=userStant.getRandomUserPic();
        modelMap.addAttribute("userPic", userPic);

        List<ChatMessage> chatMessageList=generateChatMessage(now,xianlu,xhs,xhsphone);
        log.info("线路:"+xianlu+ "||用户名称:"+chatMessageList.get(0).getUserName() + "||用户头像:"+ userPic + "||聊天内容:"+ JSON.toJSONString(chatMessageList));
        log.info("==================================#########################===========================================");
        modelMap.addAttribute("userName", chatMessageList.get(0).getUserName());
        modelMap.addAttribute("msgList", chatMessageList);
        modelMap.addAttribute("firstDateTimeStr", chatMessageList.get(0).getDateTimeStr());

        if(xhs!=null&&xhs){
            if(xhsphone!=null&&xhsphone){
                return "chat-interface-v4.html";
            }
        }
        return (xhs!=null&&xhs)?"siyubao_xhs":"siyubao_cq";
    }

    private List<ChatMessage> generateChatMessage(LocalTime now, String xianlu,Boolean xhs,Boolean xhsphone) throws IOException {
        /**
         * 首先从文件中读取复制出来的聊天内容
         *
         */
        String path="/Users/meizhiwen/dev/siyubao/src/main/resources/static/chatcontent/"+xianlu+"_dychat.txt";
        if(xhs!=null&&xhs&&xhsphone!=null&&xhsphone){
            path="/Users/meizhiwen/dev/siyubao/src/main/resources/static/xhschatcontent/"+xianlu+"_xhschat.txt";
        }
        List<String> cc = Files.readAllLines(Paths.get(path));
        String first = cc.get(0);;//第一句话不是11结尾的，就报错，表示没有用户的名称
        if(!first.endsWith("11")){
            throw new RuntimeException("缺少用户名称");
        }
        first=first.substring(0,first.length()-2);
        //总共有几句话对话
//        int chatCnt = cc.size()-1;//5

        LocalTime localTimeBefore = now;
        localTimeBefore = now.minusMinutes(RandomUtils.nextInt(1,2));//最原始第一句话的时间,在当前时间往前面蝛一或者两分钟

        List<ChatMessage> chatMessageList=new ArrayList();

        for (int i = 1; i < cc.size(); i++) {
            //循环每一句话生成聊天内容
            String ct=cc.get(i);

            //判断是不是以"end"结尾
            if(ct.endsWith("end")){
                break;
            }

            ChatMessage m1=new ChatMessage();
            m1.setMsgType(1);
            m1.setUserName(first);

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
                //localTimeBefore = localTimeBefore.plusMinutes(RandomUtils.nextInt(1,2));//减3分钟
            }
        }

        return chatMessageList;
    }

}
