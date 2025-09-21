package cn.laobayou.siyubao.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 字符编码检测和转换工具类
 * 专门用于解决OCR识别中的中文乱码问题
 */
@Slf4j
public class CharacterEncodingUtils {
    
    // 支持的中文字符编码列表
    private static final List<Charset> CHINESE_CHARSETS = Arrays.asList(
        StandardCharsets.UTF_8,
        Charset.forName("GBK"),
        Charset.forName("GB2312"),
        Charset.forName("Big5"),
        Charset.forName("ISO-8859-1")
    );
    
    /**
     * 检测并修复文本的字符编码问题
     * @param text 原始文本
     * @return 修复后的文本
     */
    public static String detectAndFixEncoding(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        log.debug("开始检测字符编码，原始文本长度: {}", text.length());
        
        // 1. 检测是否包含乱码字符
        if (!containsGarbledCharacters(text)) {
            log.debug("未检测到乱码字符，直接返回原文本");
            return cleanText(text);
        }
        
        // 2. 尝试不同的编码修复策略
        String bestResult = text;
        int maxChineseCount = countChineseCharacters(text);
        
        for (Charset charset : CHINESE_CHARSETS) {
            try {
                String fixedText = tryFixWithCharset(text, charset);
                if (fixedText != null) {
                    int chineseCount = countChineseCharacters(fixedText);
                    if (chineseCount > maxChineseCount) {
                        maxChineseCount = chineseCount;
                        bestResult = fixedText;
                        log.debug("找到更好的编码: {}, 中文字符数: {}", charset.name(), chineseCount);
                    }
                }
            } catch (Exception e) {
                log.debug("尝试编码 {} 失败: {}", charset.name(), e.getMessage());
            }
        }
        
        // 3. 清理文本
        bestResult = cleanText(bestResult);
        
        log.debug("字符编码修复完成，修复前中文字符数: {}, 修复后中文字符数: {}", 
                countChineseCharacters(text), countChineseCharacters(bestResult));
        
        return bestResult;
    }
    
    /**
     * 检测是否包含乱码字符
     */
    public static boolean containsGarbledCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 检测常见的乱码模式
        return text.contains("?") || 
               text.contains("") || 
               text.matches(".*[\\uFFFD].*") || // 替换字符
               text.matches(".*[\\u0000-\\u001F].*") || // 控制字符
               text.matches(".*[\\u007F-\\u009F].*"); // 扩展控制字符
    }
    
    /**
     * 尝试使用指定字符集修复编码
     */
    private static String tryFixWithCharset(String text, Charset charset) {
        try {
            // 将文本转换为字节数组，然后使用指定字符集重新解码
            byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
            String result = new String(bytes, charset);
            
            // 检查修复后的文本是否包含更多中文字符
            if (countChineseCharacters(result) > countChineseCharacters(text)) {
                return result;
            }
            
            return null;
        } catch (Exception e) {
            log.debug("使用字符集 {} 修复失败: {}", charset.name(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 计算文本中的中文字符数量
     */
    public static int countChineseCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (char c : text.toCharArray()) {
            if (isChineseCharacter(c)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 判断是否为中文字符
     */
    public static boolean isChineseCharacter(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF) || // CJK统一汉字
               (c >= 0x3400 && c <= 0x4DBF) || // CJK扩展A
               (c >= 0x20000 && c <= 0x2A6DF) || // CJK扩展B
               (c >= 0x2A700 && c <= 0x2B73F) || // CJK扩展C
               (c >= 0x2B740 && c <= 0x2B81F) || // CJK扩展D
               (c >= 0x2B820 && c <= 0x2CEAF) || // CJK扩展E
               (c >= 0xF900 && c <= 0xFAFF) || // CJK兼容汉字
               (c >= 0x2F800 && c <= 0x2FA1F); // CJK兼容汉字补充
    }
    
    /**
     * 清理文本中的常见OCR错误，同时保留换行符和段落格式
     */
    public static String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 保留换行符，只清理其他空白字符
        // 先保护换行符
        text = text.replace("\n", "§NEWLINE§");
        text = text.replace("\r\n", "§NEWLINE§");
        text = text.replace("\r", "§NEWLINE§");
        
        // 清理常见的OCR错误（不包括换行符）
        text = text.replaceAll("[ \\t]+", " "); // 合并多个空格和制表符，但保留换行符
        text = text.replaceAll("[\\u00A0]", " "); // 替换不间断空格
        text = text.replaceAll("[\\u2000-\\u200F]", " "); // 替换各种空格字符
        text = text.replaceAll("[\\u2028-\\u202F]", " "); // 替换各种分隔符
        text = text.replaceAll("[\\u205F-\\u206F]", " "); // 替换其他Unicode空格
        text = text.replaceAll("[\\u3000]", " "); // 替换全角空格
        
        // 恢复换行符
        text = text.replace("§NEWLINE§", "\n");
        
        // 清理多余的连续换行符（保留段落分隔，但避免过多空行）
        text = text.replaceAll("\n{3,}", "\n\n"); // 最多保留两个连续换行符
        
        // 清理每行的首尾空白，但保留换行符
        String[] lines = text.split("\n");
        StringBuilder cleanedText = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String cleanedLine = lines[i].trim();
            cleanedText.append(cleanedLine);
            if (i < lines.length - 1) {
                cleanedText.append("\n");
            }
        }
        
        // 清理整体的首尾空白
        text = cleanedText.toString().trim();
        
        return text;
    }
    
    /**
     * 检测文本的主要语言类型
     */
    public static String detectLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return "unknown";
        }
        
        int chineseCount = countChineseCharacters(text);
        int totalChars = text.replaceAll("\\s", "").length();
        
        if (totalChars == 0) {
            return "unknown";
        }
        
        double chineseRatio = (double) chineseCount / totalChars;
        
        if (chineseRatio > 0.5) {
            return "chinese";
        } else if (chineseRatio > 0.1) {
            return "mixed";
        } else {
            return "english";
        }
    }
    
    /**
     * 验证文本是否为有效的UTF-8编码
     */
    public static boolean isValidUTF8(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        
        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            String decoded = new String(bytes, StandardCharsets.UTF_8);
            return decoded.equals(text);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取文本的字符编码统计信息
     */
    public static String getEncodingStats(String text) {
        if (text == null || text.isEmpty()) {
            return "文本为空";
        }
        
        int totalChars = text.length();
        int chineseChars = countChineseCharacters(text);
        int englishChars = 0;
        int digitChars = 0;
        int otherChars = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) && !isChineseCharacter(c)) {
                englishChars++;
            } else if (Character.isDigit(c)) {
                digitChars++;
            } else if (!Character.isWhitespace(c)) {
                otherChars++;
            }
        }
        
        return String.format("总字符数: %d, 中文字符: %d (%.1f%%), 英文字符: %d (%.1f%%), 数字: %d (%.1f%%), 其他: %d (%.1f%%)",
                totalChars,
                chineseChars, (double) chineseChars / totalChars * 100,
                englishChars, (double) englishChars / totalChars * 100,
                digitChars, (double) digitChars / totalChars * 100,
                otherChars, (double) otherChars / totalChars * 100);
    }
}
