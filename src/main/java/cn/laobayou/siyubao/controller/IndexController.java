package cn.laobayou.siyubao.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
