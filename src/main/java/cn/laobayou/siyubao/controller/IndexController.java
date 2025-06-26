package cn.laobayou.siyubao.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@Slf4j
@RestController
public class IndexController {

    @RequestMapping("/index")
    public String hello(){
        log.info("这是我的第一个controoler");
        log.info("这是我的第一个controoler");
        return "hello meizhiwen";
    }


}
