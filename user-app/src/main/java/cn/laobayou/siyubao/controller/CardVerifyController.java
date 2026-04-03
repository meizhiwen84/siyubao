package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.CardKey;
import cn.laobayou.siyubao.service.CardKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class CardVerifyController {
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

