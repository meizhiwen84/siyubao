package cn.laobayou.siyubao.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
public class ChatPreviewController {

    @GetMapping("/chat-preview")
    public String chatPreview(@RequestParam(required = false) String xianlu) {
        log.info("访问聊天预览页面，xianlu参数: {}", xianlu);
        return "chat-preview.html";
    }
}