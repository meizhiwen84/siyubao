package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.service.RouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
public class ChatPreviewController {

    @Autowired
    private RouteService routeService;

    @GetMapping("/chat-preview")
    public String chatPreview(@RequestParam(required = false) String xianlu, Model model) {
        log.info("访问聊天预览页面，xianlu参数: {}", xianlu);
        
        // 只获取状态为"打开"的线路数据并添加到模型中
        model.addAttribute("routes", routeService.getRoutesByStatus(true));
        
        return "chat-preview.html";
    }
}