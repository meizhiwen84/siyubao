package cn.laobayou.siyubao.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RemoteAdminService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${admin.base-url:http://localhost:6943}")
    private String baseUrl;

    private String normalize(String url) {
        if (url == null) return "";
        String t = url.trim();
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    private String fallbackBaseUrl() {
        String b = normalize(baseUrl);
        if (b.isEmpty()) return "http://localhost:6943";
        if (b.contains(":6942")) return b.replace(":6942", ":6943");
        return "http://localhost:6943";
    }

    public Map<String, Object> publicLogin(String username, String password, String deviceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("deviceId", deviceId);
        return postJson(normalize(baseUrl) + "/api/public/login", body, null);
    }

    public Map<String, Object> publicRegister(String username, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        return postJson(normalize(baseUrl) + "/api/public/register", body, null);
    }

    public Map<String, Object> publicPlans() {
        return getJson(normalize(baseUrl) + "/api/public/plans", null);
    }

    public Map<String, Object> me(String token) {
        return getJson(normalize(baseUrl) + "/api/me", token);
    }

    public Map<String, Object> heartbeat(String token) {
        return postJson(normalize(baseUrl) + "/api/heartbeat", new HashMap<>(), token);
    }

    public Map<String, Object> consumeGenerate(String token) {
        return postJson(normalize(baseUrl) + "/api/generate/consume", new HashMap<>(), token);
    }

    public Map<String, Object> paymentConfig() {
        return getJson(normalize(baseUrl) + "/api/payment/config", null);
    }

    public Map<String, Object> paymentMyOrders(String token) {
        return getJson(normalize(baseUrl) + "/api/payment/my-orders", token);
    }

    public Map<String, Object> paymentSubmit(String token, Map<String, Object> body) {
        return postJson(normalize(baseUrl) + "/api/payment/submit", body, token);
    }

    public Map<String, Object> customerServiceInfo() {
        return getJson(normalize(baseUrl) + "/api/customer-service/info", null);
    }

    public Map<String, Object> feedbackSubmit(String token, Map<String, Object> body) {
        return postJson(normalize(baseUrl) + "/api/feedback/submit", body, token);
    }

    private Map<String, Object> getJson(String url, String token) {
        try {
            return doGetJson(url, token);
        } catch (HttpClientErrorException.NotFound e) {
            String fb = fallbackBaseUrl();
            String cur = normalize(baseUrl);
            if (!fb.equals(cur)) {
                try {
                    return doGetJson(url.replace(cur, fb), token);
                } catch (Exception ignored) {
                }
            }
            throw e;
        }
    }

    private Map<String, Object> doGetJson(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        if (token != null && !token.trim().isEmpty()) headers.set("Authorization", "Bearer " + token.trim());
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        Object body = resp.getBody();
        return body instanceof Map ? (Map<String, Object>) body : new HashMap<>();
    }

    private Map<String, Object> postJson(String url, Map<String, Object> body, String token) {
        try {
            return doPostJson(url, body, token);
        } catch (HttpClientErrorException.NotFound e) {
            String fb = fallbackBaseUrl();
            String cur = normalize(baseUrl);
            if (!fb.equals(cur)) {
                try {
                    return doPostJson(url.replace(cur, fb), body, token);
                } catch (Exception ignored) {
                }
            }
            throw e;
        }
    }

    private Map<String, Object> doPostJson(String url, Map<String, Object> body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        if (token != null && !token.trim().isEmpty()) headers.set("Authorization", "Bearer " + token.trim());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Object b = resp.getBody();
        return b instanceof Map ? (Map<String, Object>) b : new HashMap<>();
    }
}
