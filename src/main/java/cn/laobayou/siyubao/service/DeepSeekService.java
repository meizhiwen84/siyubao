package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.DeepSeekRequest;
import cn.laobayou.siyubao.bean.DeepSeekRequestMessage;
import cn.laobayou.siyubao.bean.DeepSeekResponse;
import cn.laobayou.siyubao.bean.SiyubaoJsonRequest;
import cn.laobayou.siyubao.bean.SiyubaoJsonResponse;
import cn.laobayou.siyubao.bean.SiyubaoResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DeepSeekService {

    @Value("${deepseek.api.url}")
    private String url;

    @Value("${deepseek.api.model}")
    private String model;

    @Value("${siyubao.url}")
    private String siyubaoJsonUrl;

    private String yunqueUrl="https://wapi.yunque360.com/v1/chat/push/info?id=";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 请求云que的系统的json数据
     * @param msgUrl
     * @return
     */
    public List<SiyubaoJsonResponse>requestYunqueChatJson(String msgUrl) {
        try {
            // 记录输入参数
            System.out.println("=== 云雀平台接口调用开始 ===");
            System.out.println("输入URL: " + msgUrl);
            
            // URL解码处理
            String decodedUrl = msgUrl;
            try {
                decodedUrl = URLDecoder.decode(msgUrl, StandardCharsets.UTF_8.toString());
                System.out.println("URL解码后: " + decodedUrl);
            } catch (Exception e) {
                System.out.println("URL解码失败，使用原始URL: " + e.getMessage());
            }
            
            // 检查URL格式（先检查解码后的URL，如果没有id=则检查原始URL）
            String urlToCheck = decodedUrl.contains("id=") ? decodedUrl : msgUrl;
            if (urlToCheck == null || (!urlToCheck.contains("id=") && !urlToCheck.contains("id%3D"))) {
                System.err.println("错误: URL格式不正确，缺少id参数: " + msgUrl);
                return null;
            }
            
            //将消息的url截取出后面的短码
            String shortcode;
            if (decodedUrl.contains("id=")) {
                shortcode = decodedUrl.substring(decodedUrl.indexOf("id=") + 3);
            } else {
                // 处理URL编码的情况
                shortcode = msgUrl.substring(msgUrl.indexOf("id%3D") + 5);
            }
            System.out.println("提取的shortcode: " + shortcode);
            
            // 构造完整的请求URL
            String fullUrl = yunqueUrl + shortcode;
            System.out.println("完整请求URL: " + fullUrl);

            SiyubaoJsonRequest requestBody = SiyubaoJsonRequest.builder()
                    .shortcode(shortcode)
                    .build();
            // 创建HTTP请求
            Request request = new Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build();
            
            System.out.println("发送HTTP GET请求...");
            
            // 发送请求并处理响应
            Response response = client.newCall(request).execute();
            System.out.println("响应状态码: " + response.code());
            System.out.println("响应消息: " + response.message());
            
            if (!response.isSuccessful()) {
                String errorMsg = "请求云que系统的json接口出现异常 - 状态码: " + response.code() + ", 消息: " + response.message();
                System.err.println(errorMsg);
                throw new IOException(errorMsg);
            }
            
            String rsStr = response.body().string();
            System.out.println("响应内容: " + rsStr);
            System.out.println("响应内容长度: " + rsStr.length());
            System.out.println("响应内容前500字符: " + (rsStr.length() > 500 ? rsStr.substring(0, 500) + "..." : rsStr));

            // 解析响应JSON
            JSONObject jsonResponse = JSONObject.parseObject(rsStr);
            if (jsonResponse == null) {
                System.err.println("错误: 无法解析响应JSON");
                return null;
            }
            
            // 检查result字段
            Object resultObj = jsonResponse.get("result");
            if (resultObj == null) {
                System.err.println("错误: 响应中缺少result字段");
                return null;
            }
            System.out.println("响应内容resultObj: " + resultObj);
            Map<String, Object> result = (Map<String, Object>) resultObj;
            System.out.println("响应内容result: " + result);
            // 提取用户信息
            String showName = null;
            Object userObj = result.get("user");
            if (userObj != null) {
                Map<String, Object> user = (Map<String, Object>) userObj;
                showName = (String) user.get("showname");
                System.out.println("提取的用户名: " + showName);
            }

            // 提取对话信息
            Object conversationObj = result.get("conversation");
            if (conversationObj == null) {
                System.err.println("错误: 响应中缺少conversation字段");
                return null;
            }
            
            JSONArray conversation = (JSONArray) conversationObj;
            System.out.println("对话条数: " + conversation.size());
            
            List<SiyubaoJsonResponse> siyubaoResponses = conversation.toJavaList(SiyubaoJsonResponse.class);
            if (siyubaoResponses != null && !siyubaoResponses.isEmpty() && showName != null) {
                siyubaoResponses.get(0).setShowname(showName);
            }
            
            System.out.println("=== 云雀平台接口调用成功 ===");
            return siyubaoResponses;
            
        } catch (Exception e) {
            System.err.println("=== 云雀平台接口调用异常 ===");
            System.err.println("异常类型: " + e.getClass().getSimpleName());
            System.err.println("异常消息: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


        /**
         * 请求云客通的系统的json数据
         * @param msgUrl
         * @return
         */
    public List<SiyubaoJsonResponse>requestChatJson(String msgUrl){
        //将消息的url截取出后面的短码 https://aih5.lm12301.com/o/mBz0xELc6CjyAl2
        String shortcode=msgUrl.substring(msgUrl.indexOf("o/")+2);

        SiyubaoJsonRequest requestBody = SiyubaoJsonRequest.builder()
                .shortcode(shortcode)
                .build();
        // 创建HTTP请求
        Request request = new Request.Builder()
                .url(siyubaoJsonUrl)
                .post(RequestBody.create(JSON.toJSONString(requestBody), MediaType.get("application/json")))
                .build();
        // 发送请求并处理响应
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("请求云客通系统的json接口出现异常 Unexpected code " + response);
            }
            SiyubaoResponse siyubaoResponse = JSON.parseObject(response.body().string(), SiyubaoResponse.class);
            return siyubaoResponse.getData().getMsg_list();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String chat(List<DeepSeekRequestMessage> messageList) throws IOException {
        //将用户问的这句话封闭成一个Messages对象，最终转换成json格式设置到prompt属性里面



        DeepSeekRequest requestBody = DeepSeekRequest.builder()
                .model(model)
                .messages(messageList)
                .think(false)
                .stream(false)
                .build();
        // 创建HTTP请求
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON.toJSONString(requestBody), MediaType.get("application/json")))
                .build();
        // 发送请求并处理响应
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            DeepSeekResponse deepSeekResponse = JSON.parseObject(response.body().string(), DeepSeekResponse.class);
            return deepSeekResponse.getMessage().getContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}