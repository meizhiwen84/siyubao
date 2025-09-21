package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.util.CharacterEncodingUtils;
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
            JsonNode linesNode = result.path("lines");
            if (!linesNode.isArray() || linesNode.size() == 0) {
                log.debug("TextIn API响应中没有识别到文本行");
                return "";
            }
            
            // 解析文本行并构建段落结构
            List<TextLine> textLines = new ArrayList<>();
            for (JsonNode lineNode : linesNode) {
                String lineText = lineNode.path("text").asText("").trim();
                if (!lineText.isEmpty()) {
                    TextLine textLine = new TextLine();
                    textLine.text = lineText;
                    textLine.position = parsePosition(lineNode.path("position"));
                    textLine.direction = lineNode.path("direction").asInt(0);
                    textLine.type = lineNode.path("type").asText("text");
                    textLines.add(textLine);
                }
            }
            
            if (textLines.isEmpty()) {
                return "";
            }
            
            // 按照Y坐标排序（从上到下）
            textLines.sort((a, b) -> Integer.compare(a.position.topY, b.position.topY));
            
            // 基于背景区域的文字块分组和合并
            StringBuilder result_text = new StringBuilder();
            List<List<TextLine>> visualRegions = groupByVisualRegion(textLines);
            
            // 将同一视觉区域的文字块合并为单行文本输出
            for (int i = 0; i < visualRegions.size(); i++) {
                List<TextLine> region = visualRegions.get(i);
                
                // 合并同一区域内的文字块，确保连贯性
                StringBuilder regionText = new StringBuilder();
                for (int j = 0; j < region.size(); j++) {
                    TextLine line = region.get(j);
                    String lineText = line.text.trim();
                    
                    // 应用字符编码清理
                    lineText = CharacterEncodingUtils.cleanText(lineText);
                    
                    if (!lineText.isEmpty()) {
                        if (regionText.length() > 0) {
                            // 智能添加分隔符，确保文字连贯性
                            if (needsSpaceSeparator(regionText.toString(), lineText)) {
                                regionText.append(" ");
                            }
                        }
                        regionText.append(lineText);
                    }
                }
                
                String finalRegionText = regionText.toString().trim();
                if (!finalRegionText.isEmpty()) {
                    result_text.append(finalRegionText);
                    
                    // 不同视觉区域之间用换行符分隔
                    if (i < visualRegions.size() - 1) {
                        result_text.append("\n");
                    }
                }
            }
            
            String finalText = result_text.toString();
            
            log.debug("TextIn API识别结果，文本行数: {}, 总长度: {}", 
                    textLines.size(), finalText.length());
            
            return finalText;
            
        } catch (Exception e) {
            log.error("解析TextIn API响应失败: {}", e.getMessage());
            throw new Exception("解析API响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 文本行数据结构
     */
    private static class TextLine {
        String text;
        Position position;
        int direction;
        String type;
    }
    
    /**
     * 位置信息数据结构
     */
    private static class Position {
        int topY;
        int bottomY;
        int leftX;
        int rightX;
        int height;
    }
    
    /**
     * 解析位置信息
     */
    private Position parsePosition(JsonNode positionNode) {
        Position pos = new Position();
        if (positionNode.isArray() && positionNode.size() >= 8) {
            // position是8个整数的数组，表示四边形的四个顶点坐标
            // [x1,y1,x2,y2,x3,y3,x4,y4] 左上角开始顺时针
            int x1 = positionNode.get(0).asInt();
            int y1 = positionNode.get(1).asInt();
            int x2 = positionNode.get(2).asInt();
            int y2 = positionNode.get(3).asInt();
            int x3 = positionNode.get(4).asInt();
            int y3 = positionNode.get(5).asInt();
            int x4 = positionNode.get(6).asInt();
            int y4 = positionNode.get(7).asInt();
            
            // 找到最小和最大的x、y坐标
            pos.leftX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
            pos.rightX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
            pos.topY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
            pos.bottomY = Math.max(Math.max(y1, y2), Math.max(y3, y4));
            pos.height = pos.bottomY - pos.topY;
        }
        return pos;
    }
    
    /**
     * 将文本行分组为段落
     */
    private List<List<TextLine>> groupIntoParagraphs(List<TextLine> textLines) {
        List<List<TextLine>> paragraphs = new ArrayList<>();
        if (textLines.isEmpty()) {
            return paragraphs;
        }
        
        // 计算图片的中心X坐标，用于区分左右侧
        int minX = textLines.stream().mapToInt(line -> line.position.leftX).min().orElse(0);
        int maxX = textLines.stream().mapToInt(line -> line.position.rightX).max().orElse(0);
        int centerX = (minX + maxX) / 2;
        
        List<TextLine> currentParagraph = new ArrayList<>();
        currentParagraph.add(textLines.get(0));
        
        for (int i = 1; i < textLines.size(); i++) {
            TextLine currentLine = textLines.get(i);
            TextLine previousLine = textLines.get(i - 1);
            
            // 计算行间距
            double lineSpacing = currentLine.position.topY - previousLine.position.bottomY;
            double averageHeight = (previousLine.position.height + currentLine.position.height) / 2.0;
            double threshold = averageHeight * 1.5;
            
            // 判断当前行和前一行是否在同一侧（左侧或右侧）
            boolean previousIsRightSide = previousLine.position.leftX > centerX;
            boolean currentIsRightSide = currentLine.position.leftX > centerX;
            boolean sameSide = previousIsRightSide == currentIsRightSide;
            
            // 判断是否应该开始新段落
            // 1. 行间距大于平均行高的1.5倍
            // 2. 或者文本类型不同（如从文本到印章）
            // 3. 或者不在同一侧（左右侧切换）且有一定的垂直间距
            boolean shouldStartNewParagraph = lineSpacing > threshold || 
                                            !currentLine.type.equals(previousLine.type) ||
                                            (!sameSide && lineSpacing > 0);
            
            if (shouldStartNewParagraph) {
                paragraphs.add(currentParagraph);
                currentParagraph = new ArrayList<>();
            }
            
            currentParagraph.add(currentLine);
        }
        
        // 添加最后一个段落
        if (!currentParagraph.isEmpty()) {
            paragraphs.add(currentParagraph);
        }
        
        return paragraphs;
    }
    
    /**
     * 判断是否应该在两行之间换行 - 基于句子完整性
     */
    private boolean shouldBreakLine(String currentText, String nextText) {
        // 清理文本，去除首尾空白
        currentText = currentText.trim();
        nextText = nextText.trim();
        
        // 1. 强制换行的情况：当前行以完整句子结束符结尾
        if (currentText.matches(".*[。！？]$")) {
            return true;
        }
        
        // 2. 当前行以分号、冒号结尾，通常表示一个完整的语义单元
        if (currentText.matches(".*[；：]$")) {
            return true;
        }
        
        // 3. 当前行以逗号结尾，但下一行开头是新的主语或明显的新句子开始
        if (currentText.matches(".*[，]$")) {
            // 下一行以常见的句子开头词开始
            if (nextText.matches("^(但是|然而|因此|所以|而且|并且|同时|另外|此外|总之|最后|首先|其次|再次|然后|接着|于是|不过|可是|只是|如果|假如|虽然|尽管|由于|因为|为了|通过|根据|按照|依据|基于).*")) {
                return true;
            }
            // 下一行以人名、地名等专有名词开始（通常是大写字母或常见姓氏）
            if (nextText.matches("^[A-Z].*") || nextText.matches("^(张|李|王|刘|陈|杨|赵|黄|周|吴|徐|孙|胡|朱|高|林|何|郭|马|罗|梁|宋|郑|谢|韩|唐|冯|于|董|萧|程|曹|袁|邓|许|傅|沈|曾|彭|吕|苏|卢|蒋|蔡|贾|丁|魏|薛|叶|阎|余|潘|杜|戴|夏|钟|汪|田|任|姜|范|方|石|姚|谭|廖|邹|熊|金|陆|郝|孔|白|崔|康|毛|邱|秦|江|史|顾|侯|邵|孟|龙|万|段|漕|钱|汤|尹|黎|易|常|武|乔|贺|赖|龚|文).*")) {
                return true;
            }
        }
        
        // 4. 下一行以明显的新段落标志开始
        if (nextText.matches("^[0-9]+[、．.].*") || // 数字编号
            nextText.matches("^[一二三四五六七八九十]+[、．.].*") || // 中文数字编号
            nextText.matches("^[（(][0-9]+[）)].*") || // 括号编号
            nextText.matches("^[①②③④⑤⑥⑦⑧⑨⑩].*")) { // 圆圈数字
            return true;
        }
        
        // 5. 当前行很短且下一行不是明显的延续
        if (currentText.length() <= 8) {
            // 如果下一行不是以连接词开始，可能当前行是独立的短句或标题
            if (!nextText.matches("^(的|了|着|过|在|与|和|或|及|以及|等|等等).*")) {
                return true;
            }
        }
        
        // 6. 当前行很长，可能已经是完整句子
        if (currentText.length() >= 50) {
            // 如果不是以连接性标点结尾，可能是完整句子
            if (!currentText.matches(".*[，、]$")) {
                return true;
            }
        }
        
        // 7. 检查语义完整性：当前行包含主谓宾结构且下一行开始新的主语
        if (containsCompleteSubjectPredicate(currentText) && startsWithNewSubject(nextText)) {
            return true;
        }
        
        // 默认不换行，继续构建句子
        return false;
    }
    
    /**
     * 检查文本是否包含相对完整的主谓结构
     */
    private boolean containsCompleteSubjectPredicate(String text) {
        // 简单的启发式判断：包含动词且长度适中
        return text.length() >= 6 && 
               (text.matches(".*[是有在做说去来看听想要能会可应该必须].*") ||
                text.matches(".*[了着过].*"));
    }
    
    /**
     * 检查文本是否以新的主语开始
     */
    private boolean startsWithNewSubject(String text) {
        // 以人称代词、名词或专有名词开始
        return text.matches("^(我|你|他|她|它|我们|你们|他们|她们|它们|这|那|这个|那个|这些|那些|大家|人们|公司|政府|学校|医院|银行|商店).*") ||
               text.matches("^[A-Z].*") || // 英文专有名词
               text.matches("^[张李王刘陈杨赵黄周吴徐孙胡朱高林何郭马罗梁宋郑谢韩唐冯于董萧程曹袁邓许傅沈曾彭吕苏卢蒋蔡贾丁魏薛叶阎余潘杜戴夏钟汪田任姜范方石姚谭廖邹熊金陆郝孔白崔康毛邱秦江史顾侯邵孟龙万段漕钱汤尹黎易常武乔贺赖龚文].*"); // 常见姓氏
    }

    /**
     * 基于视觉区域对文字块进行分组
     * 同一背景区域的文字块会被分到同一组
     */
    private List<List<TextLine>> groupByVisualRegion(List<TextLine> textLines) {
        List<List<TextLine>> regions = new ArrayList<>();
        if (textLines.isEmpty()) {
            return regions;
        }
        
        // 计算文字块的平均高度，用于判断是否在同一行
        double avgHeight = textLines.stream()
                .mapToInt(line -> line.position.height)
                .average()
                .orElse(20.0);
        
        List<TextLine> currentRegion = new ArrayList<>();
        currentRegion.add(textLines.get(0));
        
        for (int i = 1; i < textLines.size(); i++) {
            TextLine current = textLines.get(i);
            TextLine previous = textLines.get(i - 1);
            
            // 判断是否属于同一视觉区域
            if (isSameVisualRegion(previous, current, avgHeight)) {
                currentRegion.add(current);
            } else {
                // 开始新的视觉区域
                regions.add(new ArrayList<>(currentRegion));
                currentRegion.clear();
                currentRegion.add(current);
            }
        }
        
        // 添加最后一个区域
        if (!currentRegion.isEmpty()) {
            regions.add(currentRegion);
        }
        
        return regions;
    }
    
    /**
     * 判断两个文字块是否属于同一视觉区域
     */
    private boolean isSameVisualRegion(TextLine line1, TextLine line2, double avgHeight) {
        // 垂直距离阈值：如果两个文字块的垂直距离小于平均高度的1.5倍，认为在同一区域
        double verticalThreshold = avgHeight * 1.5;
        double verticalDistance = Math.abs(line1.position.topY - line2.position.topY);
        
        // 水平重叠检查：如果有水平重叠或距离很近，更可能是同一区域
        boolean hasHorizontalOverlap = !(line1.position.rightX < line2.position.leftX || 
                                       line2.position.rightX < line1.position.leftX);
        
        double horizontalDistance = hasHorizontalOverlap ? 0 : 
            Math.min(Math.abs(line1.position.rightX - line2.position.leftX),
                    Math.abs(line2.position.rightX - line1.position.leftX));
        
        // 水平距离阈值：字符宽度的3倍
        double horizontalThreshold = avgHeight * 3;
        
        // 同一视觉区域的条件：
        // 1. 垂直距离小于阈值
        // 2. 有水平重叠或水平距离小于阈值
        return verticalDistance < verticalThreshold && 
               (hasHorizontalOverlap || horizontalDistance < horizontalThreshold);
    }
    
    /**
     * 判断两个文本片段之间是否需要空格分隔符
     */
    private boolean needsSpaceSeparator(String text1, String text2) {
        if (text1.isEmpty() || text2.isEmpty()) {
            return false;
        }
        
        char lastChar = text1.charAt(text1.length() - 1);
        char firstChar = text2.charAt(0);
        
        // 中文字符之间通常不需要空格
        if (isChinese(lastChar) && isChinese(firstChar)) {
            return false;
        }
        
        // 标点符号后不需要空格
        if (isPunctuation(lastChar)) {
            return false;
        }
        
        // 标点符号前不需要空格
        if (isPunctuation(firstChar)) {
            return false;
        }
        
        // 数字和字母之间可能需要空格
        if (Character.isLetterOrDigit(lastChar) && Character.isLetterOrDigit(firstChar)) {
            return true;
        }
        
        // 中英文混合时需要空格
        if ((isChinese(lastChar) && Character.isLetterOrDigit(firstChar)) ||
            (Character.isLetterOrDigit(lastChar) && isChinese(firstChar))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 判断字符是否为中文字符
     */
    private boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FFF;
    }
    
    /**
     * 判断字符是否为标点符号
     */
    private boolean isPunctuation(char c) {
        String chinesePunctuation = "，。！？；：\u201c\u201d\u2018\u2019（）【】《》、";
        String englishPunctuation = ",.!?;:\"'()[]<>/\\";
        return chinesePunctuation.indexOf(c) >= 0 || englishPunctuation.indexOf(c) >= 0;
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