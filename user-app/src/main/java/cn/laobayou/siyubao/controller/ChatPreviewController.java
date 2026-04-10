package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.desktop.DesktopRuntime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import javax.servlet.http.HttpSession;

@Slf4j
@Controller
public class ChatPreviewController {

    @GetMapping("/chat-preview")
    public String chatPreview(@RequestParam(required = false) String xianlu, Model model, HttpSession session) {
        return "redirect:/app/chat-preview";
    }
}
