package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.service.RouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import javax.servlet.http.HttpSession;
import java.util.Optional;
import cn.laobayou.siyubao.bean.CardKey;
import cn.laobayou.siyubao.service.CardKeyService;

@Slf4j
@Controller
public class ChatPreviewController {

    @Autowired
    private RouteService routeService;
    @Autowired
    private CardKeyService cardKeyService;

    @GetMapping("/chat-preview")
    public String chatPreview(@RequestParam(required = false) String xianlu, Model model, HttpSession session) {
        log.info("访问聊天预览页面，xianlu参数: {}", xianlu);
        Optional<CardKey> opt = cardKeyService.currentSessionCard(session);
        if (!opt.isPresent() || !cardKeyService.isValid(opt.get())) {
            return "redirect:/card-verify";
        }
        String cardKey = opt.get().getCode();
        // 只获取状态为"打开"的线路数据并添加到模型中
        model.addAttribute("routes", routeService.getRoutesByStatus(true, cardKey));
        model.addAttribute("cardCode", cardKey);
        model.addAttribute("cardRemaining", opt.get().getRemaining());
        model.addAttribute("cardEnabled", opt.get().getEnabled());
        
        return "chat-preview.html";
    }
}
