package cn.laobayou.siyubao.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 系统异常处理控制器
 * 处理所有未被捕获的异常，向用户展示友好的错误页面
 */
@Slf4j
@Controller
public class CustomErrorController implements ErrorController {

    private static final String ERROR_PATH = "/error";

    /**
     * 处理错误请求
     * @param request HTTP请求对象
     * @param model 模型对象
     * @return 错误页面模板
     */
    @RequestMapping(ERROR_PATH)
    public String handleError(HttpServletRequest request, Model model) {
        // 获取错误状态码
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        // 获取异常信息
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        // 生成错误代码
        String errorCode = generateErrorCode(status);
        
        // 记录错误日志
        logError(request, status, exception, message, requestUri, errorCode);
        
        // 设置模型属性
        model.addAttribute("error_code", errorCode);
        model.addAttribute("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 根据状态码设置不同的错误信息
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            switch (statusCode) {
                case 404:
                    model.addAttribute("error_title", "页面未找到");
                    model.addAttribute("error_message", "抱歉，您访问的页面不存在或已被移除。");
                    break;
                case 403:
                    model.addAttribute("error_title", "访问被拒绝");
                    model.addAttribute("error_message", "抱歉，您没有权限访问此页面。");
                    break;
                case 500:
                    model.addAttribute("error_title", "服务器内部错误");
                    model.addAttribute("error_message", "服务器遇到了一个意外的错误，我们正在努力修复。");
                    break;
                case 503:
                    model.addAttribute("error_title", "服务暂时不可用");
                    model.addAttribute("error_message", "服务器正在维护中，请稍后再试。");
                    break;
                default:
                    model.addAttribute("error_title", "系统遇到了一些问题");
                    model.addAttribute("error_message", "抱歉，系统在处理您的请求时发生了异常。我们已经记录了这个问题，技术团队会尽快处理。");
                    break;
            }
        } else {
            model.addAttribute("error_title", "系统遇到了一些问题");
            model.addAttribute("error_message", "抱歉，系统在处理您的请求时发生了异常。我们已经记录了这个问题，技术团队会尽快处理。");
        }
        
        return "error";
    }

    /**
     * 生成错误代码
     * @param status 错误状态码
     * @return 格式化的错误代码
     */
    private String generateErrorCode(Object status) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        if (status != null) {
            return String.format("ERR_%s_%s_%s", status.toString(), timestamp, randomId);
        } else {
            return String.format("ERR_UNKNOWN_%s_%s", timestamp, randomId);
        }
    }

    /**
     * 记录错误日志
     * @param request HTTP请求对象
     * @param status 错误状态码
     * @param exception 异常对象
     * @param message 错误消息
     * @param requestUri 请求URI
     * @param errorCode 错误代码
     */
    private void logError(HttpServletRequest request, Object status, Object exception, 
                         Object message, Object requestUri, String errorCode) {
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("系统异常处理 - ");
        logMessage.append("错误代码: ").append(errorCode);
        
        if (status != null) {
            logMessage.append(", 状态码: ").append(status);
        }
        
        if (requestUri != null) {
            logMessage.append(", 请求URI: ").append(requestUri);
        }
        
        if (message != null) {
            logMessage.append(", 错误消息: ").append(message);
        }
        
        // 获取客户端信息
        String userAgent = request.getHeader("User-Agent");
        String clientIp = getClientIpAddress(request);
        
        logMessage.append(", 客户端IP: ").append(clientIp);
        logMessage.append(", User-Agent: ").append(userAgent);
        
        // 记录错误日志
        if (exception != null) {
            log.error(logMessage.toString(), (Throwable) exception);
        } else {
            log.error(logMessage.toString());
        }
    }

    /**
     * 获取客户端真实IP地址
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 返回错误路径
     * @return 错误处理路径
     */
    public String getErrorPath() {
        return ERROR_PATH;
    }
}