package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.bean.ChatMessageHistory;
import cn.laobayou.siyubao.repository.ChatMessageHistoryRepository;
import com.alibaba.fastjson.JSON;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatMessageHistoryService {
    private final ChatMessageHistoryRepository repository;

    public ChatMessageHistoryService(ChatMessageHistoryRepository repository) {
        this.repository = repository;
    }

    public void save(Long userId, String line, String platform, List<ChatMessage> chatMessageList) {
        saveAndReturnId(userId, line, platform, chatMessageList);
    }

    public Long saveAndReturnId(Long userId, String line, String platform, List<ChatMessage> chatMessageList) {
        if (userId == null || userId <= 0) return null;
        ChatMessageHistory m = new ChatMessageHistory();
        m.setUserId(userId);
        m.setLine(line);
        m.setPlatform(platform);
        m.setChatMessage(JSON.toJSONString(chatMessageList));
        m.setPhone(extractPhoneOrWechat(chatMessageList));
        m.setUserName(extractUserName(chatMessageList));
        m.setUserPic(extractUserPic(chatMessageList));
        m.setCreateTime(LocalDateTime.now());
        ChatMessageHistory saved = repository.save(m);
        return saved == null ? null : saved.getId();
    }

    public Page<ChatMessageHistory> search(Long userId, String line, String platform, String phone, int page, int size) {
        return repository.search(userId, emptyToNull(line), emptyToNull(platform), emptyToNull(phone), PageRequest.of(page, size));
    }

    public void updateChatMessage(Long userId, Long id, List<ChatMessage> chatMessageList, String userName, String userPic) {
        if (userId == null || userId <= 0) throw new RuntimeException("未登录");
        if (id == null) throw new RuntimeException("缺少ID");
        ChatMessageHistory m = repository.findByIdAndUserId(id, userId).orElseThrow(() -> new RuntimeException("记录不存在"));
        m.setChatMessage(JSON.toJSONString(chatMessageList == null ? java.util.Collections.emptyList() : chatMessageList));
        m.setPhone(extractPhoneOrWechat(chatMessageList));
        if (userName != null && !userName.trim().isEmpty()) m.setUserName(userName.trim());
        else m.setUserName(extractUserName(chatMessageList));
        if (userPic != null && !userPic.trim().isEmpty()) m.setUserPic(userPic.trim());
        else m.setUserPic(extractUserPic(chatMessageList));
        repository.save(m);
    }

    private String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String extractPhoneOrWechat(List<ChatMessage> list) {
        if (list == null) return null;
        Pattern phonePattern = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");
        Pattern wxPattern = Pattern.compile("微信号[:：]([a-zA-Z][a-zA-Z0-9_-]{4,19})");
        for (ChatMessage cm : list) {
            if (cm == null || cm.getMsg() == null) continue;
            String msg = cm.getMsg();
            Matcher pm = phonePattern.matcher(msg);
            if (pm.find()) return pm.group(1);
            if (msg.contains("微信号")) {
                Matcher wm = wxPattern.matcher(msg);
                if (wm.find()) return wm.group(1);
            }
        }
        return null;
    }

    private String extractUserName(List<ChatMessage> list) {
        if (list == null) return null;
        for (ChatMessage cm : list) {
            if (cm == null) continue;
            String un = cm.getUserName();
            if (un != null) {
                String t = un.trim();
                if (!t.isEmpty()) return t;
            }
        }
        return null;
    }

    private String extractUserPic(List<ChatMessage> list) {
        if (list == null) return null;
        for (ChatMessage cm : list) {
            if (cm == null) continue;
            String up = cm.getUserPic();
            if (up != null) {
                String t = up.trim();
                if (!t.isEmpty()) return t;
            }
        }
        return null;
    }
}
