package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.CardKey;
import cn.laobayou.siyubao.service.CardKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
public class CardKeyController {
    @Autowired
    private CardKeyService cardKeyService;

    @GetMapping("/card-verify")
    public String cardVerifyPage(Model model, @RequestParam(required = false) String error) {
        model.addAttribute("error", error);
        return "card-verify";
    }

    @PostMapping("/card-verify")
    public String cardVerify(@RequestParam String code, HttpSession session, Model model) {
        boolean ok = cardKeyService.validateAndStore(session, code);
        if (!ok) {
            model.addAttribute("error", "卡密无效或剩余次数不足");
            return "card-verify";
        }
        return "redirect:/chat-preview";
    }

    @GetMapping("/card-config")
    public String cardConfigPage(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<CardKey> p = cardKeyService.findAll(PageRequest.of(page, size));
        model.addAttribute("page", p);
        return "card-config";
    }

    @PostMapping("/card-config/create")
    public String create(@RequestParam String code, @RequestParam int total) {
        cardKeyService.create(code, total);
        return "redirect:/card-config";
    }

    @PostMapping("/card-config/update")
    public String update(@RequestParam Long id, @RequestParam(required = false) Integer total, @RequestParam(required = false) Integer remaining, @RequestParam(required = false) Boolean enabled) {
        cardKeyService.update(id, total, remaining, enabled);
        return "redirect:/card-config";
    }

    @PostMapping("/card-config/toggle")
    public String toggle(@RequestParam Long id, @RequestParam boolean enabled) {
        cardKeyService.update(id, null, null, enabled);
        return "redirect:/card-config";
    }

    @GetMapping("/api/card/session/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sessionStatus(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Optional<CardKey> opt = cardKeyService.currentSessionCard(session);
        if (!opt.isPresent()) {
            result.put("success", false);
            result.put("message", "未验证");
            return ResponseEntity.status(401).body(result);
        }
        CardKey ck = opt.get();
        result.put("success", true);
        result.put("enabled", ck.getEnabled());
        result.put("remaining", ck.getRemaining());
        result.put("code", ck.getCode());
        return ResponseEntity.ok(result);
    }
}

