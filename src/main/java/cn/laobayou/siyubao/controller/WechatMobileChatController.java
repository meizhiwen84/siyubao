package cn.laobayou.siyubao.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class WechatMobileChatController {

    @GetMapping("/wechat-mobile-chat")
    public String wechatMobileChat() {
        log.info("访问手机版微信客服聊天页面");
        return "wechat-mobile-chat.html";
    }
}