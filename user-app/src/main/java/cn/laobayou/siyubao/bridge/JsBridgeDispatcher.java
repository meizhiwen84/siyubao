package cn.laobayou.siyubao.bridge;

import cn.laobayou.siyubao.bean.Route;
import cn.laobayou.siyubao.desktop.DesktopRuntime;
import cn.laobayou.siyubao.service.ChatMessageHistoryService;
import cn.laobayou.siyubao.service.ChatPageData;
import cn.laobayou.siyubao.service.ChatPageService;
import cn.laobayou.siyubao.service.LocalDeviceService;
import cn.laobayou.siyubao.service.LocalUserSessionService;
import cn.laobayou.siyubao.service.OcrService;
import cn.laobayou.siyubao.service.RemoteAdminService;
import cn.laobayou.siyubao.service.RouteService;
import cn.laobayou.siyubao.service.UploadService;
import cn.laobayou.siyubao.util.ByteArrayMultipartFile;
import cn.laobayou.siyubao.bean.ChatMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsBridgeDispatcher {
    private final ObjectMapper objectMapper;
    private final RouteService routeService;
    private final OcrService ocrService;
    private final ChatPageService chatPageService;
    private final ChatMessageHistoryService chatMessageHistoryService;
    private final UploadService uploadService;
    private final LocalUserSessionService localUserSessionService;
    private final LocalDeviceService localDeviceService;
    private final RemoteAdminService remoteAdminService;

    public JsBridgeDispatcher(
            ObjectMapper objectMapper,
            RouteService routeService,
            OcrService ocrService,
            ChatPageService chatPageService,
            ChatMessageHistoryService chatMessageHistoryService,
            UploadService uploadService,
            LocalUserSessionService localUserSessionService,
            LocalDeviceService localDeviceService,
            RemoteAdminService remoteAdminService
    ) {
        this.objectMapper = objectMapper;
        this.routeService = routeService;
        this.ocrService = ocrService;
        this.chatPageService = chatPageService;
        this.chatMessageHistoryService = chatMessageHistoryService;
        this.uploadService = uploadService;
        this.localUserSessionService = localUserSessionService;
        this.localDeviceService = localDeviceService;
        this.remoteAdminService = remoteAdminService;
    }

    public String dispatchToJson(String requestJson) {
        try {
            BridgeRequest req = objectMapper.readValue(requestJson, BridgeRequest.class);
            Map<String, Object> resp = dispatch(req);
            return objectMapper.writeValueAsString(resp);
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(fail(e.getMessage()));
            } catch (Exception ignored) {
                return "{\"success\":false,\"message\":\"bridge error\"}";
            }
        }
    }

    public Map<String, Object> dispatch(BridgeRequest req) {
        if (req == null || req.getMethod() == null) {
            return fail("missing method");
        }
        String method = req.getMethod();
        JsonNode params = req.getParams();

        if ("auth.me".equals(method)) {
            try {
                String token = requireToken();
                return remoteAdminService.me(token);
            } catch (Exception e) {
                return fail(e.getMessage());
            }
        }

        if ("auth.logout".equals(method)) {
            try {
                localUserSessionService.clear();
                DesktopRuntime.setCardCode(null);
                return ok(null);
            } catch (Exception e) {
                return fail(e.getMessage());
            }
        }

        if (method.startsWith("route.")) {
            Long userId = requireUserId();
            switch (method) {
                case "route.list": {
                    List<Route> routes = routeService.getAllRoutes(userId);
                    return ok(routes);
                }
                case "route.create": {
                    String routeName = text(params, "routeName");
                    String routeValue = text(params, "routeValue");
                    Route route = routeService.createRoute(userId, routeName, routeValue);
                    return ok(route);
                }
                case "route.update": {
                    Long id = longVal(params, "id");
                    String routeName = text(params, "routeName");
                    String routeValue = text(params, "routeValue");
                    Route route = routeService.updateRoute(userId, id, routeName, routeValue);
                    return ok(route);
                }
                case "route.updateStatus": {
                    Long id = longVal(params, "id");
                    Boolean status = boolVal(params, "status");
                    Route route = routeService.updateRouteStatus(userId, id, status);
                    return ok(route);
                }
                case "route.updateWelcomeMessage": {
                    Long id = longVal(params, "id");
                    String welcomeMessage = text(params, "welcomeMessage");
                    Route route = routeService.updateWelcomeMessage(userId, id, welcomeMessage);
                    return ok(route);
                }
                case "route.delete": {
                    Long id = longVal(params, "id");
                    routeService.deleteRoute(userId, id);
                    return ok(null);
                }
                case "route.uploadAvatar": {
                    Long id = longVal(params, "id");
                    String platform = text(params, "platform");
                    String filename = text(params, "filename");
                    String contentBase64 = text(params, "contentBase64");
                    try {
                        byte[] bytes = contentBase64 == null ? null : Base64.getDecoder().decode(contentBase64);
                        String avatarUrl = routeService.uploadAvatarBytes(bytes, filename, platform);
                        Route route = routeService.updateRouteAvatar(userId, id, platform, avatarUrl);
                        Map<String, Object> resp = ok(route);
                        resp.put("avatarPath", avatarUrl);
                        return resp;
                    } catch (Exception e) {
                        return fail(e.getMessage());
                    }
                }
                case "route.clearAvatar": {
                    Long id = longVal(params, "id");
                    String platform = text(params, "platform");
                    Route route = routeService.clearRouteAvatar(userId, id, platform);
                    return ok(route);
                }
                default:
                    return fail("unknown method: " + method);
            }
        }

        if ("ocr.perform".equals(method)) {
            try {
                String filename = text(req.getParams(), "filename");
                String contentType = text(req.getParams(), "contentType");
                String contentBase64 = text(req.getParams(), "contentBase64");
                byte[] bytes = contentBase64 == null ? null : Base64.getDecoder().decode(contentBase64);
                ByteArrayMultipartFile f = new ByteArrayMultipartFile("image", filename == null ? "image.png" : filename, contentType, bytes);
                String recognizedText = ocrService.recognizeText(f);
                Map<String, Object> r = new HashMap<>();
                if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                    r.put("success", true);
                    r.put("text", recognizedText.trim());
                    r.put("message", "识别成功");
                } else {
                    r.put("success", false);
                    r.put("text", "");
                    r.put("message", "未识别到文字内容");
                }
                return r;
            } catch (Exception e) {
                return fail("识别失败: " + (e.getMessage() == null ? "error" : e.getMessage()));
            }
        }

        if ("upload.image".equals(method)) {
            Long userId = requireUserId();
            try {
                String filename = text(req.getParams(), "filename");
                String contentBase64 = text(req.getParams(), "contentBase64");
                byte[] bytes = contentBase64 == null ? null : Base64.getDecoder().decode(contentBase64);
                String url = uploadService.saveImageBytes(userId, filename, bytes);
                Map<String, Object> r = ok(null);
                r.put("url", url);
                return r;
            } catch (Exception e) {
                return fail(e.getMessage());
            }
        }

        if ("chat.generate".equals(method)) {
            Long userId = requireUserId();
            String token = requireToken();
            try {
                String xianlu = text(params, "xianlu");
                String xianshiname = text(params, "xianshiname");
                String chatContent = text(params, "chatContent");
                String platform = text(params, "platform");
                Boolean editable = boolVal(params, "editable");
                java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Shanghai"));
                Map<String, Object> consume = remoteAdminService.consumeGenerate(token);
                if (consume == null || !Boolean.TRUE.equals(consume.get("success"))) {
                    String msg = consume == null ? null : String.valueOf(consume.get("message"));
                    return fail(msg == null || msg.trim().isEmpty() ? "今日生成次数已用完" : msg);
                }
                ChatPageData page = chatPageService.buildPage(now, userId, xianlu, xianshiname, chatContent, platform);
                if (Boolean.TRUE.equals(consume.get("watermark"))) {
                    page.getModel().put("_sxjwWatermark", "私信截图王");
                }
                Long messageId = chatMessageHistoryService.saveAndReturnId(userId, xianlu, platform, page.getMessages());
                String html = chatPageService.renderHtml(page.getTemplate(), page.getModel(), Boolean.TRUE.equals(editable));
                Map<String, Object> r = ok(null);
                r.put("html", html);
                r.put("template", page.getTemplate());
                if (messageId != null) r.put("messageId", messageId);
                if (consume != null) {
                    if (consume.get("watermark") != null) r.put("watermark", consume.get("watermark"));
                    if (consume.get("todayUsed") != null) r.put("todayUsed", consume.get("todayUsed"));
                    if (consume.get("dailyLimit") != null) r.put("dailyLimit", consume.get("dailyLimit"));
                }
                return r;
            } catch (Exception e) {
                return fail(e.getMessage());
            }
        }

        if ("chat.regenerate".equals(method)) {
            Long userId = requireUserId();
            try {
                String token = localUserSessionService.token();
                String xianlu = text(params, "xianlu");
                String xianshiname = text(params, "xianshiname");
                String platform = text(params, "platform");
                String userName = text(params, "userName");
                String userAvatar = text(params, "userAvatar");
                String myAvatar = text(params, "myAvatar");
                String topTime = text(params, "topTime");
                Long messageId = longVal(params, "messageId");
                Boolean editable = boolVal(params, "editable");

                java.util.List<ChatMessage> msgs = java.util.Collections.emptyList();
                JsonNode n = params == null ? null : params.get("chatMessages");
                if (n != null && !n.isNull()) {
                    if (n.isTextual()) {
                        msgs = objectMapper.readValue(n.asText(), new TypeReference<java.util.List<ChatMessage>>() {});
                    } else if (n.isArray()) {
                        msgs = objectMapper.convertValue(n, new TypeReference<java.util.List<ChatMessage>>() {});
                    }
                }

                java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Shanghai"));
                ChatPageData page = chatPageService.buildReGeneratePage(now, userId, xianlu, xianshiname, platform, userAvatar, msgs);
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
                String html = chatPageService.renderHtml(page.getTemplate(), page.getModel(), Boolean.TRUE.equals(editable));
                Map<String, Object> r = ok(null);
                r.put("html", html);
                r.put("template", page.getTemplate());
                return r;
            } catch (Exception e) {
                return fail(e.getMessage());
            }
        }

        if ("history.list".equals(method)) {
            Long userId = requireUserId();
            try {
                String line = text(params, "line");
                String platform = text(params, "platform");
                String phone = text(params, "phone");
                Integer page = intVal(params, "page");
                Integer size = intVal(params, "size");
                int p = page == null ? 0 : Math.max(page, 0);
                int s = size == null ? 10 : Math.min(Math.max(size, 1), 100);
                Page<cn.laobayou.siyubao.bean.ChatMessageHistory> r = chatMessageHistoryService.search(userId, line, platform, phone, p, s);
                Map<String, Object> resp = ok(r.getContent());
                resp.put("total", r.getTotalElements());
                resp.put("pages", r.getTotalPages());
                resp.put("page", p);
                resp.put("size", s);
                return resp;
            } catch (Exception e) {
                return fail(e.getMessage());
            }
        }

        return fail("unknown method: " + method);
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        if (data != null) m.put("data", data);
        return m;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("message", message == null ? "error" : message);
        return m;
    }

    private Long requireUserId() {
        Long uid = localUserSessionService.userId();
        if (uid == null || uid <= 0) throw new RuntimeException("未登录");
        return uid;
    }

    private String requireToken() {
        String token = localUserSessionService.token();
        if (token == null || token.trim().isEmpty()) throw new RuntimeException("未登录");
        return token.trim();
    }

    private String text(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.get(key);
        return v == null || v.isNull() ? null : v.asText();
    }

    private Long longVal(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.get(key);
        return v == null || v.isNull() ? null : v.asLong();
    }

    private Boolean boolVal(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.get(key);
        return v == null || v.isNull() ? null : v.asBoolean();
    }

    private Integer intVal(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.get(key);
        return v == null || v.isNull() ? null : v.asInt();
    }
}
