package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.service.ChatMessageHistoryService;
import cn.laobayou.siyubao.service.ChatPageData;
import cn.laobayou.siyubao.service.ChatPageService;
import cn.laobayou.siyubao.service.LocalUserSessionService;
import cn.laobayou.siyubao.service.RemoteAdminService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ChatApiController {
    private final ObjectMapper objectMapper;
    private final ChatPageService chatPageService;
    private final ChatMessageHistoryService chatMessageHistoryService;
    private final LocalUserSessionService localUserSessionService;
    private final RemoteAdminService remoteAdminService;

    public ChatApiController(ObjectMapper objectMapper, ChatPageService chatPageService, ChatMessageHistoryService chatMessageHistoryService, LocalUserSessionService localUserSessionService, RemoteAdminService remoteAdminService) {
        this.objectMapper = objectMapper;
        this.chatPageService = chatPageService;
        this.chatMessageHistoryService = chatMessageHistoryService;
        this.localUserSessionService = localUserSessionService;
        this.remoteAdminService = remoteAdminService;
    }

    @PostMapping("/api/chat/regenerate")
    public ResponseEntity<Map<String, Object>> regenerate(@RequestBody JsonNode body) {
        Map<String, Object> resp = new HashMap<>();
        Long userId = localUserSessionService.userId();
        if (userId == null || userId <= 0) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return ResponseEntity.status(401).body(resp);
        }

        try {
            String xianlu = text(body, "xianlu");
            String xianshiname = text(body, "xianshiname");
            String platform = text(body, "platform");
            String userName = text(body, "userName");
            String userAvatar = text(body, "userAvatar");
            String myAvatar = text(body, "myAvatar");
            String topTime = text(body, "topTime");
            Long messageId = longVal(body, "messageId");
            boolean editable = bool(body, "editable");

            List<ChatMessage> msgs = java.util.Collections.emptyList();
            JsonNode n = body == null ? null : body.get("chatMessages");
            if (n != null && !n.isNull()) {
                if (n.isTextual()) {
                    msgs = objectMapper.readValue(n.asText(), new TypeReference<List<ChatMessage>>() {});
                } else if (n.isArray()) {
                    msgs = objectMapper.convertValue(n, new TypeReference<List<ChatMessage>>() {});
                }
            }

            LocalTime now = LocalTime.now(ZoneId.of("Asia/Shanghai"));
            ChatPageData page = chatPageService.buildReGeneratePage(now, userId, xianlu, xianshiname, platform, userAvatar, msgs);
            String token = localUserSessionService.token();
            if (token != null && !token.trim().isEmpty()) {
                try {
                    Map<String, Object> me = remoteAdminService.me(token.trim());
                    Object plan = me == null ? null : me.get("plan");
                    if (plan instanceof Map) {
                        Object wm = ((Map<?, ?>) plan).get("watermark");
                        if (Boolean.TRUE.equals(wm)) page.getModel().put("_sxjwWatermark", "私信截图王");
                    }
                } catch (Exception ignored) {
                }
            }
            if (userName != null && !userName.trim().isEmpty()) {
                page.getModel().put("userName", userName.trim());
            }
            if (myAvatar != null && !myAvatar.trim().isEmpty()) {
                page.getModel().put("myPic", myAvatar.trim());
            }
            if (topTime != null && !topTime.trim().isEmpty()) {
                page.getModel().put("topTime", topTime.trim());
                page.getModel().put("firstDateTimeStr", topTime.trim());
            }

            if (messageId != null) {
                chatMessageHistoryService.updateChatMessage(userId, messageId, msgs, userName, userAvatar);
            }
            String html = chatPageService.renderHtml(page.getTemplate(), page.getModel(), editable);
            resp.put("success", true);
            resp.put("html", html);
            resp.put("template", page.getTemplate());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    private String text(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.get(key);
        return v == null || v.isNull() ? null : v.asText();
    }

    private boolean bool(JsonNode n, String key) {
        if (n == null) return false;
        JsonNode v = n.get(key);
        return v != null && !v.isNull() && v.asBoolean();
    }

    private Long longVal(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.get(key);
        return v == null || v.isNull() ? null : v.asLong();
    }
}
