package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.bean.Route;
import cn.laobayou.siyubao.bean.XianluEnum;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;

@Service
public class ChatPageService {
    private static final String TOOL_TITLE = "云客";
    private static final String WELCOME_TEMPLATE = "你好，欢迎来xianlu旅游！ 目前xianlu旅游限时特惠优惠多多，您这边大概几个人，什么时候出行呢？可以留个联系方式，给你发行程报价参考下！";

    private final UserStant userStant;
    private final RouteService routeService;
    private final SpringTemplateEngine templateEngine;

    public ChatPageService(UserStant userStant, RouteService routeService, SpringTemplateEngine templateEngine) {
        this.userStant = userStant;
        this.routeService = routeService;
        this.templateEngine = templateEngine;
    }

    public ChatPageData buildPage(LocalTime now, Long userId, String xianlu, String xianshiname, String chatContent, String platform, String chatBg) throws IOException {
        String pf = StringUtils.isBlank(platform) ? "dy" : platform.trim();
        String dynamicAvatar = normalizeStaticUrl(getDynamicRouteAvatar(userId, xianlu, pf));
        Map<String, String> xianluNameAndPic = userStant.getXianluNameAndPic(xianlu, pf);

        String userPic = normalizeStaticUrl(userStant.getRandomUserPic());
        List<ChatMessage> chatMessageList = generateChatMessage(now, xianlu, chatContent, pf, userPic);

        Map<String, Object> model = new HashMap<>();
        model.put("xianlu", xianlu);
        model.put("platform", pf);
        model.put("title", pf + "-" + xianlu + "-dy截图生成聊天");
        model.put("message", pf + TOOL_TITLE);
        model.put("myPic", dynamicAvatar);
        model.put("myName", (xianshiname != null && xianshiname.equals("true")) ? xianluNameAndPic.get("xianluName") : "");
        model.put("userPic", userPic);
        model.put("userName", chatMessageList.get(0).getUserName());
        model.put("msgList", chatMessageList);
        model.put("firstDateTimeStr", chatMessageList.get(0).getDateTimeStr());
        model.put("topTime", chatMessageList.get(0).getDateTimeStr());
        model.put("chatBg", chatBg);

        String welcomword = routeService.getWelcomeMessageByRouteValue(xianlu, userId);
        if (StringUtils.isBlank(welcomword)) {
            welcomword = xianluNameAndPic.get("welcomword");
        }
        model.put("welcomword", welcomword);

        return new ChatPageData(templateByPlatform(pf), model, chatMessageList);
    }

    public ChatPageData buildReGeneratePage(LocalTime now, Long userId, String xianlu, String xianshiname, String platform, String userAvatar, List<ChatMessage> rawMessages, String chatBg) {
        String pf = StringUtils.isBlank(platform) ? "dy" : platform.trim();

        String dynamicAvatar = normalizeStaticUrl(getDynamicRouteAvatar(userId, xianlu, pf));
        Map<String, String> xianluNameAndPic = userStant.getXianluNameAndPic(xianlu, pf);

        List<ChatMessage> chatMessageList = new ArrayList<>();
        if (rawMessages != null) {
            for (ChatMessage cm : rawMessages) {
                if (cm == null || cm.getMsg() == null || cm.getMsg().trim().isEmpty()) continue;
                chatMessageList.add(cm);
            }
        }

        if (chatMessageList.isEmpty()) {
            ChatMessage m0 = new ChatMessage();
            m0.setMsgType(1);
            m0.setUserName("");
            m0.setMsg("（无聊天内容）");
            m0.setDateTimeStr(userStant.getTimeStr(now.getHour()) + ":" + userStant.getTimeStr(now.getMinute()));
            chatMessageList.add(m0);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("xianlu", xianlu);
        model.put("platform", pf);
        model.put("title", xianlu + "-dy截图生成聊天");
        model.put("message", TOOL_TITLE);
        model.put("myPic", dynamicAvatar);
        model.put("myName", (xianshiname != null && xianshiname.equals("true")) ? xianluNameAndPic.get("xianluName") : "");
        model.put("userPic", normalizeStaticUrl(userAvatar));
        model.put("chatBg", chatBg);

        String welcomword = routeService.getWelcomeMessageByRouteValue(xianlu, userId);
        if (StringUtils.isBlank(welcomword)) {
            welcomword = xianluNameAndPic.get("welcomword");
        }
        model.put("welcomword", welcomword);

        String userName = "";
        for (ChatMessage cm : chatMessageList) {
            if (cm != null && cm.getUserName() != null && !cm.getUserName().trim().isEmpty()) {
                userName = cm.getUserName().trim();
                break;
            }
        }
        model.put("userName", userName);
        model.put("msgList", chatMessageList);
        model.put("firstDateTimeStr", chatMessageList.get(0).getDateTimeStr());
        model.put("topTime", chatMessageList.get(0).getDateTimeStr());

        String template;
        if ("xhs".equals(pf)) template = "chat-interface-v4.html";
        else if ("sph".equals(pf)) template = "wechat-mobile-chat.html";
        else if ("dy".equals(pf)) template = "douyin_mobile_chat.html";
        else template = "siyubao_cq";

        return new ChatPageData(template, model, chatMessageList);
    }

    public String renderHtml(String templateName, Map<String, Object> model) {
        return renderHtml(templateName, model, false);
    }

    public String renderHtml(String templateName, Map<String, Object> model, boolean editable) {
        Context ctx = new Context(Locale.SIMPLIFIED_CHINESE);
        ctx.setVariables(model);
        String name = templateName;
        if (name != null && name.endsWith(".html")) {
            name = name.substring(0, name.length() - 5);
        }
        String html = templateEngine.process(name, ctx);

        if (editable) {
            html = injectEditableMeta(html, model);
            html = injectEditableScript(html);
        }
        html = injectWatermark(html, model);

        int metaCharsetIdx = indexOfIgnoreCase(html, "<meta charset");
        if (metaCharsetIdx >= 0) {
            int tagEnd = html.indexOf(">", metaCharsetIdx);
            if (tagEnd >= 0) {
                int insertPos = tagEnd + 1;
                html = html.substring(0, insertPos) + "<base href=\"/\">" + html.substring(insertPos);
                return html;
            }
        }

        int headIdx = indexOfIgnoreCase(html, "<head>");
        if (headIdx >= 0) {
            int insertPos = headIdx + "<head>".length();
            html = html.substring(0, insertPos) + "<base href=\"/\">" + html.substring(insertPos);
        }
        return html;
    }

    private String injectWatermark(String html, Map<String, Object> model) {
        if (html == null) return null;
        if (model == null) return html;
        Object v = model.get("_sxjwWatermark");
        if (v == null) return html;
        String text = String.valueOf(v).trim();
        if (text.isEmpty()) return html;
        if (html.contains("id=\"sxjw-watermark\"")) return html;

        String dataUrl = buildWatermarkSvgDataUrl(text);
        String css =
                "<style id=\"sxjw-watermark-style\">" +
                        "#sxjw-watermark{position:fixed;inset:0;z-index:999998;pointer-events:none;opacity:1;" +
                        "background-image:url('" + dataUrl + "');background-repeat:repeat;}" +
                        "</style>";
        int headEnd = indexOfIgnoreCase(html, "</head>");
        if (headEnd >= 0) {
            html = html.substring(0, headEnd) + css + html.substring(headEnd);
        } else {
            html = css + html;
        }
        int bodyIdx = indexOfIgnoreCase(html, "<body");
        if (bodyIdx >= 0) {
            int bodyTagEnd = html.indexOf(">", bodyIdx);
            if (bodyTagEnd >= 0) {
                int insertPos = bodyTagEnd + 1;
                html = html.substring(0, insertPos) + "<div id=\"sxjw-watermark\"></div>" + html.substring(insertPos);
            }
        } else {
            html = "<div id=\"sxjw-watermark\"></div>" + html;
        }
        return html;
    }

    private String buildWatermarkSvgDataUrl(String text) {
        String t = escapeSvgText(text);
        String svg =
                "<svg xmlns='http://www.w3.org/2000/svg' width='320' height='220'>" +
                        "<g transform='translate(10,150) rotate(-20)'>" +
                        "<text x='0' y='0' font-size='24' fill='rgba(0,0,0,0.08)' font-family='Arial,Microsoft YaHei,sans-serif'>" +
                        t +
                        "</text>" +
                        "</g>" +
                        "</svg>";
        String encoded = svg
                .replace("%", "%25")
                .replace("#", "%23")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "%20")
                .replace("\"", "%22")
                .replace("<", "%3C")
                .replace(">", "%3E");
        return "data:image/svg+xml," + encoded;
    }

    private String escapeSvgText(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&apos;").replace("\"", "&quot;");
    }

    private String injectEditableMeta(String html, Map<String, Object> model) {
        int headIdx = indexOfIgnoreCase(html, "<head>");
        if (headIdx < 0) return html;
        int insertPos = headIdx + "<head>".length();
        StringBuilder meta = new StringBuilder();
        meta.append("<meta name=\"siyubao-editable\" content=\"1\">");
        Object xianlu = model.get("xianlu");
        Object platform = model.get("platform");
        Object userPic = model.get("userPic");
        Object myPic = model.get("myPic");
        Object userName = model.get("userName");
        if (xianlu != null) meta.append("<meta name=\"siyubao-xianlu\" content=\"").append(escapeHtmlAttr(String.valueOf(xianlu))).append("\">");
        if (platform != null) meta.append("<meta name=\"siyubao-platform\" content=\"").append(escapeHtmlAttr(String.valueOf(platform))).append("\">");
        if (userPic != null) meta.append("<meta name=\"siyubao-user-pic\" content=\"").append(escapeHtmlAttr(String.valueOf(userPic))).append("\">");
        if (myPic != null) meta.append("<meta name=\"siyubao-my-pic\" content=\"").append(escapeHtmlAttr(String.valueOf(myPic))).append("\">");
        if (userName != null) meta.append("<meta name=\"siyubao-user-name\" content=\"").append(escapeHtmlAttr(String.valueOf(userName))).append("\">");
        return html.substring(0, insertPos) + meta + html.substring(insertPos);
    }

    private String injectEditableScript(String html) {
        String script =
                "<style>" +
                        "[data-siyubao-idx]{cursor:text}" +
                        ".siyubao-editing{outline:2px solid #3b82f6;outline-offset:2px}" +
                        ".siyubao-toolbar{position:fixed;left:12px;right:12px;bottom:12px;z-index:999999;display:flex;gap:8px;justify-content:center;pointer-events:none}" +
                        ".siyubao-toolbar button{pointer-events:auto;background:#111827;color:#fff;border:0;border-radius:8px;padding:8px 10px;font-size:12px}" +
                        ".siyubao-toolbar button.secondary{background:#374151}" +
                        ".siyubao-menu{position:fixed;left:50%;top:50%;transform:translate(-50%,-50%);z-index:1000000;background:#fff;border:1px solid #e5e7eb;border-radius:12px;padding:10px;min-width:220px;box-shadow:0 10px 30px rgba(0,0,0,.15)}" +
                        ".siyubao-menu h3{margin:0 0 10px;font-size:13px;color:#111827}" +
                        ".siyubao-menu .row{display:flex;gap:8px;flex-wrap:wrap}" +
                        ".siyubao-menu button{background:#111827;color:#fff;border:0;border-radius:8px;padding:8px 10px;font-size:12px}" +
                        ".siyubao-menu button.secondary{background:#374151}" +
                        ".siyubao-backdrop{position:fixed;inset:0;background:rgba(0,0,0,.35);z-index:999999}" +
                        ".message-time{margin:8px 0;text-align:center;font-size:12px;color:#9ca3af}" +
                        "[data-siyubao-wrap]{margin-top:10px}" +
                        ".message-time+[data-siyubao-wrap]{margin-top:6px}" +
                        ".siyubao-chat-img{max-width:120px;max-height:160px;object-fit:cover;border-radius:6px;display:block}" +
                        "[data-siyubao-content=\"2\"]{padding:0!important;background:transparent!important;box-shadow:none!important;border:0!important}" +
                        "</style>" +
                        "<script>(function(){" +
                        "var meta=document.querySelector('meta[name=\"siyubao-editable\"]');" +
                        "if(!meta){return;}" +
                        "function metaVal(n){var m=document.querySelector('meta[name=\"'+n+'\"]');return m?m.getAttribute('content')||'':'';}" +
                        "function setMeta(n,v){var m=document.querySelector('meta[name=\"'+n+'\"]');if(!m){m=document.createElement('meta');m.setAttribute('name',n);document.head.appendChild(m);}m.setAttribute('content',v||'');}" +
                        "function topTime(){var t=document.querySelector('[data-siyubao-top-time]');return t?(t.innerText||'').trim():'';}" +
                        "var selected=null;" +
                        "var toolbar=null;" +
                        "function updateReadMark(){" +
                        "var wraps=[].slice.call(document.querySelectorAll('[data-siyubao-wrap]'));" +
                        "var readMark=document.querySelector('[data-siyubao-read]');" +
                        "if(!readMark) return;" +
                        "var lastMsgType=1;" +
                        "for(var i=wraps.length-1;i>=0;i--){" +
                        "var w=wraps[i];" +
                        "var n=w.querySelector('[data-siyubao-idx]'); if(!n) continue;" +
                        "var isIgnored=w.getAttribute&&w.getAttribute('data-siyubao-ignore')!=null;" +
                        "var msgType=parseInt(n.getAttribute('data-siyubao-type'))||1;" +
                        "if(!isIgnored){" +
                        "lastMsgType=msgType;" +
                        "break;" +
                        "}" +
                        "if(lastMsgType===1){" +
                        "lastMsgType=msgType;" +
                        "}" +
                        "}" +
                        "readMark.style.display=(lastMsgType===2?'flex':'none');" +
                        "}" +
                        "function ensureToolbar(){" +
                        "if(toolbar) return toolbar;" +
                        "toolbar=document.createElement('div');toolbar.className='siyubao-toolbar';" +
                        "toolbar.innerHTML=" +
                        "'<button data-act=\"insert-above\">上方插入</button>'+" +
                        "'<button data-act=\"insert-below\">下方插入</button>'+" +
                        "'<button class=\"secondary\" data-act=\"toggle-type\">切换我/对方</button>'+" +
                        "'<button class=\"secondary\" data-act=\"delete\">删除</button>';"+
                        "document.body.appendChild(toolbar);" +
                        "toolbar.addEventListener('click',function(e){" +
                        "var b=e.target&&e.target.closest?e.target.closest('button'):null; if(!b) return;" +
                        "var act=b.getAttribute('data-act'); if(!act) return;" +
                        "if(!selected) return;" +
                        "if(act==='insert-above'){openInsertMenu('above');}" +
                        "else if(act==='insert-below'){openInsertMenu('below');}" +
                        "else if(act==='delete'){deleteSelected();}" +
                        "else if(act==='toggle-type'){toggleType();}" +
                        "},true);" +
                        "return toolbar;" +
                        "}" +
                        "function setSelected(el){" +
                        "if(selected&&selected.classList) selected.classList.remove('siyubao-selected');" +
                        "selected=el;" +
                        "if(selected&&selected.classList) selected.classList.add('siyubao-selected');" +
                        "if(selected){ensureToolbar();}" +
                        "}" +
                        "function nearestMessageWrap(el){" +
                        "var w=el;while(w&&w.nodeType===1){" +
                        "if(w.getAttribute&&w.getAttribute('data-siyubao-wrap')!=null) return w;" +
                        "if(w.classList&&w.classList.contains('message')) return w;" +
                        "w=w.parentNode;" +
                        "}return null;" +
                        "}" +
                        "function timeNodeForWrap(wrap){" +
                        "if(!wrap||!wrap.previousElementSibling) return null;" +
                        "var p=wrap.previousElementSibling;" +
                        "if(p.classList&&p.classList.contains('message-time')&&p.getAttribute('data-siyubao-time-for')!=null) return p;" +
                        "return null;" +
                        "}" +
                        "function collect(){" +
                        "var wraps=[].slice.call(document.querySelectorAll('[data-siyubao-wrap]'));" +
                        "if(!wraps||wraps.length===0){wraps=[].slice.call(document.querySelectorAll('.chat-container .message'));}" +
                        "var res=[];" +
                        "wraps.forEach(function(w,idx){" +
                        "if(w.getAttribute&&w.getAttribute('data-siyubao-ignore')!=null) return;" +
                        "var n=w.querySelector('[data-siyubao-idx]'); if(!n) return;" +
                        "n.setAttribute('data-siyubao-idx',''+idx);" +
                        "var tn=timeNodeForWrap(w);" +
                        "var img=n.querySelector('img');" +
                        "var ct=parseInt(n.getAttribute('data-siyubao-content'))|| (img?2:1);" +
                        "var msg=img?(img.getAttribute('src')||''):(n.innerText||'');" +
                        "res.push({idx:idx,msg:(msg||'').replace(/\\r\\n/g,'\\n').trim(),contentType:ct,msgType:parseInt(n.getAttribute('data-siyubao-type'))||1,dateTimeStr:(tn?(tn.innerText||'').trim():(n.getAttribute('data-siyubao-time')||'')),showTime:!!tn});" +
                        "});" +
                        "return res;" +
                        "}" +
                        "function getReadText(){" +
                        "var readEl=document.querySelector('[data-siyubao-read-text]');" +
                        "return readEl?readEl.innerText||'已读':'已读';" +
                        "}" +
                        "function send(){" +
                        "try{parent.postMessage({type:'siyubao-edit',messages:collect(),topTime:topTime(),xianlu:metaVal('siyubao-xianlu'),platform:metaVal('siyubao-platform'),userAvatar:metaVal('siyubao-user-pic'),myAvatar:metaVal('siyubao-my-pic'),userName:metaVal('siyubao-user-name'),readText:getReadText()},'*');}catch(e){}" +
                        "}" +
                        "function requestUpload(p){try{parent.postMessage(Object.assign({type:'siyubao-request-upload'},p||{}),'*');}catch(e){}}" +
                        "window.addEventListener('message',function(evt){" +
                        "var d=evt&&evt.data; if(!d) return;" +
                        "if(d.type==='siyubao-clear-edit'){" +
                        "if(selected&&selected.classList) selected.classList.remove('siyubao-selected');" +
                        "selected=null;" +
                        "if(toolbar&&toolbar.parentNode) toolbar.parentNode.removeChild(toolbar);" +
                        "toolbar=null;" +
                        "return;" +
                        "}" +
                        "if(d.type!=='siyubao-uploaded') return;" +
                        "var kind=d.kind||'';" +
                        "if(kind==='avatar-user'){" +
                        "var u=d.url||''; if(u){setMeta('siyubao-user-pic',u);var imgs=document.querySelectorAll('img[data-siyubao-avatar=\"user\"]');for(var i=0;i<imgs.length;i++){imgs[i].setAttribute('src',u);} send();}" +
                        "}else if(kind==='avatar-me'){" +
                        "var u2=d.url||''; if(u2){setMeta('siyubao-my-pic',u2);var imgs2=document.querySelectorAll('img[data-siyubao-avatar=\"me\"]');for(var j=0;j<imgs2.length;j++){imgs2[j].setAttribute('src',u2);} send();}" +
                        "}else if(kind==='replace-image'){" +
                        "var u3=d.url||''; if(!u3) return; if(selected){var im=selected.querySelector('img'); if(im){im.setAttribute('src',u3); send(); return;}}" +
                        "}else if(kind==='insert-image'){" +
                        "var u4=d.url||''; if(!u4) return; var w=d.where||'below'; var mt=parseInt(d.msgType)||1; insertMessage(w,mt,2); if(selected){var im2=selected.querySelector('img'); if(im2){im2.setAttribute('src',u4);} } send();" +
                        "}" +
                        "},true);" +
                        "function enableEdit(el){" +
                        "if(!el||!el.setAttribute)return;" +
                        "el.setAttribute('contenteditable','true');" +
                        "if(el.classList) el.classList.add('siyubao-editing');" +
                        "setTimeout(function(){try{el.focus();}catch(_){}} ,0);" +
                        "}" +
                        "function closeMenu(){" +
                        "var mm=window.__siyubao_menu; if(!mm) return;" +
                        "try{mm.bd&&mm.bd.parentNode&&mm.bd.parentNode.removeChild(mm.bd);}catch(_){}" +
                        "try{mm.m&&mm.m.parentNode&&mm.m.parentNode.removeChild(mm.m);}catch(_){}" +
                        "window.__siyubao_menu=null;" +
                        "}" +
                        "function openInsertMenu(where){" +
                        "closeMenu();" +
                        "var bd=document.createElement('div');bd.className='siyubao-backdrop';bd.addEventListener('click',closeMenu,true);" +
                        "var m=document.createElement('div');m.className='siyubao-menu';" +
                        "m.innerHTML='<h3>插入类型</h3><div class=\"row\">'+" +
                        "'<button data-t=\"recv-text\">对方文字</button>'+" +
                        "'<button data-t=\"send-text\">我方文字</button>'+" +
                        "'<button data-t=\"recv-img\">对方图片</button>'+" +
                        "'<button data-t=\"send-img\">我方图片</button>'+" +
                        "'<button class=\"secondary\" data-t=\"cancel\">取消</button>'+" +
                        "'</div>';"+
                        "document.body.appendChild(bd);document.body.appendChild(m);" +
                        "m.addEventListener('click',function(e){" +
                        "var b=e.target&&e.target.closest?e.target.closest('button'):null; if(!b) return;" +
                        "var t=b.getAttribute('data-t'); if(!t) return;" +
                        "if(t==='cancel'){closeMenu();return;}" +
                        "if(t==='recv-text'){insertMessage(where,1,1);}"+
                        "else if(t==='send-text'){insertMessage(where,2,1);}"+
                        "else if(t==='recv-img'){requestUpload({kind:'insert-image',where:where,msgType:1});closeMenu();return;}"+
                        "else if(t==='send-img'){requestUpload({kind:'insert-image',where:where,msgType:2});closeMenu();return;}"+
                        "closeMenu(); send();" +
                        "},true);" +
                        "window.__siyubao_menu={bd:bd,m:m};" +
                        "}" +
                        "function buildMessageWrapSph(msgType,contentType){" +
                        "var wrap=document.createElement('div');wrap.setAttribute('data-siyubao-wrap','1');wrap.className='message '+(msgType===2?'sent':'received');" +
                        "var content=document.createElement('div');content.className='message-content';content.setAttribute('data-siyubao-idx','0');content.setAttribute('data-siyubao-type',''+msgType);content.setAttribute('data-siyubao-time',topTime());content.setAttribute('data-siyubao-content',''+contentType);" +
                        "if(contentType===2){var img=document.createElement('img');img.className='siyubao-chat-img';img.setAttribute('src','/siyubao_cq_files/tos-cn-i-0813c000-ce_ogF0XgU4AAA5AeTEwEiCBqeBKfoSbhliEAEAMI.jpeg');content.appendChild(img);}"+
                        "else{content.innerText='新消息';}" +
                        "if(msgType===1){" +
                        "var sp=document.createElement('span');var av=document.createElement('div');av.className='avatar';var im=document.createElement('img');im.setAttribute('data-siyubao-avatar','user');im.setAttribute('alt','客服头像');im.setAttribute('src',metaVal('siyubao-user-pic')||'');av.appendChild(im);sp.appendChild(av);wrap.appendChild(sp);wrap.appendChild(content);" +
                        "}else{" +
                        "wrap.appendChild(content);var sp2=document.createElement('span');var av2=document.createElement('div');av2.className='avatar';var im2=document.createElement('img');im2.setAttribute('data-siyubao-avatar','me');im2.setAttribute('alt','用户头像');im2.setAttribute('src',metaVal('siyubao-my-pic')||'');av2.appendChild(im2);sp2.appendChild(av2);wrap.appendChild(sp2);" +
                        "}" +
                        "return wrap;" +
                        "}" +
                        "function buildMessageWrapXhs(msgType,contentType){" +
                        "var wrap=document.createElement('div');wrap.setAttribute('data-siyubao-wrap','1');wrap.className='flex items-start space-x-3';" +
                        "var otherAv=metaVal('siyubao-user-pic')||'';var myAv=metaVal('siyubao-my-pic')||'';" +
                        "if(msgType===1){" +
                        "var sp=document.createElement('span');var im=document.createElement('img');im.setAttribute('data-siyubao-avatar','user');im.setAttribute('alt','对方头像');im.className='w-10 h-10 rounded-full object-cover';im.setAttribute('src',otherAv);sp.appendChild(im);wrap.appendChild(sp);" +
                        "}" +
                        "var flex=document.createElement('div');flex.className='flex-1'+(msgType===2?' text-right':'');" +
                        "var bubble=document.createElement('div');bubble.setAttribute('data-siyubao-idx','0');bubble.setAttribute('data-siyubao-type',''+msgType);bubble.setAttribute('data-siyubao-time',topTime());bubble.setAttribute('data-siyubao-content',''+contentType);" +
                        "bubble.className=(msgType===2?'bg-chat-mine text-white px-4 py-3 chat-bubble-mine shadow-sm max-w-[calc(100%-3rem)] inline-block text-left':'bg-chat-other px-4 py-3 chat-bubble-other shadow-sm max-w-[calc(100%-3rem)] inline-block');" +
                        "if(contentType===2){var img=document.createElement('img');img.className='siyubao-chat-img';img.setAttribute('src','/siyubao_cq_files/tos-cn-i-0813c000-ce_ogF0XgU4AAA5AeTEwEiCBqeBKfoSbhliEAEAMI.jpeg');bubble.appendChild(img);}else{var p=document.createElement('p');p.innerText='新消息';bubble.appendChild(p);}" +
                        "flex.appendChild(bubble);wrap.appendChild(flex);" +
                        "if(msgType===2){" +
                        "var sp2=document.createElement('span');var im2=document.createElement('img');im2.setAttribute('data-siyubao-avatar','me');im2.setAttribute('alt','我的头像');im2.className='w-10 h-10 rounded-full object-cover';im2.setAttribute('src',myAv);sp2.appendChild(im2);wrap.appendChild(sp2);" +
                        "}" +
                        "return wrap;" +
                        "}" +
                        "function buildMessageWrap(msgType,contentType){" +
                        "var pf=metaVal('siyubao-platform')||'';" +
                        "if(pf==='xhs') return buildMessageWrapXhs(msgType,contentType);" +
                        "if(pf==='dy') return buildMessageWrapDy(msgType,contentType);" +
                        "return buildMessageWrapSph(msgType,contentType);" +
                        "}" +
                        "function buildMessageWrapDy(msgType,contentType){" +
                        "var wrap=document.createElement('div');wrap.setAttribute('data-siyubao-wrap','1');" +
                        "wrap.className=(msgType===2?'flex items-end justify-end mb-3':'flex items-start mb-3');" +
                        "var otherAv=metaVal('siyubao-user-pic')||'';var myAv=metaVal('siyubao-my-pic')||'';" +
                        "if(msgType===1){" +
                        "var div1=document.createElement('div');div1.className='mr-2 mt-1';" +
                        "var img1=document.createElement('img');img1.setAttribute('data-siyubao-avatar','user');img1.setAttribute('alt','对方头像');img1.className='w-10 h-10 avatar-circle';img1.setAttribute('src',otherAv);div1.appendChild(img1);wrap.appendChild(div1);" +
                        "}" +
                        "var msgWrapper=document.createElement('div');" +
                        "msgWrapper.className=(msgType===1?'message-wrapper':'max-w-[75%] mr-2');" +
                        "var bubble=document.createElement('div');bubble.setAttribute('data-siyubao-idx','0');bubble.setAttribute('data-siyubao-type',''+msgType);bubble.setAttribute('data-siyubao-time',topTime());bubble.setAttribute('data-siyubao-content',''+contentType);" +
                        "bubble.className=(msgType===2?'bg-douyin-blue chat-bubble-right px-4 py-2 text-white text-chat-md':'bg-douyin-white chat-bubble-left px-4 py-2 text-douyin-text text-chat-md');" +
                        "if(contentType===2){var img2=document.createElement('img');img2.className='max-w-full rounded-lg';img2.setAttribute('src','/siyubao_cq_files/tos-cn-i-0813c000-ce_ogF0XgU4AAA5AeTEwEiCBqeBKfoSbhliEAEAMI.jpeg');bubble.appendChild(img2);}else{var span=document.createElement('span');span.innerText='新消息';bubble.appendChild(span);}" +
                        "msgWrapper.appendChild(bubble);wrap.appendChild(msgWrapper);" +
                        "if(msgType===2){" +
                        "var div2=document.createElement('div');" +
                        "var img3=document.createElement('img');img3.setAttribute('data-siyubao-avatar','me');img3.setAttribute('alt','自己头像');img3.className='w-10 h-10 avatar-circle';img3.setAttribute('src',myAv);div2.appendChild(img3);wrap.appendChild(div2);" +
                        "}" +
                        "return wrap;" +
                        "}" +
                        "function insertMessage(where,msgType,contentType){" +
                        "var sel=selected; if(!sel) return;" +
                        "var w=nearestMessageWrap(sel); if(!w||!w.parentNode) return;" +
                        "var anchor=w; var tn=timeNodeForWrap(w); if(where==='above'&&tn) anchor=tn;" +
                        "var nw=buildMessageWrap(msgType,contentType);" +
                        "if(where==='above'){w.parentNode.insertBefore(nw,anchor);}else{" +
                        "if(w.nextSibling){w.parentNode.insertBefore(nw,w.nextSibling);}else{w.parentNode.appendChild(nw);}" +
                        "}" +
                        "setSelected(nw.querySelector('[data-siyubao-idx]'));" +
                        "updateReadMark();" +
                        "}" +
                        "function deleteSelected(){" +
                        "var sel=selected; if(!sel) return;" +
                        "var w=nearestMessageWrap(sel); if(!w||!w.parentNode) return;" +
                        "var tn=timeNodeForWrap(w); if(tn&&tn.parentNode) tn.parentNode.removeChild(tn);" +
                        "w.parentNode.removeChild(w);" +
                        "selected=null; updateReadMark(); send();" +
                        "}" +
                        "function toggleType(){" +
                        "var sel=selected; if(!sel) return;" +
                        "var w=nearestMessageWrap(sel); if(!w||!w.parentNode) return;" +
                        "var msgType=parseInt(sel.getAttribute('data-siyubao-type'))||1;" +
                        "var contentType=parseInt(sel.getAttribute('data-siyubao-content'))||1;" +
                        "var txt='';var imgEl=sel.querySelector('img'); if(imgEl){txt=imgEl.getAttribute('src')||'';} else {txt=(sel.innerText||'').trim();}" +
                        "var newType=msgType===1?2:1;" +
                        "var nw=buildMessageWrap(newType,contentType);" +
                        "var c=nw.querySelector('[data-siyubao-idx]');" +
                        "if(contentType===2){var im=c.querySelector('img'); if(im) im.setAttribute('src',txt||im.getAttribute('src')||'');} else {c.innerText=txt;}" +
                        "w.parentNode.replaceChild(nw,w);" +
                        "setSelected(nw.querySelector('[data-siyubao-idx]'));" +
                        "updateReadMark(); send();" +
                        "}" +
                        "document.addEventListener('click',function(e){" +
                        "var t=e.target; if(!t) return;" +
                        "var av=t.closest ? t.closest('img[data-siyubao-avatar]') : null;" +
                        "if(av){" +
                        "var who=av.getAttribute('data-siyubao-avatar')||'';" +
                        "if(who==='user'){requestUpload({kind:'avatar-user'});}else if(who==='me'){requestUpload({kind:'avatar-me'});}"+
                        "return;" +
                        "}" +
                        "var timeTop=t.closest ? t.closest('[data-siyubao-top-time]') : null;" +
                        "if(timeTop){setSelected(null);enableEdit(timeTop);return;}" +
                        "var timeNode=t.closest ? t.closest('[data-siyubao-time-for]') : null;" +
                        "if(timeNode){setSelected(null);enableEdit(timeNode);return;}" +
                        "var el=t.closest ? t.closest('[data-siyubao-idx]') : null;" +
                        "var readText=t.closest ? t.closest('[data-siyubao-read-text]') : null;" +
                        "if(readText){" +
                        "setSelected(null);" +
                        "enableEdit(readText);" +
                        "return;" +
                        "}" +
                        "if(!el) return;" +
                        "setSelected(el);" +
                        "var img=el.querySelector('img');" +
                        "if(img){" +
                        "requestUpload({kind:'replace-image',idx:parseInt(el.getAttribute('data-siyubao-idx'))||0});" +
                        "return;" +
                        "}" +
                        "enableEdit(el);" +
                        "},true);" +
                        "document.addEventListener('dblclick',function(e){" +
                        "var t=e.target; if(!t) return;" +
                        "var el=t.closest ? t.closest('[data-siyubao-idx]') : null;" +
                        "if(!el) return;" +
                        "var msgWrap=nearestMessageWrap(el); if(!msgWrap) return;" +
                        "var tn=timeNodeForWrap(msgWrap);" +
                        "if(tn){tn.parentNode&&tn.parentNode.removeChild(tn);send();return;}" +
                        "var dt=el.getAttribute('data-siyubao-time')||topTime()||'';" +
                        "var div=document.createElement('div');div.className='message-time';div.setAttribute('data-siyubao-time-for','1');div.innerText=dt;" +
                        "msgWrap.parentNode.insertBefore(div,msgWrap);" +
                        "enableEdit(div);" +
                        "},true);" +
                        "document.addEventListener('blur',function(e){" +
                        "var el=e.target; if(!el || !el.getAttribute) return;" +
                        "if(el.getAttribute('data-siyubao-idx')==null && el.getAttribute('data-siyubao-top-time')==null && el.getAttribute('data-siyubao-time-for')==null && el.getAttribute('data-siyubao-read-text')==null) return;" +
                        "el.removeAttribute('contenteditable'); el.classList.remove('siyubao-editing'); send();" +
                        "},true);" +
                        "document.addEventListener('DOMContentLoaded',function(){" +
                        "setTimeout(updateReadMark,100);" +
                        "});" +
                        "setTimeout(updateReadMark,200);" +
                        "})();</script>";
        int bodyEnd = indexOfIgnoreCase(html, "</body>");
        if (bodyEnd >= 0) {
            return html.substring(0, bodyEnd) + script + html.substring(bodyEnd);
        }
        return html + script;
    }

    private String escapeHtmlAttr(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private int indexOfIgnoreCase(String s, String target) {
        if (s == null || target == null) return -1;
        return s.toLowerCase(java.util.Locale.ROOT).indexOf(target.toLowerCase(java.util.Locale.ROOT));
    }

    private String templateByPlatform(String platform) {
        if (platform == null) return "siyubao_cq";
        if (platform.equals("dy")) return "douyin_mobile_chat.html";
        if (platform.equals("xhs")) return "chat-interface-v4.html";
        if (platform.equals("sph")) return "wechat-mobile-chat.html";
        return "siyubao_cq";
    }

    private String getDynamicRouteAvatar(Long userId, String routeValue, String platform) {
        try {
            Route targetRoute = routeService.getRouteByValue(routeValue, userId).orElse(null);
            if (targetRoute != null) {
                String avatarPath = null;
                switch (platform.toLowerCase()) {
                    case "dy":
                        avatarPath = targetRoute.getDouyinAvatar();
                        break;
                    case "sph":
                        avatarPath = targetRoute.getShipinAvatar();
                        break;
                    case "xhs":
                        avatarPath = targetRoute.getXiaohongshuAvatar();
                        break;
                    default:
                        break;
                }
                if (avatarPath != null && !avatarPath.trim().isEmpty()) {
                    return avatarPath;
                }
            }
        } catch (Exception ignored) {
        }
        Map<String, String> fallbackResult = userStant.getXianluNameAndPic(routeValue, platform);
        return fallbackResult.get("xianluPic");
    }

    private String normalizeStaticUrl(String url) {
        if (url == null) return null;
        String t = url.trim();
        if (t.isEmpty()) return t;
        if (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("data:") || t.startsWith("blob:") || t.startsWith("//")) return t;
        if (t.startsWith("/")) return t;
        if (t.startsWith("./")) return "/" + t.substring(2);
        return "/" + t;
    }

    private List<ChatMessage> generateChatMessage(LocalTime now, String xianlu, String chatContent, String platform, String userPic) throws IOException {
        List<ChatMessage> chatMessageList = new ArrayList<>();
        List<String> cc = new ArrayList<>();
        if (chatContent != null && !chatContent.trim().isEmpty()) {
            String[] lines = chatContent.split("\\n|\\r\\n|\\r");
            if (lines.length == 0) {
                return chatMessageList;
            }
            cc = Arrays.stream(lines)
                    .filter(line -> line != null && !line.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        } else {
            String path = "static/chatcontent/" + xianlu + "_dychat.txt";
            if (platform.equals("xhs")) {
                path = "static/chatcontent/" + xianlu + "_xhschat.txt";
            } else if (platform.equals("sph")) {
                path = "static/chatcontent/" + xianlu + "_sphchat.txt";
            }
            ClassPathResource resource = new ClassPathResource(path);
            cc = userStant.getFileLinesByResource(resource);
        }

        String first = cc.get(0);
        if (!first.endsWith("11")) {
            throw new RuntimeException("缺少用户名称");
        }
        first = first.substring(0, first.length() - 2);

        LocalTime localTimeBefore = now.minusMinutes(RandomUtils.nextInt(1, 2));

        for (int i = 1; i < cc.size(); i++) {
            String ct = cc.get(i);
            if (ct == null || ct.trim().isEmpty()) continue;
            if (ct.endsWith("end")) break;

            ChatMessage m1 = new ChatMessage();
            m1.setMsgType(1);
            m1.setUserName(first);
            m1.setUserPic(userPic);
            if (ct.endsWith("22")) {
                m1.setMsgType(2);
                ct = ct.substring(0, ct.length() - 2);
            }
            if (ct.trim().isEmpty()) continue;
            m1.setMsg(ct);
            m1.setDateTimeStr(userStant.getTimeStr(localTimeBefore.getHour()) + ":" + userStant.getTimeStr(localTimeBefore.getMinute()));
            chatMessageList.add(m1);

            if (i == (cc.size() - 2) || i == (cc.size() - 3)) {
                localTimeBefore = now;
            }
        }
        return chatMessageList;
    }

    public static String getWelcomeMsg(String xianlu) {
        String xianluName = XianluEnum.getNameByCode(xianlu);
        return WELCOME_TEMPLATE.replaceAll("xianlu", xianluName);
    }
}
