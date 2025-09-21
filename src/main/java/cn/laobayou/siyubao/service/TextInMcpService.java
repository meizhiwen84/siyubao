package cn.laobayou.siyubao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TextIn 通用文字识别服务
 * 直接调用TextIn HTTP API进行图片文字识别
 */
@Slf4j
@Service
public class TextInMcpService {

    // TextIn API配置
    private static final String TEXTIN_API_URL = "https://api.textin.com/ai/service/v2/recognize";
    private static final int TIMEOUT_MS = 30000; // 30秒超时

    @Value("${ocr.textin.mcp.timeout:30}")
    private int timeoutSeconds;

    @Value("${ocr.textin.mcp.enabled:true}")
    private boolean mcpEnabled;
    
    @Value("${ocr.textin.app.id:}")
    private String appId;
    
    @Value("${ocr.textin.app.secret:}")
    private String appSecret;

    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        log.info("TextIn API服务初始化完成 - enabled: {}, appId: {}", 
                mcpEnabled, appId != null && !appId.isEmpty() ? "已配置" : "未配置");
    }

    /**
     * 使用TextIn API进行文字识别
     * @param imageFile 图片文件
     * @return 识别出的文字
     * @throws Exception 识别异常
     */
    public String recognizeText(MultipartFile imageFile) throws Exception {
        if (!mcpEnabled) {
            throw new RuntimeException("TextIn服务已禁用");
        }

        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            throw new RuntimeException("TextIn API配置不完整，请检查app-id和app-secret");
        }

        long startTime = System.currentTimeMillis();
        log.info("开始TextIn API文字识别，文件名: {}, 文件大小: {} bytes", 
                imageFile.getOriginalFilename(), imageFile.getSize());

        // 验证文件
        validateImageFile(imageFile);

        // 创建临时文件
        Path tempFile = createTempImageFile(imageFile);
        
        try {
            // 调用TextIn API
            String result = callTextInApi(tempFile);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("TextIn API识别完成，耗时: {} ms，结果长度: {}", 
                    duration, result != null ? result.length() : 0);
            
            return result;
            
        } finally {
            // 清理临时文件
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 验证图片文件
     */
    private void validateImageFile(MultipartFile imageFile) throws IOException {
        if (imageFile.isEmpty()) {
            throw new IOException("图片文件不能为空");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("不支持的文件类型，请上传图片文件");
        }

        // 限制文件大小为10MB
        if (imageFile.getSize() > 10 * 1024 * 1024) {
            throw new IOException("图片文件过大，请上传小于10MB的图片");
        }
    }

    /**
     * 创建临时图片文件
     */
    private Path createTempImageFile(MultipartFile imageFile) throws IOException {
        String originalFilename = imageFile.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".png";
        
        Path tempFile = Files.createTempFile("textin_api_", extension);
        
        try (InputStream inputStream = imageFile.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        
        log.debug("创建临时图片文件: {}", tempFile.toAbsolutePath());
        return tempFile;
    }

    /**
     * 调用TextIn API进行文字识别
     */
    private String callTextInApi(Path imagePath) throws Exception {
        // 读取图片文件
        byte[] imageData = Files.readAllBytes(imagePath);
        
        // 创建HTTP连接
        URL url = new URL(TEXTIN_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // 设置请求方法和头部
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("x-ti-app-id", appId);
            connection.setRequestProperty("x-ti-secret-code", appSecret);
            connection.setRequestProperty("Connection", "Keep-Alive");
            
            // 设置超时
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            
            // 允许输出
            connection.setDoOutput(true);
            connection.setDoInput(true);
            
            log.debug("发送TextIn API请求，图片大小: {} bytes", imageData.length);
            
            // 发送图片数据
            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                out.write(imageData);
                out.flush();
            }
            
            // 读取响应
            int responseCode = connection.getResponseCode();
            InputStream inputStream = responseCode == 200 ? 
                connection.getInputStream() : connection.getErrorStream();
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            if (responseCode != 200) {
                log.error("TextIn API请求失败，状态码: {}, 响应: {}", responseCode, response.toString());
                throw new RuntimeException("HTTP请求失败，状态码: " + responseCode + ", 响应: " + response.toString());
            }
            
            log.info("TextIn API响应成功，状态码: {}, 响应长度: {}, 响应内容: {}", responseCode, response.length(), response.toString());
            
            // 解析API响应
            return parseTextInApiResponse(response.toString());
            
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 解析TextIn API响应
     */
    private String parseTextInApiResponse(String apiResponse) throws Exception {
        try {
            JsonNode responseJson = objectMapper.readTree(apiResponse);
            
            // 检查响应状态
            if (!responseJson.has("code") || responseJson.get("code").asInt() != 200) {
                String errorMessage = responseJson.path("message").asText("未知错误");
                int errorCode = responseJson.path("code").asInt(-1);
                throw new RuntimeException("TextIn API调用失败: " + errorMessage + " (错误码: " + errorCode + ")");
            }
            
            // 提取识别结果
            JsonNode result = responseJson.path("result");
            if (result.isMissingNode()) {
                log.warn("TextIn API响应中缺少result字段");
                return "";
            }
            
            // 提取文本行
            StringBuilder fullText = new StringBuilder();
            JsonNode linesNode = result.path("lines");
            
            if (linesNode.isArray()) {
                for (JsonNode lineNode : linesNode) {
                    String lineText = lineNode.path("text").asText("");
                    if (!lineText.isEmpty()) {
                        if (fullText.length() > 0) {
                            fullText.append("\n");
                        }
                        fullText.append(lineText);
                    }
                }
            }
            
            String resultText = fullText.toString();
            log.debug("TextIn API识别结果，文本行数: {}, 总长度: {}", 
                    linesNode.size(), resultText.length());
            
            return resultText;
            
        } catch (Exception e) {
            log.error("解析TextIn API响应失败: {}", e.getMessage());
            throw new Exception("解析API响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(Path tempFile) {
        if (tempFile != null && Files.exists(tempFile)) {
            try {
                Files.delete(tempFile);
                log.debug("删除临时文件: {}", tempFile.toAbsolutePath());
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查TextIn API服务是否可用
     */
    public boolean isServiceAvailable() {
        if (!mcpEnabled) {
            return false;
        }
        
        // 检查API密钥是否配置
        if (appId == null || appId.trim().isEmpty() || 
            appSecret == null || appSecret.trim().isEmpty()) {
            log.warn("TextIn API密钥未配置，请设置ocr.textin.app.id和ocr.textin.app.secret");
            return false;
        }
        
        // 简单检查：配置完整即认为可用
        // 实际可用性需要通过真实的API调用来验证
        return true;
    }

    /**
     * 获取服务状态信息
     */
    public String getServiceStatus() {
        if (!mcpEnabled) {
            return "TextIn服务已禁用";
        }
        
        if (appId == null || appId.trim().isEmpty() || 
            appSecret == null || appSecret.trim().isEmpty()) {
            return "TextIn API配置不完整";
        }
        
        return "TextIn API服务已配置";
    }

    /**
     * 获取详细的服务状态
     */
    public Map<String, Object> getDetailedStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", mcpEnabled);
        status.put("api_url", TEXTIN_API_URL);
        status.put("timeout_ms", TIMEOUT_MS);
        status.put("app_id_configured", appId != null && !appId.trim().isEmpty());
        status.put("app_secret_configured", appSecret != null && !appSecret.trim().isEmpty());
        status.put("service_available", isServiceAvailable());
        return status;
    }
}