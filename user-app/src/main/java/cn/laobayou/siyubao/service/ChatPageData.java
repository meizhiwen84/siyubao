package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.ChatMessage;

import java.util.List;
import java.util.Map;

public class ChatPageData {
    private final String template;
    private final Map<String, Object> model;
    private final List<ChatMessage> messages;

    public ChatPageData(String template, Map<String, Object> model, List<ChatMessage> messages) {
        this.template = template;
        this.model = model;
        this.messages = messages;
    }

    public String getTemplate() {
        return template;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }
}
