package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.bean.DeepSeekRequestMessage;
import cn.laobayou.siyubao.bean.RoleType;
import cn.laobayou.siyubao.bean.SiyubaoJsonResponse;
import cn.laobayou.siyubao.bean.UserQuestionContent;
import cn.laobayou.siyubao.service.DeepSeekService;
import cn.laobayou.siyubao.service.SiyubaoConfig;
import cn.laobayou.siyubao.service.UserStant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * 机器人替换聊天截图
 */
@Slf4j
@Controller
public class JiQiRenYuqueTIhuanController {
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

    @RequestMapping("/yqtihuan")
    public String gen(ModelMap modelMap,@RequestParam String msgurl,@RequestParam String xianlu,String platform) throws IOException {
        if(StringUtils.isBlank(platform)){
            platform="dy";
        }
        /**
         * 1、参数传过来，替换哪条线
         * 2、替换的云客通url
         */
        Map<String, String> xianluNameAndPic = userStant.getXianluNameAndPic(xianlu,platform);

        modelMap.addAttribute("title", xianlu+"替换云雀");
        modelMap.addAttribute("message", title);
//        modelMap.addAttribute("myName", xianluNameAndPic.get("xianluName"));
        modelMap.addAttribute("myName", "");
        modelMap.addAttribute("myPic", xianluNameAndPic.get("xianluPic"));

        List<ChatMessage> chatMessageList=tihuanChatMessage(msgurl);
        modelMap.addAttribute("msgList", chatMessageList);
        modelMap.addAttribute("firstDateTimeStr", chatMessageList.get(0).getDateTimeStr());
        modelMap.addAttribute("welcomword", xianluNameAndPic.get("welcomword"));
        for (ChatMessage chatMessage : chatMessageList){
            if(chatMessage.getMsgType()==1){
                modelMap.addAttribute("userName",chatMessage.getUserName());
                modelMap.addAttribute("userPic", userStant.getRandomUserPic());
                break;
            }
        }
        if(platform!=null&&!platform.trim().equals("")){
            if(platform.equals("dy")){
                return "siyubao_cq";
            }
            if(platform.equals("xhs")){
                return "chat-interface-v4.html";
            }
            if(platform.equals("sph")){
                return "wechat-mobile-chat.html";
            }
        }
        return "siyubao_cq";
    }

    /**
     * 替换消息
     * @param
     * @return
     */
    private List<ChatMessage> tihuanChatMessage(String msgUrl) {
        List<SiyubaoJsonResponse> siyubaoJsonResponses = deepSeekService.requestYunqueChatJson(msgUrl);
        List<ChatMessage> chatMessageList=new ArrayList();

        String showName=siyubaoJsonResponses.get(0).getShowname();
        for (SiyubaoJsonResponse siyubaoJsonResponse : siyubaoJsonResponses){
            if(!siyubaoJsonResponse.getType().equals("msg")){
                continue;
            }
            ChatMessage chatMessage=new ChatMessage();
            int msgType=1;
            if(siyubaoJsonResponse.getFrom().equals("worker")){
                msgType=2;
            }
            String dateTimeStr="";

//
            long microseconds = Long.valueOf(siyubaoJsonResponse.getMicrotime()) ;

            // 转换为毫秒（微秒/1000）
            long milliseconds = microseconds / 1000;

            // 创建Date对象
            Date date = new Date(milliseconds);

            // 设置日期格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // 设置时区（可选，根据需要设置）
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+8")); // 例如设置为东八区

            // 格式化输出
            String formattedDate = sdf.format(date);


            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 解析为目标时间
            LocalDateTime targetTime = LocalDateTime.parse(formattedDate, formatter);
            dateTimeStr=userStant.getTimeStr(targetTime.getHour())+":"+userStant.getTimeStr(targetTime.getMinute());
//            if(isTodayDateStr(siyubaoJsonResponse.getSend_time())){
//                //当天时间
//            }else{
//                dateTimeStr=siyubaoJsonResponse.getSend_time();
//            }
//            chatMessage.setContentType();
            chatMessage.setDateTimeStr(dateTimeStr);
            chatMessage.setMsg(StringUtils.isBlank(siyubaoJsonResponse.getText())?siyubaoJsonResponse.getContent():siyubaoJsonResponse.getText());
            chatMessage.setMsgType(msgType);
            if(msgType==1){
                chatMessage.setUserName(showName);
                chatMessage.setUserPic(siyubaoJsonResponse.getAvatar());
            }
            chatMessageList.add(chatMessage);
        }

        return chatMessageList;
    }



}
