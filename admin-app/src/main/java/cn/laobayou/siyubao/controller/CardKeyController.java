package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.CardKey;
import cn.laobayou.siyubao.service.CardKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CardKeyController {
    @Autowired
    private CardKeyService cardKeyService;

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
}

