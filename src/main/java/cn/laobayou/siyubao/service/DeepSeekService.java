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
        //将消息的url截取出后面的短码 https://aih5.lm12301.com/o/mBz0xELc6CjyAl2
        String shortcode = msgUrl.substring(msgUrl.indexOf("id=") + 3);

        SiyubaoJsonRequest requestBody = SiyubaoJsonRequest.builder()
                .shortcode(shortcode)
                .build();
        // 创建HTTP请求
        Request request = new Request.Builder()
                .url(yunqueUrl+shortcode)
                .get()
                .build();
        // 发送请求并处理响应
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("请求云que系统的json接口出现异常 Unexpected code " + response);
            }
            String rsStr=response.body().string();


            String showName=(String)((Map<String, Object>)(((Map<String, Object>)(((Map<String, Object>) JSONObject.parse(rsStr)).get("result"))).get("user"))).get("showname");

            JSONArray result =(JSONArray) (((Map<String, Object>)(((Map<String, Object>) JSONObject.parse(rsStr)).get("result"))).get("conversation"));
            List<SiyubaoJsonResponse> siyubaoResponses = result.toJavaList(SiyubaoJsonResponse.class);
            siyubaoResponses.get(0).setShowname(showName);
            return siyubaoResponses;
//            SiyubaoResponse siyubaoResponse = JSON.parseObject(response.body().string(), SiyubaoResponse.class);
//            return siyubaoResponse.getData().getMsg_list();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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