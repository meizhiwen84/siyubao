package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.CardKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.Optional;

@Service
public class CardKeyService {
    public static final String SESSION_CARD_CODE = "SESSION_CARD_CODE";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.base-url:http://localhost:6943}")
    private String adminBaseUrl;

    public boolean isValid(CardKey cardKey) {
        return cardKey != null && Boolean.TRUE.equals(cardKey.getEnabled()) && cardKey.getRemaining() != null && cardKey.getRemaining() > 0;
    }

    public Optional<CardKey> findByCode(String code) {
        if (code == null) return Optional.empty();
        String t = code.trim();
        if (t.isEmpty()) return Optional.empty();
        try {
            CardKey ck = restTemplate.getForObject(adminBaseUrl + "/api/card/{code}", CardKey.class, t);
            return Optional.ofNullable(ck);
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    public boolean validateAndStore(HttpSession session, String code) {
        Optional<CardKey> opt = findByCode(code);
        if (!opt.isPresent()) return false;
        CardKey ck = opt.get();
        if (!isValid(ck)) return false;
        session.setAttribute(SESSION_CARD_CODE, ck.getCode());
        return true;
    }

    public Optional<CardKey> currentSessionCard(HttpSession session) {
        Object v = session.getAttribute(SESSION_CARD_CODE);
        if (v == null) return Optional.empty();
        return findByCode(v.toString());
    }

    public boolean decrementAfterSuccess(HttpSession session) {
        Object v = session.getAttribute(SESSION_CARD_CODE);
        if (v == null) return false;
        try {
            java.util.Map resp = restTemplate.postForObject(adminBaseUrl + "/api/card/{code}/decrement", null, java.util.Map.class, v.toString());
            if (resp == null) return false;
            Object ok = resp.get("success");
            return Boolean.TRUE.equals(ok);
        } catch (RestClientException e) {
            return false;
        }
    }
}
