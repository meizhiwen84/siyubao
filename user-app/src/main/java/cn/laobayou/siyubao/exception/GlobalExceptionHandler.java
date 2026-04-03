package cn.laobayou.siyubao.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 全局异常处理器
 * 捕获应用程序中的各种异常，并返回友好的错误页面
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理404异常
     * @param request HTTP请求对象
     * @param ex 异常对象
     * @return 错误页面视图
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFoundException(HttpServletRequest request, NoHandlerFoundException ex) {
        String errorCode = generateErrorCode("404");
        
        log.warn("404错误 - 错误代码: {}, 请求URI: {}, 客户端IP: {}", 
                errorCode, request.getRequestURI(), getClientIpAddress(request));
        
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("error_code", errorCode);
        modelAndView.addObject("error_title", "页面未找到");
        modelAndView.addObject("error_message", "抱歉，您访问的页面不存在或已被移除。");
        modelAndView.addObject("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return modelAndView;
    }

    /**
     * 处理运行时异常
     * @param request HTTP请求对象
     * @param ex 异常对象
     * @return 错误页面视图
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleRuntimeException(HttpServletRequest request, RuntimeException ex) {
        String errorCode = generateErrorCode("500");
        
        log.error("运行时异常 - 错误代码: {}, 请求URI: {}, 客户端IP: {}", 
                errorCode, request.getRequestURI(), getClientIpAddress(request), ex);
        
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("error_code", errorCode);
        modelAndView.addObject("error_title", "服务器内部错误");
        modelAndView.addObject("error_message", "服务器遇到了一个意外的错误，我们正在努力修复。");
        modelAndView.addObject("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return modelAndView;
    }

    /**
     * 处理非法参数异常
     * @param request HTTP请求对象
     * @param ex 异常对象
     * @return 错误页面视图
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException ex) {
        String errorCode = generateErrorCode("400");
        
        log.warn("非法参数异常 - 错误代码: {}, 请求URI: {}, 客户端IP: {}, 异常信息: {}", 
                errorCode, request.getRequestURI(), getClientIpAddress(request), ex.getMessage());
        
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("error_code", errorCode);
        modelAndView.addObject("error_title", "请求参数错误");
        modelAndView.addObject("error_message", "请求参数不正确，请检查您的输入。");
        modelAndView.addObject("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return modelAndView;
    }

    /**
     * 处理空指针异常
     * @param request HTTP请求对象
     * @param ex 异常对象
     * @return 错误页面视图
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleNullPointerException(HttpServletRequest request, NullPointerException ex) {
        String errorCode = generateErrorCode("500");
        
        log.error("空指针异常 - 错误代码: {}, 请求URI: {}, 客户端IP: {}", 
                errorCode, request.getRequestURI(), getClientIpAddress(request), ex);
        
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("error_code", errorCode);
        modelAndView.addObject("error_title", "系统内部错误");
        modelAndView.addObject("error_message", "系统遇到了一个内部错误，我们正在努力修复。");
        modelAndView.addObject("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return modelAndView;
    }

    /**
     * 处理所有其他异常
     * @param request HTTP请求对象
     * @param ex 异常对象
     * @return 错误页面视图
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGenericException(HttpServletRequest request, Exception ex) {
        String errorCode = generateErrorCode("500");
        
        log.error("未知异常 - 错误代码: {}, 请求URI: {}, 客户端IP: {}, 异常类型: {}", 
                errorCode, request.getRequestURI(), getClientIpAddress(request), ex.getClass().getSimpleName(), ex);
        
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("error_code", errorCode);
        modelAndView.addObject("error_title", "系统遇到了一些问题");
        modelAndView.addObject("error_message", "抱歉，系统在处理您的请求时发生了异常。我们已经记录了这个问题，技术团队会尽快处理。");
        modelAndView.addObject("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return modelAndView;
    }

    /**
     * 生成错误代码
     * @param statusCode 状态码
     * @return 格式化的错误代码
     */
    private String generateErrorCode(String statusCode) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        return String.format("ERR_%s_%s_%s", statusCode, timestamp, randomId);
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
}