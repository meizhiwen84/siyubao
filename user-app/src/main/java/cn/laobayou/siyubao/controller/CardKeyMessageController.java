package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.desktop.DesktopRuntime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import javax.servlet.http.HttpSession;

@Slf4j
@Controller
public class CardKeyMessageController {
    @GetMapping("/card-message")
    public String page(Model model, HttpSession session) {
        return DesktopRuntime.isDesktopMode() ? "redirect:/app/card-message" : "redirect:/app/card-message";
    }
}
