package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.CardKeyMessage;
import cn.laobayou.siyubao.bean.CardKey;
import cn.laobayou.siyubao.service.CardKeyService;
import cn.laobayou.siyubao.service.CardKeyMessageService;
import cn.laobayou.siyubao.service.RouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpSession;

@Slf4j
@Controller
public class CardKeyMessageController {
    @Autowired
    private CardKeyMessageService service;
    @Autowired
    private RouteService routeService;
    @Autowired
    private CardKeyService cardKeyService;

    @GetMapping("/card-message")
    public String page(@RequestParam String erbao, Model model, HttpSession session) {
        if (!"zhr".equals(erbao)) {
            model.addAttribute("message", "非法访问");
            return "simple-error";
        }
        String cardKey = currentCardKey(session);
        model.addAttribute("routes", routeService.getAllRoutes(cardKey));
        model.addAttribute("platforms", Arrays.asList("dy", "xhs", "sph"));
        return "card-message";
    }

    @GetMapping("/card-message/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String cardKey,
            @RequestParam(required = false) String line,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> result = new HashMap<>();
        String ck = sanitize(cardKey, 64);
        String ln = sanitize(line, 64);
        String pf = sanitize(platform, 8);
        String ph = sanitize(phone, 32);
        if (pf != null && !Arrays.asList("dy", "xhs", "sph").contains(pf)) {
            result.put("success", false);
            result.put("message", "平台参数不合法");
            return ResponseEntity.badRequest().body(result);
        }
        Page<CardKeyMessage> p = service.search(ck, ln, pf, ph, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        result.put("success", true);
        result.put("total", p.getTotalElements());
        result.put("pages", p.getTotalPages());
        result.put("page", Math.max(page, 0));
        result.put("size", Math.min(Math.max(size, 1), 100));
        result.put("data", p.getContent());
        return ResponseEntity.ok(result);
    }

    private String sanitize(String s, int maxLen) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() > maxLen) t = t.substring(0, maxLen);
        return t;
    }

    private String currentCardKey(HttpSession session) {
        Optional<CardKey> opt = cardKeyService.currentSessionCard(session);
        if (!opt.isPresent() || !cardKeyService.isValid(opt.get())) {
            throw new RuntimeException("卡密无效或未登录");
        }
        return opt.get().getCode();
    }
}
