package cn.laobayou.siyubao.interceptor;

import cn.laobayou.siyubao.bean.CardKey;
import cn.laobayou.siyubao.service.CardKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Optional;

@Component
public class CardKeySessionInterceptor implements HandlerInterceptor {
    @Autowired
    private CardKeyService cardKeyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect("/card-verify");
            return false;
        }
        Optional<CardKey> opt = cardKeyService.currentSessionCard(session);
        if (!opt.isPresent() || !cardKeyService.isValid(opt.get())) {
            response.sendRedirect("/card-verify");
            return false;
        }
        return true;
    }
}

