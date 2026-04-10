package cn.laobayou.siyubao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminSpaController {
    @GetMapping({"/admin", "/admin/"})
    public String root() {
        return "forward:/admin/index.html";
    }

    @GetMapping("/admin/{path:(?!assets$)[^\\.]+}")
    public String top(@PathVariable String path) {
        return "forward:/admin/index.html";
    }

    @GetMapping("/admin/{path:(?!assets$)[^\\.]+}/**")
    public String deep(@PathVariable String path) {
        return "forward:/admin/index.html";
    }
}
