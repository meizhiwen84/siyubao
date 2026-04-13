package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.service.ChatMessageHistoryService;
import cn.laobayou.siyubao.service.ChatPageData;
import cn.laobayou.siyubao.service.ChatPageService;
import cn.laobayou.siyubao.service.LocalUserSessionService;
import cn.laobayou.siyubao.service.RemoteAdminService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
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
    @Autowired
    private ChatMessageHistoryService chatMessageHistoryService;
    @Autowired
    private LocalUserSessionService localUserSessionService;
    @Autowired
    private RemoteAdminService remoteAdminService;
    @Autowired
    private ChatPageService chatPageService;

    @RequestMapping("/lookchatcontent")
    public String lookchatcontent(ModelMap modelMap, String xianshiname, String platform,String xianlu, String userName,String userAvatar, String chatContent, String chatBg) {
        String pf = (platform == null || platform.trim().isEmpty()) ? "dy" : platform.trim();
        String xn = (xianshiname == null) ? "" : xianshiname.trim();
        modelMap.addAttribute("platform", pf);
        modelMap.addAttribute("xianshiname", xn);
        modelMap.addAttribute("xianlu", xianlu);
        modelMap.addAttribute("userName", userName);
        modelMap.addAttribute("userAvatar", userAvatar);
        modelMap.addAttribute("chatContent", chatContent);
        modelMap.addAttribute("chatBg", chatBg);
        return "lookchatcontent";
    }

    @RequestMapping("/reGenerateDyChat")
    public String reGen(ModelMap modelMap,@RequestParam String xianshiname,String platform,String xianlu,String userName,String userAvatar,String chatContent, String chatBg, javax.servlet.http.HttpSession session ) throws IOException {
        platform = (platform == null || platform.trim().isEmpty()) ? "dy" : platform.trim();
        xianshiname = (xianshiname == null) ? "" : xianshiname.trim();
        LocalTime now = LocalTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        Long userId = localUserSessionService.userId();
        if (userId == null || userId <= 0) {
            modelMap.addAttribute("message", "未登录");
            return "simple-error";
        }
        if (chatContent == null || chatContent.trim().isEmpty()) {
            List<String> cc = Files.readAllLines(Paths.get("./rechatcontent/rechatcontent.txt"));
            if (cc.size() > 1) {
                throw new RuntimeException("文件内容出错");
            }
            Pattern pattern = Pattern.compile("线路:(.*?)\\|\\|用户名称:(.*?)\\|\\|用户头像:(.*?)\\|\\|聊天内容:(.*)");
            Matcher matcher = pattern.matcher(cc.get(0));
            if (matcher.find()) {
                xianlu = matcher.group(1);
                userName = matcher.group(2);
                userAvatar = matcher.group(3);
                chatContent = matcher.group(4);
            }
        }
        log.info("线路: " + xianlu);
        log.info("用户名称: " + userName);
        log.info("用户头像: " + userAvatar);
        log.info("聊天内容: " + chatContent);
        log.info("聊天背景: " + chatBg);

        List<ChatMessage> chatMessageList = JSON.parseArray(chatContent, ChatMessage.class);
        ChatPageData page = chatPageService.buildReGeneratePage(now, userId, xianlu, xianshiname, platform, userAvatar, chatMessageList, chatBg);
        modelMap.addAllAttributes(page.getModel());
        return page.getTemplate();
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
     *chatContent :表示聊天内容是从前面传过来，不是从文件里面读取的
     sph：  洗成视频号的粉

     platform:dy  xhs sph
     * @param modelMap
     * @return
     */

    @RequestMapping("/generateDyChat")
    public String gen(ModelMap modelMap,@RequestParam(required = false, defaultValue = "sc") String xianlu, String xianshiname, @RequestParam(required = false) String chatContent,String platform, String chatBg, javax.servlet.http.HttpSession session) throws IOException {
        LocalTime now = LocalTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        Long userId = localUserSessionService.userId();
        String token = localUserSessionService.token();
        if (userId == null || userId <= 0 || token == null || token.trim().isEmpty()) {
            modelMap.addAttribute("message", "未登录");
            return "simple-error";
        }

        Map<String, Object> consume = remoteAdminService.consumeGenerate(token);
        if (consume == null || !Boolean.TRUE.equals(consume.get("success"))) {
            String msg = consume == null ? null : String.valueOf(consume.get("message"));
            modelMap.addAttribute("message", msg == null || msg.trim().isEmpty() ? "今日生成次数已用完" : msg);
            return "simple-error";
        }

        ChatPageData page = chatPageService.buildPage(now, userId, xianlu, xianshiname, chatContent, platform, chatBg);
        modelMap.addAllAttributes(page.getModel());
        chatMessageHistoryService.save(userId, xianlu, platform, page.getMessages());
        return page.getTemplate();
    }

}
