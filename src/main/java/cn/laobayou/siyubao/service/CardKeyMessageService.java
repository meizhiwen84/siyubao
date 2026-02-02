package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.CardKey;
import cn.laobayou.siyubao.bean.CardKeyMessage;
import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.repository.CardKeyMessageRepository;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CardKeyMessageService {
    @Autowired
    private CardKeyService cardKeyService;
    @Autowired
    private CardKeyMessageRepository repository;

    public void saveFromSession(HttpSession session, String line, String platform, List<ChatMessage> chatMessageList) {
        Optional<CardKey> opt = cardKeyService.currentSessionCard(session);
        if (!opt.isPresent()) return;
        CardKeyMessage m = new CardKeyMessage();
        m.setCardKey(opt.get().getCode());
        m.setLine(line);
        m.setPlatform(platform);
        m.setChatMessage(JSON.toJSONString(chatMessageList));
        m.setPhone(extractPhoneOrWechat(chatMessageList));
        m.setCreateTime(LocalDateTime.now());
        repository.save(m);
    }

    public Page<CardKeyMessage> search(String cardKey, String line, String platform, String phone, int page, int size) {
        return repository.search(emptyToNull(cardKey), emptyToNull(line), emptyToNull(platform), emptyToNull(phone), PageRequest.of(page, size));
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
}

