package cn.laobayou.siyubao.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CharacterEncodingUtils 测试类
 * 主要测试时间格式移除功能
 */
public class CharacterEncodingUtilsTest {

    @Test
    void testRemoveDateTime_WithSingleDateTime() {
        // 测试单个时间格式的移除
        String input = "这是一段文字 2024-01-15 14:30:25 后面还有内容";
        String expected = "这是一段文字 后面还有内容";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals(expected, result, "应该移除单个时间格式");
    }

    @Test
    void testRemoveDateTime_WithMultipleDateTimes() {
        // 测试多个时间格式的移除
        String input = "开始时间：2024-01-15 09:00:00，结束时间：2024-01-15 17:30:45，总共8小时30分钟";
        String expected = "开始时间：，结束时间：，总共8小时30分钟";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals(expected, result, "应该移除多个时间格式");
    }

    @Test
    void testRemoveDateTime_WithDateTimeAtBeginning() {
        // 测试开头的时间格式
        String input = "2024-01-15 08:00:00 这是今天的第一条记录";
        String expected = "这是今天的第一条记录";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals(expected, result, "应该移除开头的时间格式");
    }

    @Test
    void testRemoveDateTime_WithDateTimeAtEnd() {
        // 测试结尾的时间格式
        String input = "这条记录的时间是 2024-01-15 23:59:59";
        String expected = "这条记录的时间是";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals(expected, result, "应该移除结尾的时间格式");
    }

    @Test
    void testRemoveDateTime_WithOnlyDateTime() {
        // 测试只有时间格式的字符串
        String input = "2024-01-15 12:00:00";
        String expected = "";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals(expected, result, "只有时间格式的字符串应该返回空字符串");
    }

    @Test
    void testRemoveDateTime_WithNoDateTime() {
        // 测试没有时间格式的字符串
        String input = "这是一段普通的文字，没有任何时间信息";
        String expected = "这是一段普通的文字，没有任何时间信息";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals(expected, result, "没有时间格式的字符串应该保持不变");
    }

    @Test
    void testRemoveDateTime_WithSimilarButNotExactFormat() {
        // 测试相似但不完全匹配的格式
        String input = "日期：2024-1-15 时间：14:30:5 不完整格式：2024-01-15 14:30";
        String expected = "日期：2024-1-15 时间：14:30:5 不完整格式：2024-01-15 14:30";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals(expected, result, "不完全匹配的格式应该保持不变");
    }

    @Test
    void testRemoveDateTime_WithMultipleSpaces() {
        // 测试多个空格的情况
        String input = "文字内容    2024-01-15 14:30:25    更多内容";
        String expected = "文字内容 更多内容";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals(expected, result, "应该正确处理多个空格");
    }

    @Test
    void testRemoveDateTime_WithNewlines() {
        // 测试包含换行符的情况
        String input = "第一行文字\n2024-01-15 14:30:25\n第二行文字";
        String expected = "第一行文字\n第二行文字";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals(expected, result, "应该正确处理换行符");
    }

    @Test
    void testRemoveDateTime_WithNullInput() {
        // 测试null输入
        String result = CharacterEncodingUtils.removeDateTime(null);
        assertNull(result, "null输入应该返回null");
    }

    @Test
    void testRemoveDateTime_WithEmptyInput() {
        // 测试空字符串输入
        String input = "";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals("", result, "空字符串输入应该返回空字符串");
    }

    @Test
    void testRemoveDateTime_WithWhitespaceOnly() {
        // 测试只有空白字符的输入
        String input = "   \t\n   ";
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals("", result, "只有空白字符的输入应该返回空字符串");
    }

    @Test
    void testRemoveDateTime_ComplexScenario() {
        // 测试复杂场景：包含多个时间、多行文字、特殊字符等
        String input = "系统日志记录：\n" +
                      "启动时间：2024-01-15 08:00:00\n" +
                      "用户登录：张三 于 2024-01-15 09:15:30 登录系统\n" +
                      "操作记录：\n" +
                      "- 查看文档 (2024-01-15 10:30:45)\n" +
                      "- 编辑文件 (2024-01-15 11:20:15)\n" +
                      "系统关闭：2024-01-15 18:00:00";
        
        String expected = "系统日志记录：\n" +
                         "启动时间：\n" +
                         "用户登录：张三 于 登录系统\n" +
                         "操作记录：\n" +
                         "- 查看文档 ()\n" +
                         "- 编辑文件 ()\n" +
                         "系统关闭：";
        
        String result = CharacterEncodingUtils.removeDateTime(input);
        assertEquals(expected, result, "复杂场景应该正确移除所有时间格式");
    }
}