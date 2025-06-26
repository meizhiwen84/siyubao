package cn.laobayou.siyubao.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
public class MbController {


    @RequestMapping("/list")
    public String gen(ModelMap modelMap){
        modelMap.addAttribute("message", "老梅");

        return "siyubao_cq";
    }

}
