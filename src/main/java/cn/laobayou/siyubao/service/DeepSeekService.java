package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.DeepSeekRequest;
import cn.laobayou.siyubao.bean.DeepSeekRequestMessage;
import cn.laobayou.siyubao.bean.DeepSeekResponse;
import com.alibaba.fastjson.JSON;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DeepSeekService {

    @Value("${deepseek.api.url}")
    private String url;

    @Value("${deepseek.api.model}")
    private String model;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

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