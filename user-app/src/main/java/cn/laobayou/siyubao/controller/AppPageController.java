package cn.laobayou.siyubao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
public class AppPageController {
    @GetMapping({"", "/"})
    public String index() {
        return "forward:/app/index.html";
    }

    @GetMapping("/{p1:[^\\.]*}")
    public String route1() {
        return "forward:/app/index.html";
    }

    @GetMapping("/{p1:[^\\.]*}/{p2:[^\\.]*}")
    public String route2() {
        return "forward:/app/index.html";
    }

    @GetMapping("/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}")
    public String route3() {
        return "forward:/app/index.html";
    }
}
