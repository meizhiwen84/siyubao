package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.bean.SiyubaoConfig;
import cn.laobayou.siyubao.bean.UserQuestionContent;
import cn.laobayou.siyubao.bean.DeepSeekRequestMessage;
import cn.laobayou.siyubao.bean.RoleType;
import cn.laobayou.siyubao.bean.UserStant;
import cn.laobayou.siyubao.service.DeepSeekService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.util.DateUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalTime;
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
    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private SiyubaoConfig siyubaoConfig;

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
    public String gen(ModelMap modelMap) throws IOException {
        LocalTime now = LocalTime.now();



        modelMap.addAttribute("message", title);
        modelMap.addAttribute("myPic", myPic);
        modelMap.addAttribute("myName", myName);
        modelMap.addAttribute("userName", UserStant.getRandomUserName());
        modelMap.addAttribute("userPic", UserStant.getRandomUserPic());
        modelMap.addAttribute("hellomsg", siyubaoConfig.getWelcomeword());

        List<ChatMessage> chatMessageList=generateChatMessage(now);
        modelMap.addAttribute("msgList", chatMessageList);


        return "siyubao_cq";
    }

    private String getAnswerByQ(List<DeepSeekRequestMessage> messageList,UserQuestionContent q) throws IOException {
        messageList.add(DeepSeekRequestMessage.builder().role(RoleType.user.name()).content(q.getPreWord()+q.getContent()+q.getEndWord()+",返回结果不要超过50个字").build());

        String rs = deepSeekService.chat(messageList);
        messageList.add(DeepSeekRequestMessage.builder().role(RoleType.assistant.name()).content(rs).build());
        return rs;
    }

    private List<ChatMessage> generateChatMessage(LocalTime now) throws IOException {
        //最原始的第一句话的时间
        LocalTime localTimeBefore = now.minusMinutes(RandomUtils.nextInt(20, 40));

        List<ChatMessage> chatMessageList=new ArrayList();
        ChatMessage m1=new ChatMessage();
        m1.setMsg(siyubaoConfig.getWelcomeword());
        m1.setDateTimeStr(UserStant.getTimeStr(localTimeBefore.getHour())+":"+UserStant.getTimeStr(localTimeBefore.getMinute()));
        m1.setMsgType(2);
        chatMessageList.add(m1);

        List<DeepSeekRequestMessage> messageList=new ArrayList<>();//问问题的上下文

        //把第一句问候语加进去
        DeepSeekRequestMessage helloMsg = DeepSeekRequestMessage.builder().content(siyubaoConfig.getWelcomeword()).role(RoleType.assistant.name()).build();
        messageList.add(helloMsg);

        /**
         * 1、随机生成用户问几个问题，2-4个问题，每个问题不能问重复的
         * 2、每个问题调用deepseek去获取答案，
         * 3、到回复最后一个问题发送过后，马上发送一条"要求用户留联系方式的问题"
         * 4、每个要判断下有没有电话号码或者微信号，有的话就直接返回最后一句话
         */
        int qcnt = RandomUtils.nextInt(1, 2);//问题数量
        LocalTime tempTime=localTimeBefore;

        /**
         * 获得这个数量的问题
         */
        ArrayList<UserQuestionContent> cntQuestions = UserStant.getCntQuestions(qcnt);
        //再添加上多少钱的这一个问题
        UserQuestionContent userQuestionContent1=UserQuestionContent.builder().content(UserStant.getRandomFamilySizeResponse()).haiyouqs(true).preWord(siyubaoConfig.getJitianjiwan()).build();
        cntQuestions.add(0,userQuestionContent1);

        UserQuestionContent userQuestionContent2=UserQuestionContent.builder().content(UserStant.getDsQian()).build();
        cntQuestions.add(1,userQuestionContent2);

        UserQuestionContent userQuestionContent3=UserQuestionContent.builder().content(UserStant.getSendLiuziMsg()).roleType(RoleType.assistant).build();
        cntQuestions.add(userQuestionContent3);

        //循环生成每一个问题和调deepseek查找答案
        for(UserQuestionContent q : cntQuestions){
            if(q.getRoleType()!=null&&q.getRoleType().name().equals(RoleType.assistant.name())){
                System.out.println("是我最后的消息========");
                tempTime = tempTime.plusMinutes(RandomUtils.nextInt(0,1));//减3分钟
                ChatMessage liuzicm=new ChatMessage();
                chatMessageList.add(liuzicm);
                liuzicm.setMsg(q.getContent());
                liuzicm.setDateTimeStr(UserStant.getTimeStr(tempTime.getHour())+":"+UserStant.getTimeStr(tempTime.getMinute()));
                liuzicm.setMsgType(2);
                continue;
            }

            System.out.println("问 ："+q);
            tempTime = tempTime.plusMinutes(RandomUtils.nextInt(1, 10/(qcnt+2)));//减3分钟
            ChatMessage tcm=new ChatMessage();
            chatMessageList.add(tcm);
            tcm.setMsg(q.getContent()+"");//用户的问题
//        m1.setMsg(UserStant.getRandomFamilySizeResponse());
            tcm.setDateTimeStr(UserStant.getTimeStr(tempTime.getHour())+":"+UserStant.getTimeStr(tempTime.getMinute()));
            tcm.setMsgType(1);

            if(q.isHaiyouqs()){
                //表示后面马上还跟的有用户问的问题
                continue;
            }

            //生成这个问题的答案 调deepseek
            String as=this.getAnswerByQ(messageList,q);
            System.out.println("答 ："+as);
            tempTime = tempTime.plusMinutes(RandomUtils.nextInt(1, 10/(qcnt+2)));//减3分钟
            ChatMessage tcmas=new ChatMessage();
            chatMessageList.add(tcmas);
            tcmas.setMsg(as);
            tcmas.setDateTimeStr(UserStant.getTimeStr(tempTime.getHour())+":"+UserStant.getTimeStr(tempTime.getMinute()));
            tcmas.setMsgType(2);
        }

        //发送号码
        ChatMessage hmcm=new ChatMessage();
//        m1.setContentType(2);
//        m1.setMsg("./avatar/avatar_"+33+".jpg");
        hmcm.setMsg(UserStant.getRandomOkResponse()+"q1w2hhhq1");
        hmcm.setDateTimeStr(UserStant.getTimeStr(now.getHour())+":"+UserStant.getTimeStr(now.getMinute()));
        hmcm.setMsgType(1);
        chatMessageList.add(hmcm);

        ChatMessage m2=new ChatMessage();
        m2.setMsg(siyubaoConfig.getFinalword());
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
        m1.setMsg("大概7月吧先了解下");
//        m1.setMsg(UserStant.getRandomFamilySizeResponse());
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
//        m1.setContentType(2);
//        m1.setMsg("./avatar/avatar_"+33+".jpg");
        m1.setMsg(UserStant.getRandomOkResponse()+"q1w2hhhq1");
        m1.setDateTimeStr(UserStant.getTimeStr(tempTime.getHour())+":"+UserStant.getTimeStr(tempTime.getMinute()));
        m1.setMsgType(1);
        chatMessageList.add(m1);

//        ChatMessage mm1=new ChatMessage();
//        mm1.setMsg("好的，您发了我加您");
//        mm1.setDateTimeStr(UserStant.getTimeStr(tempTime.getHour())+":"+UserStant.getTimeStr(tempTime.getMinute()));
//        mm1.setMsgType(2);
//        chatMessageList.add(mm1);

//        ChatMessage m2=new ChatMessage();
//        m2.setMsg("Hyd19850721");//====================填微信
//        m2.setDateTimeStr(UserStant.getTimeStr(now.getHour())+":"+UserStant.getTimeStr(now.getMinute()));
//        m2.setMsgType(1);
//        chatMessageList.add(m2);

    }

}
