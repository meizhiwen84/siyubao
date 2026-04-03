package cn.laobayou.siyubao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TextInMcpService 段落格式处理测试
 */
public class TextInMcpServiceTest {

    private TextInMcpService textInMcpService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        textInMcpService = new TextInMcpService();
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(textInMcpService, "objectMapper", objectMapper);
    }

    @Test
    void testParseTextInApiResponse_WithMultipleParagraphs() throws Exception {
        // 模拟TextIn API响应，包含左右侧文字块的文本
        String mockApiResponse = "{\n" +
                "    \"code\": 200,\n" +
                "    \"message\": \"success\",\n" +
                "    \"result\": {\n" +
                "        \"width\": 600,\n" +
                "        \"height\": 400,\n" +
                "        \"lines\": [\n" +
                "            {\n" +
                "                \"text\": \"左侧第一段文字内容，用于测试OCR识别功能。\",\n" +
                "                \"position\": [50, 50, 250, 50, 250, 70, 50, 70],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"text\": \"左侧第一段包含多行文字，应该被识别为连续的段落。\",\n" +
                "                \"position\": [50, 70, 250, 70, 250, 90, 50, 90],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"text\": \"右侧第一段文字内容，与左侧有明显的间距。\",\n" +
                "                \"position\": [350, 100, 550, 100, 550, 120, 350, 120],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"text\": \"右侧第一段的第二行文字，应该与右侧第一段连接。\",\n" +
                "                \"position\": [350, 120, 550, 120, 550, 140, 350, 140],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"text\": \"左侧第二段标题文字\",\n" +
                "                \"position\": [50, 180, 150, 180, 150, 200, 50, 200],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"text\": \"右侧第二段标题下的正文内容，应该形成新的段落。\",\n" +
                "                \"position\": [350, 220, 550, 220, 550, 240, 350, 240],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        // 使用反射调用私有方法
        String result = (String) ReflectionTestUtils.invokeMethod(
                textInMcpService, "parseTextInApiResponse", mockApiResponse);

        // 验证基本结果
        assertNotNull(result, "识别结果不应为空");
        assertFalse(result.trim().isEmpty(), "识别结果不应为空字符串");
        
        // 验证段落结构
        String[] paragraphs = result.split("\\n\\n");
        
        // 验证段落分组结果
        assertTrue(paragraphs.length >= 2, "应该识别出多个段落，实际段落数: " + paragraphs.length);
        assertEquals(4, paragraphs.length, "应该识别出4个段落");
        
        // 验证左侧第一段包含两行文字但不以"22"结尾
        String leftFirstParagraph = paragraphs[0];
        assertTrue(leftFirstParagraph.contains("左侧第一段文字内容"), "左侧第一段应包含第一行文字");
        assertTrue(leftFirstParagraph.contains("左侧第一段包含多行文字"), "左侧第一段应包含第二行文字");
        assertFalse(leftFirstParagraph.endsWith("22"), "左侧第一段不应以'22'结尾");
        
        // 验证右侧第一段包含两行文字并以"22"结尾
        String rightFirstParagraph = paragraphs[1];
        assertTrue(rightFirstParagraph.contains("右侧第一段文字内容"), "右侧第一段应包含第三行文字");
        assertTrue(rightFirstParagraph.contains("右侧第一段的第二行文字"), "右侧第一段应包含第四行文字");
        assertTrue(rightFirstParagraph.endsWith("22"), "右侧第一段应以'22'结尾");
        
        // 验证左侧第二段包含标题但不以"22"结尾
        String leftSecondParagraph = paragraphs[2];
        assertTrue(leftSecondParagraph.contains("左侧第二段标题文字"), "左侧第二段应包含标题");
        assertFalse(leftSecondParagraph.endsWith("22"), "左侧第二段不应以'22'结尾");
        
        // 验证右侧第二段包含正文并以"22"结尾
        String rightSecondParagraph = paragraphs[3];
        assertTrue(rightSecondParagraph.contains("右侧第二段标题下的正文内容"), "右侧第二段应包含正文");
        assertTrue(rightSecondParagraph.endsWith("22"), "右侧第二段应以'22'结尾");
        

    }

    @Test
    void testParseTextInApiResponse_EmptyLines() throws Exception {
        // 测试空的lines数组
        String mockApiResponse = "{\n" +
                "    \"code\": 200,\n" +
                "    \"message\": \"success\",\n" +
                "    \"result\": {\n" +
                "        \"width\": 600,\n" +
                "        \"height\": 400,\n" +
                "        \"lines\": []\n" +
                "    }\n" +
                "}";

        String result = (String) ReflectionTestUtils.invokeMethod(
                textInMcpService, "parseTextInApiResponse", mockApiResponse);

        assertEquals("", result, "空的lines数组应返回空字符串");
    }

    @Test
    void testParseTextInApiResponse_ErrorResponse() {
        // 测试错误响应
        String mockApiResponse = "{\n" +
                "    \"code\": 40303,\n" +
                "    \"message\": \"文件类型不支持\"\n" +
                "}";

        Exception exception = assertThrows(Exception.class, () -> {
            ReflectionTestUtils.invokeMethod(
                    textInMcpService, "parseTextInApiResponse", mockApiResponse);
        });

        // 验证异常被正确抛出（消息可能为null，所以检查异常类型或消息内容）
        assertNotNull(exception, "应该抛出异常");
        if (exception.getMessage() != null) {
            assertTrue(exception.getMessage().contains("TextIn API调用失败") || 
                      exception.getMessage().contains("40303"), 
                      "异常消息应包含错误信息");
        }
    }

    @Test
    void testParseTextInApiResponse_WithListItems() throws Exception {
        // 测试列表项的处理
        String mockApiResponse = "{\n" +
                "    \"code\": 200,\n" +
                "    \"message\": \"success\",\n" +
                "    \"result\": {\n" +
                "        \"width\": 600,\n" +
                "        \"height\": 400,\n" +
                "        \"lines\": [\n" +
                "            {\n" +
                "                \"text\": \"列表标题：\",\n" +
                "                \"position\": [50, 50, 150, 50, 150, 70, 50, 70],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"text\": \"1. 第一个列表项\",\n" +
                "                \"position\": [50, 90, 200, 90, 200, 110, 50, 110],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"text\": \"2. 第二个列表项\",\n" +
                "                \"position\": [50, 110, 200, 110, 200, 130, 50, 130],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        String result = (String) ReflectionTestUtils.invokeMethod(
                textInMcpService, "parseTextInApiResponse", mockApiResponse);

        assertNotNull(result);
        
        // 验证列表项应该换行处理
        assertTrue(result.contains("1. 第一个列表项\n2. 第二个列表项"), 
                "列表项应该换行处理");
        

    }

    @Test
    void testParseTextInApiResponse_WithDateTimeRemoval() throws Exception {
        // 测试时间格式移除功能
        String mockApiResponse = "{\n" +
                "    \"code\": 200,\n" +
                "    \"message\": \"success\",\n" +
                "    \"result\": {\n" +
                "        \"width\": 600,\n" +
                "        \"height\": 400,\n" +
                "        \"lines\": [\n" +
                "            {\n" +
                "                \"text\": \"系统启动时间：2024-01-15 08:00:00\",\n" +
                "                \"position\": [50, 50, 300, 50, 300, 70, 50, 70],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"text\": \"用户登录：张三 于 2024-01-15 09:15:30 登录\",\n" +
                "                \"position\": [50, 80, 350, 80, 350, 100, 50, 100],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"text\": \"右侧记录：操作时间 2024-01-15 14:30:25 完成\",\n" +
                "                \"position\": [400, 150, 550, 150, 550, 170, 400, 170],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"text\": \"右侧备注：处理结束于 2024-01-15 17:45:10\",\n" +
                "                \"position\": [400, 180, 550, 180, 550, 200, 400, 200],\n" +
                "                \"direction\": 0,\n" +
                "                \"type\": \"text\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        String result = (String) ReflectionTestUtils.invokeMethod(
                textInMcpService, "parseTextInApiResponse", mockApiResponse);

        assertNotNull(result, "识别结果不应为空");
        

        
        // 验证时间格式移除逻辑：左侧移除，右侧保留
        assertFalse(result.contains("2024-01-15 08:00:00"), "应该移除左侧的时间格式");
        assertFalse(result.contains("2024-01-15 09:15:30"), "应该移除左侧的时间格式");
        assertTrue(result.contains("2024-01-15 14:30:25"), "应该保留右侧的时间格式");
        assertTrue(result.contains("2024-01-15 17:45:10"), "应该保留右侧的时间格式");
        
        // 验证其他文字内容保留
        assertTrue(result.contains("系统启动时间："), "应该保留时间标签文字");
        assertTrue(result.contains("用户登录：张三 于"), "应该保留用户信息");
        assertTrue(result.contains("登录"), "应该保留登录文字");
        
        // 验证右侧文字块仍然添加"22"
        String[] paragraphs = result.split("\n\n");
        assertTrue(paragraphs.length >= 2, "应该识别出多个段落，实际段落数: " + paragraphs.length);
        
        // 查找包含"右侧记录"的段落，应该以"22"结尾
        boolean foundRightParagraphWithSuffix = false;
        for (String paragraph : paragraphs) {
            if (paragraph.contains("右侧记录") && paragraph.endsWith("22")) {
                foundRightParagraphWithSuffix = true;
                break;
            }
        }
        assertTrue(foundRightParagraphWithSuffix, "右侧文字块应该以'22'结尾");
        
        // 验证左侧文字块不以"22"结尾
        boolean foundLeftParagraphWithoutSuffix = false;
        for (String paragraph : paragraphs) {
            if (paragraph.contains("系统启动时间") && !paragraph.endsWith("22")) {
                foundLeftParagraphWithoutSuffix = true;
                break;
            }
        }
        assertTrue(foundLeftParagraphWithoutSuffix, "左侧文字块不应该以'22'结尾");
    }
}