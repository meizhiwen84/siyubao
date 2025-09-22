package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.ChatMessage;
import cn.laobayou.siyubao.bean.Route;
import cn.laobayou.siyubao.bean.XianluEnum;
import cn.laobayou.siyubao.service.DeepSeekService;
import cn.laobayou.siyubao.service.RouteService;
import cn.laobayou.siyubao.service.SiyubaoConfig;
import cn.laobayou.siyubao.service.UserStant;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抖音私信聊天内空生成
 *
 *   dychat.txt 聊天内容文件里的内容，尾部添加"22" ，标示这一行是我自己发出的信息
 */
@Slf4j
@Controller
public class DySxChatGenerateController {
    //恩施头像
    private static String xinaluPic="https://p26.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-i-0813c001_owEAQCKfa5HIKfA9IRtnlAFPgABD6FgAAG0ErC.jpeg?from=4010531038";
    //恩施名称
    private static String xinaluName="恩施百晓通";
    //===============================================

    private static String title="云客";//聊天工具名称



    private static String myPic=xinaluPic;//自己的头像
    private static String myName=xinaluName;//自己的名称

    private int 几大=2;
    private int 几小=1;
    private int 几天=3;
    private String 么的时候="下个月";
    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private SiyubaoConfig siyubaoConfig;
    
    @Autowired
    private UserStant userStant;
    
    @Autowired
    private RouteService routeService;

    private static String welcomeMsg="你好，欢迎来xianlu旅游！ 目前xianlu旅游限时特惠优惠多多，您这边大概几个人，什么时候出行呢？可以留个联系方式，给你发行程报价参考下！";

    public static String getWelcomeMsg(String xianlu){
        String xianluName = XianluEnum.getNameByCode(xianlu);
        String rs=welcomeMsg.replaceAll("xianlu",xianluName);
         return rs;
    }

    /**
     * 动态获取线路头像
     * @param routeValue 线路值
     * @param platform 平台类型 (dy-抖音, sph-视频号, xhs-小红书)
     * @return 头像路径，如果没有找到则返回默认头像
     */
    private String getDynamicRouteAvatar(String routeValue, String platform) {
        try {
            // 查找所有线路
            List<Route> routes = routeService.getAllRoutes();
            
            // 根据routeValue查找对应线路
            Route targetRoute = routes.stream()
                    .filter(route -> route.getRouteValue().equals(routeValue))
                    .findFirst()
                    .orElse(null);
            
            if (targetRoute != null) {
                String avatarPath = null;
                
                // 根据平台获取对应头像
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
                        log.warn("不支持的平台类型: {}", platform);
                        break;
                }
                
                // 如果找到头像路径且不为空，返回该路径
                if (avatarPath != null && !avatarPath.trim().isEmpty()) {
                    log.info("找到动态头像: 线路={}, 平台={}, 头像={}", routeValue, platform, avatarPath);
                    return avatarPath;
                }
            }
            
            log.info("未找到动态头像，使用默认配置: 线路={}, 平台={}", routeValue, platform);
        } catch (Exception e) {
            log.error("获取动态头像失败: 线路={}, 平台={}, 错误={}", routeValue, platform, e.getMessage());
        }
        
        // 如果没有找到或出现异常，回退到原有的静态配置方式
        Map<String, String> fallbackResult = userStant.getXianluNameAndPic(routeValue, platform);
        return fallbackResult.get("xianluPic");
    }

    @RequestMapping("/reGenerateDyChat")
    public String reGen(ModelMap modelMap,@RequestParam String xianshiname,String platform) throws IOException {
        LocalTime now = LocalTime.now(java.time.ZoneId.of("Asia/Shanghai"));
//        String xianlu="";//

        //读取文件里rechatcontent.txt 用于还原聊天内容的日志记录
        List<String> cc = Files.readAllLines(Paths.get("/Users/meizhiwen/dev/siyubao/src/main/resources/static/rechatcontent/rechatcontent.txt"));
        if(cc.size()>1){
            throw new RuntimeException("文件内容出错");
        }

        //解析这个字符串=======start
//        String input = "线路:cq||用户名称:用户8628897890167||用户头像:./avatar/avatar_11397.jpg||聊天内容:[{"contentType":1,"dateTimeStr":"22:42","msg":"五大一小","msgType":1,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"您好呀，您这边计划什么时候出行呢","msgType":2,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"29号到","msgType":1,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"多少钱","msgType":1,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"好的。您留个微，我发您吧，您看下行程安排和报价","msgType":2,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"13730002886","msgType":1,"userName":"用户8628897890167"},{"contentType":1,"dateTimeStr":"22:42","msg":"好的，马上安排专属管家添加您一会儿您记得通过一下我哦","msgType":2,"userName":"用户8628897890167"}]";

        // 定义四个变量存储提取的信息
        String xianlu = "";
        String userName = "";
        String userAvatar = "";
        String chatContent = "";

        // 使用正则表达式提取各部分信息
        Pattern pattern = Pattern.compile("线路:(.*?)\\|\\|用户名称:(.*?)\\|\\|用户头像:(.*?)\\|\\|聊天内容:(.*)");
        Matcher matcher = pattern.matcher(cc.get(0));

        if (matcher.find()) {
            xianlu = matcher.group(1);
            userName = matcher.group(2);
            userAvatar = matcher.group(3);
            chatContent = matcher.group(4);
        }

        // 输出提取的信息
        log.info("线路: " + xianlu);
        log.info("用户名称: " + userName);
        log.info("用户头像: " + userAvatar);
        log.info("聊天内容: " + chatContent);
        //解析结束===============end

        // 获取动态头像
        String dynamicAvatar = getDynamicRouteAvatar(xianlu, platform);
        Map<String, String> xianluNameAndPic = userStant.getXianluNameAndPic(xianlu,platform);

        modelMap.addAttribute("title", xianlu+"-dy截图生成聊天");
        modelMap.addAttribute("message", title);
        modelMap.addAttribute("myPic", dynamicAvatar);
        modelMap.addAttribute("myName",(xianshiname!=null&&xianshiname.equals("true"))?xianluNameAndPic.get("xianluName"):"");
//        String userPic=userStant.getRandomUserPic();
        modelMap.addAttribute("userPic", userAvatar);

        List<ChatMessage> chatMessageList=JSON.parseArray(chatContent,ChatMessage.class);

        // 过滤空行数据，确保输出内容不包含空消息
        if (chatMessageList != null) {
            chatMessageList = chatMessageList.stream()
                    .filter(msg -> msg != null && msg.getMsg() != null && !msg.getMsg().trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }

        //generateChatMessage(now,xianlu);
//        log.info("线路:"+xianlu+ "||用户名称:"+userName + "||用户头像:"+ userAvatar + "||聊天内容:"+ JSON.toJSONString(chatMessageList));
        if(JSON.toJSONString(chatMessageList).equals(chatContent)){
            log.info("==================================复现的聊天记录是一样的===========================================");
        }

        List<ChatMessage> xhsChatMessageList=new ArrayList();
        xhsChatMessageList.addAll(chatMessageList);
        //再添加最后一个需要反馈的话术

        ChatMessage m1=new ChatMessage();
        m1.setMsgType(2);

        m1.setMsg("亲;这边管家已经加您了哈;您通过-下哦");

        String fankuiDateTimeStr=userStant.getTimeStr(now.getHour())+":"+userStant.getTimeStr(now.getMinute());

        m1.setDateTimeStr(fankuiDateTimeStr);

        chatMessageList.add(m1);

        log.info("添加反馈后的聊天内容: " + JSON.toJSONString(chatMessageList));

        modelMap.addAttribute("userName", chatMessageList.get(0).getUserName());
        modelMap.addAttribute("msgList", chatMessageList);
        modelMap.addAttribute("xhsMsgList", xhsChatMessageList);
        modelMap.addAttribute("firstDateTimeStr", chatMessageList.get(0).getDateTimeStr());
        modelMap.addAttribute("fankuiDateTimeStr", fankuiDateTimeStr);

        if(platform!=null&&!platform.trim().equals("")){
            if(platform.equals("dy")){
                return "siyubao_cq";
            }
            if(platform.equals("xhs")){
                return "chat-interface-v4-fk.html";
            }
            if(platform.equals("sph")){
                return "wechat-mobile-chat.html";
            }
        }
        return "siyubao_cq";
    }

    /**
     * 封装聊天内容的数据结构
     * 双方聊天是多对多的关系
     *
     * 一对一
     * 一对多
     * 多对一
     * 多对多
     *
     * 定义一个聊天发送方的标识：1：客人 2：自己
     * 定义一个每条发送消息之后，对方需要回复的消息条数：1-10，3表示对方回复3条消息
     *
     *chatContent :表示聊天内容是从前面传过来，不是从文件里面读取的
     sph：  洗成视频号的粉

     platform:dy  xhs sph
     * @param modelMap
     * @return
     */

    @RequestMapping("/generateDyChat")
    public String gen(ModelMap modelMap,@RequestParam(required = false, defaultValue = "sc") String xianlu, String xianshiname, @RequestParam(required = false) String chatContent,String platform) throws IOException {
        // 获取中国时区(UTC+8)的当前时间
        LocalTime now = LocalTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        if(StringUtils.isBlank(platform)){
            platform="dy";
        }
        
        // 接收并打印前端传递的聊天内容参数
        if (chatContent != null && !chatContent.trim().isEmpty()) {
            // 对特殊字符进行安全处理，防止日志注入
            String safeChatContent = chatContent.replaceAll("[\r\n\t]", " ").trim();
            if (safeChatContent.length() > 500) {
                safeChatContent = safeChatContent.substring(0, 500) + "...";
            }
            log.info("接收到前端传递的聊天内容参数: [{}]", safeChatContent);
            log.info("聊天内容参数长度: {} 字符", chatContent.length());
        } else {
            log.info("未接收到聊天内容参数或参数为空");
        }

        // 获取动态头像
        String dynamicAvatar = getDynamicRouteAvatar(xianlu, platform);
        Map<String, String> xianluNameAndPic = userStant.getXianluNameAndPic(xianlu,platform);


        modelMap.addAttribute("title", platform+"-"+xianlu+"-dy截图生成聊天");
        modelMap.addAttribute("message", platform+title);
        modelMap.addAttribute("myPic", dynamicAvatar);
        modelMap.addAttribute("myName",(xianshiname!=null&&xianshiname.equals("true"))?xianluNameAndPic.get("xianluName"):"");
        String userPic=userStant.getRandomUserPic();
        modelMap.addAttribute("userPic", userPic);

        List<ChatMessage> chatMessageList = generateChatMessage(now, xianlu, chatContent,platform);
        log.info("线路:"+xianlu+ "||用户名称:"+chatMessageList.get(0).getUserName() + "||用户头像:"+ userPic + "||聊天内容:"+ JSON.toJSONString(chatMessageList));
        log.info("==================================#########################===========================================");
        modelMap.addAttribute("userName", chatMessageList.get(0).getUserName());
        modelMap.addAttribute("msgList", chatMessageList);
        modelMap.addAttribute("firstDateTimeStr", chatMessageList.get(0).getDateTimeStr());
        modelMap.addAttribute("welcomword", xianluNameAndPic.get("welcomword"));

        if(platform!=null&&!platform.trim().equals("")){
            if(platform.equals("dy")){
                return "siyubao_cq";
            }
            if(platform.equals("xhs")){
                return "chat-interface-v4.html";
            }
            if(platform.equals("sph")){
                return "wechat-mobile-chat.html";
            }
        }
        return "siyubao_cq";
    }

    private List<ChatMessage> generateChatMessage(LocalTime now, String xianlu,String chatContent,String platform) throws IOException {
        List<ChatMessage> chatMessageList=new ArrayList();
        List<String> cc =new ArrayList<>();
        // 将chatContent按行分割
        if (chatContent != null && !chatContent.trim().isEmpty()) {
            String[] lines = chatContent.split("\\n|\\r\\n|\\r");

            if (lines.length == 0) {
                return chatMessageList;
            }
            // 过滤空行数据，确保输出内容不包含空行
            cc = Arrays.stream(lines)
                    .filter(line -> line != null && !line.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }else{
            /**
             * 首先从文件中读取复制出来的聊天内容
             *
             */
            String path = "static/chatcontent/" + xianlu + "_dychat.txt";
            if (platform.equals("xhs")) {
                path = "static/chatcontent/" + xianlu + "_xhschat.txt";
            } else if (platform.equals("sph")) {
                path = "static/chatcontent/" + xianlu + "_sphchat.txt";
            }

            // 判断当前运行环境
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                // Mac本地环境使用绝对路径
                cc = Files.readAllLines(Paths.get("/Users/meizhiwen/dev/siyubao/src/main/resources/" + path));
            } else {
                // 生产环境使用ClassPathResource从classpath读取
                ClassPathResource resource = new ClassPathResource(path);
                cc = userStant.getFileLinesByResource(resource);
            }
        }
        String first = cc.get(0);;//第一句话不是11结尾的，就报错，表示没有用户的名称
        if(!first.endsWith("11")){
            throw new RuntimeException("缺少用户名称");
        }
        first=first.substring(0,first.length()-2);
        //总共有几句话对话
//        int chatCnt = cc.size()-1;//5

        LocalTime localTimeBefore = now;
        localTimeBefore = now.minusMinutes(RandomUtils.nextInt(1,2));//最原始第一句话的时间,在当前时间往前面蝛一或者两分钟

        for (int i = 1; i < cc.size(); i++) {
            //循环每一句话生成聊天内容
            String ct=cc.get(i);

            // 过滤空行数据，跳过空的聊天内容
            if (ct == null || ct.trim().isEmpty()) {
                continue;
            }

            //判断是不是以"end"结尾
            if(ct.endsWith("end")){
                break;
            }

            ChatMessage m1=new ChatMessage();
            m1.setMsgType(1);
            m1.setUserName(first);

            //判断是否包含以22结尾
            if(ct.endsWith("22")){
                //表示是我的消息
                m1.setMsgType(2);
                ct=ct.substring(0,ct.length()-2);
            }
            
            // 再次检查处理后的消息内容是否为空
            if (ct.trim().isEmpty()) {
                continue;
            }
            
            m1.setMsg(ct);

            m1.setDateTimeStr(userStant.getTimeStr(localTimeBefore.getHour())+":"+userStant.getTimeStr(localTimeBefore.getMinute()));

            chatMessageList.add(m1);

            //判断是不是倒数第二条消息，如果是，表示下一条消息就是最后一条消息了。就需要将时间改成当前的时间
            if(i==(cc.size()-2) || i==(cc.size()-3)){
                //最后一条消息
                localTimeBefore=now;
            }else if(i<(cc.size()-3)){
                //前面的消息，就将消息的生成时间往前加几分钟
                //localTimeBefore = localTimeBefore.plusMinutes(RandomUtils.nextInt(1,2));//减3分钟
            }
        }

        return chatMessageList;
    }

    /**
     * 获取线路头像API
     * @param routeValue 线路值
     * @param platform 平台类型 (dy-抖音, sph-视频号, xhs-小红书)
     * @return 头像路径
     */
    @GetMapping("/api/route/avatar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRouteAvatar(
            @RequestParam String routeValue,
            @RequestParam(defaultValue = "dy") String platform) {
        Map<String, Object> result = new HashMap<>();
        try {
            String avatarPath = getDynamicRouteAvatar(routeValue, platform);
            result.put("success", true);
            result.put("avatarPath", avatarPath);
            result.put("routeValue", routeValue);
            result.put("platform", platform);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取线路头像失败: routeValue={}, platform={}, error={}", routeValue, platform, e.getMessage());
            result.put("success", false);
            result.put("message", "获取头像失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

}
