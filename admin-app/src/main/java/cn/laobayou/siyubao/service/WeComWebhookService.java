package cn.laobayou.siyubao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WeComWebhookService {
    private final AppSettingService appSettingService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WeComWebhookService(AppSettingService appSettingService) {
        this.appSettingService = appSettingService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public void sendPaymentNotification(String username, String planName, String amount, String platform, String transactionId, String orderNo, String approveUrl) {
        sendPaymentNotification(username, planName, amount, platform, transactionId, orderNo, approveUrl, null, null);
    }

    public void sendPaymentNotification(String username, String planName, String amount, String platform, String transactionId, String orderNo, String approveUrl, String qrCodeName, String qrCodeUrl) {
        String webhookUrl = appSettingService.getString("wecom_webhook_url", "");
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return;
        }

        try {
            Map<String, Object> message = buildPaymentMessage(username, planName, amount, platform, transactionId, orderNo, approveUrl, qrCodeName, qrCodeUrl);
            String json = objectMapper.writeValueAsString(message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> buildPaymentMessage(String username, String planName, String amount, String platform, String transactionId, String orderNo, String approveUrl, String qrCodeName, String qrCodeUrl) {
        Map<String, Object> message = new HashMap<>();
        message.put("msgtype", "markdown");

        Map<String, Object> markdown = new HashMap<>();
        StringBuilder content = new StringBuilder();
        content.append("## 🔔 新支付订单通知\n\n");
        content.append("> **用户**：").append(username).append("\n\n");
        content.append("> **套餐**：").append(planName).append("\n\n");
        content.append("> **金额**：").append(amount).append("\n\n");
        content.append("> **平台**：").append(platform).append("\n\n");
        content.append("> **交易号**：").append(transactionId).append("\n\n");
        content.append("> **订单号**：").append(orderNo).append("\n\n");
        
        if (qrCodeName != null && !qrCodeName.trim().isEmpty()) {
            content.append("> **收款码**：").append(qrCodeName).append("\n\n");
        }
        if (qrCodeUrl != null && !qrCodeUrl.trim().isEmpty()) {
            content.append("> [查看收款码](").append(qrCodeUrl).append(")\n\n");
        }
        
        content.append("\n---\n\n");
        content.append("[👉 立即处理](").append(approveUrl).append(")");
        
        markdown.put("content", content.toString());
        message.put("markdown", markdown);

        return message;
    }
}
