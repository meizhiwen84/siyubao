package cn.laobayou.siyubao.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 * 处理首页相关的请求
 */
@Slf4j
@Controller
public class HomeController {

    /**
     * 首页
     * @param model 模型对象
     * @return 首页模板
     */
    @GetMapping("/")
    public String index(Model model) {
        log.info("访问首页");
        
        // 可以在这里添加一些首页需要的数据
        model.addAttribute("title", "思语宝 - 多平台聊天对话截图生成工具");
        model.addAttribute("description", "支持微信、QQ、Telegram等多平台的私信聊天对话截图生成，自定义线路配置，随意生成所需的对话截图");
        
        return "index";
    }
    
    /**
     * 首页重定向（兼容性处理）
     * @param model 模型对象
     * @return 重定向到首页
     */
    @GetMapping("/home")
    public String home(Model model) {
        return "redirect:/";
    }
    
    /**
     * iframe错误页面测试页面
     * @param model 模型对象
     * @return iframe测试页面模板
     */
    @GetMapping("/iframe-test")
    public String iframeTest(Model model) {
        log.info("访问iframe错误页面测试页面");
        model.addAttribute("title", "iframe错误页面跨框架导航测试");
        return "iframe-test";
    }
    
    /**
     * 触发500错误（用于测试）
     * @return 抛出异常
     */
    @GetMapping("/trigger-error-500")
    public String triggerError500() {
        log.info("触发500错误进行测试");
        throw new RuntimeException("这是一个测试用的500错误");
    }
}