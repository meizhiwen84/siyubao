package cn.laobayou.siyubao.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 图像预处理工具类
 * 专门优化绿色背景文字的OCR识别效果
 */
@Slf4j
@Component
public class ImagePreprocessor {

    /**
     * 预处理图像以提高绿色背景文字的识别率
     * @param imageBytes 原始图像字节数组
     * @return 预处理后的图像字节数组
     * @throws IOException IO异常
     */
    public byte[] preprocessForGreenBackground(byte[] imageBytes) throws IOException {
        log.debug("开始预处理图像以优化绿色背景文字识别");
        
        // 读取原始图像
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IOException("无法读取图像数据");
        }
        
        log.debug("原始图像尺寸: {}x{}", originalImage.getWidth(), originalImage.getHeight());
        
        // 应用专门的绿色背景处理
        BufferedImage processedImage = originalImage;
        
        // 1. 绿色背景文字增强（最关键的步骤）
        processedImage = enhanceGreenBackgroundText(processedImage);
        
        // 2. 轻微的形态学操作来清理噪点
        processedImage = applyLightMorphology(processedImage);
        
        // 转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(processedImage, "PNG", baos);
        
        // 调试：保存预处理后的图片
        try {
            File debugFile = new File("debug_processed_" + System.currentTimeMillis() + ".png");
            ImageIO.write(processedImage, "PNG", debugFile);
            log.info("调试：预处理后的图片已保存到: {}", debugFile.getAbsolutePath());
        } catch (Exception e) {
            log.warn("保存调试图片失败: {}", e.getMessage());
        }
        
        log.debug("图像预处理完成");
        return baos.toByteArray();
    }



    /**
     * 智能预处理图像，自动检测背景类型并应用相应的处理算法
     * @param imageBytes 原始图像字节数组
     * @return 预处理后的图像字节数组
     * @throws IOException IO异常
     */
    public byte[] preprocessForChatBackground(byte[] imageBytes) throws IOException {
        log.debug("开始智能预处理图像，自动检测背景类型");
        
        // 读取原始图像
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IOException("无法读取图像数据");
        }
        
        // 检测背景类型
        String backgroundType = detectBackgroundType(originalImage);
        log.info("检测到背景类型: {}", backgroundType);
        
        BufferedImage processedImage;
        
        switch (backgroundType) {
            case "blue":
                log.debug("应用蓝色背景处理算法");
                processedImage = enhanceBlueBackgroundText(originalImage);
                break;
            case "green":
                log.debug("应用绿色背景处理算法");
                processedImage = enhanceGreenBackgroundText(originalImage);
                break;
            default:
                log.debug("应用通用处理算法");
                processedImage = enhanceGeneralText(originalImage);
                break;
        }
        
        // 轻微的形态学操作来清理噪点
        processedImage = applyLightMorphology(processedImage);
        
        // 转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(processedImage, "PNG", baos);
        
        // 调试：保存预处理后的图片
        try {
            String debugFileName = "debug_" + backgroundType + "_processed_" + System.currentTimeMillis() + ".png";
            File debugFile = new File(debugFileName);
            ImageIO.write(processedImage, "PNG", debugFile);
            log.info("调试：{}背景预处理后的图片已保存到: {}", backgroundType, debugFile.getAbsolutePath());
        } catch (Exception e) {
            log.warn("保存调试图片失败: {}", e.getMessage());
        }
        
        log.debug("智能图像预处理完成");
        return baos.toByteArray();
    }
    
    /**
     * 专门处理绿色背景，增强文字对比度（优化版本，保护中文字符）
     */
    private BufferedImage enhanceGreenBackgroundText(BufferedImage image) {
        log.debug("应用优化的绿色背景文字增强算法，保护中文字符");
        
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel = new Color(image.getRGB(x, y));
                int r = pixel.getRed();
                int g = pixel.getGreen();
                int b = pixel.getBlue();
                
                // 计算灰度值
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                
                // 非常保守的绿色背景检测，只处理明显的绿色背景
                boolean isObviousGreenBg = isObviousGreenBackground(r, g, b);
                
                if (isObviousGreenBg) {
                    // 明显的绿色背景：转换为白色
                    result.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    // 对于非绿色背景的像素，使用自适应阈值保护中文字符
                    int threshold = calculateAdaptiveThreshold(image, x, y, 15);
                    
                    if (gray < threshold) {
                        // 较暗的像素，很可能是文字，保持黑色
                        result.setRGB(x, y, Color.BLACK.getRGB());
                    } else if (gray > 240) {
                        // 非常亮的颜色，保持白色
                        result.setRGB(x, y, Color.WHITE.getRGB());
                    } else {
                        // 中等亮度的像素，使用更温和的处理
                        // 检查是否为中文字符区域（通过周围像素判断）
                        if (isLikelyChineseCharacterArea(image, x, y)) {
                            // 中文字符区域，保持原色特征
                            result.setRGB(x, y, image.getRGB(x, y));
                        } else {
                            // 非中文字符区域，轻微增强对比度
                            int newR, newG, newB;
                            
                            if (gray < 128) {
                                // 偏暗的像素，轻微变暗
                                double factor = 0.9; // 更温和的处理
                                newR = (int) Math.max(0, r * factor);
                                newG = (int) Math.max(0, g * factor);
                                newB = (int) Math.max(0, b * factor);
                            } else {
                                // 偏亮的像素，轻微变亮
                                double factor = 1.1; // 更温和的处理
                                newR = (int) Math.min(255, r * factor);
                                newG = (int) Math.min(255, g * factor);
                                newB = (int) Math.min(255, b * factor);
                            }
                            
                            result.setRGB(x, y, new Color(newR, newG, newB).getRGB());
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 增强图像对比度（专门用于蓝底白字）
     */
    private BufferedImage enhanceContrastForBlueText(BufferedImage image, float factor) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, image.getType());
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // 应用对比度增强
                r = Math.min(255, Math.max(0, (int)((r - 128) * factor + 128)));
                g = Math.min(255, Math.max(0, (int)((g - 128) * factor + 128)));
                b = Math.min(255, Math.max(0, (int)((b - 128) * factor + 128)));
                
                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        
        return result;
    }
    
    /**
     * 边缘锐化（专门用于提高文字清晰度）
     */
    private BufferedImage sharpenTextEdges(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, image.getType());
        
        // 锐化卷积核
        float[] sharpenKernel = {
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0
        };
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float sum = 0;
                int index = 0;
                
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = image.getRGB(x + kx, y + ky);
                        int gray = (pixel >> 16) & 0xFF; // 取红色通道作为灰度值
                        sum += gray * sharpenKernel[index++];
                    }
                }
                
                int newValue = Math.min(255, Math.max(0, (int)sum));
                result.setRGB(x, y, (newValue << 16) | (newValue << 8) | newValue);
            }
        }
        
        // 处理边界像素
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    result.setRGB(x, y, image.getRGB(x, y));
                }
            }
        }
        
        return result;
    }
    
    /**
     * 应用形态学操作（专门用于清理噪点）
     */
    private BufferedImage applyMorphologyOperation(BufferedImage image, int kernelSize) {
        // 先腐蚀后膨胀（开运算）来去除噪点
        BufferedImage eroded = applyErosion(image, kernelSize);
        return applyDilation(eroded, kernelSize);
    }

    /**
     * 检测图像的背景类型
     */
    private String detectBackgroundType(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        int bluePixels = 0;
        int greenPixels = 0;
        int totalSamples = 0;
        
        // 采样检测背景类型
        int stepX = Math.max(1, width / 20);
        int stepY = Math.max(1, height / 20);
        
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                if (isObviousBlueBackground(r, g, b)) {
                    bluePixels++;
                } else if (isObviousGreenBackground(r, g, b)) {
                    greenPixels++;
                }
                totalSamples++;
            }
        }
        
        double blueRatio = (double) bluePixels / totalSamples;
        double greenRatio = (double) greenPixels / totalSamples;
        
        log.debug("背景检测结果 - 蓝色比例: {:.2f}, 绿色比例: {:.2f}", blueRatio, greenRatio);
        
        if (blueRatio > 0.15) {
            return "blue";
        } else if (greenRatio > 0.15) {
            return "green";
        } else {
            return "general";
        }
    }

    /**
     * 通用文字增强处理
     */
    private BufferedImage enhanceGeneralText(BufferedImage image) {
        log.debug("应用通用文字增强算法");
        
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // 计算灰度值
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                
                // 使用自适应阈值
                int threshold = calculateAdaptiveThreshold(image, x, y, 15);
                
                if (gray < threshold - 20) {
                    // 明显的文字像素
                    result.setRGB(x, y, Color.BLACK.getRGB());
                } else if (gray > threshold + 30) {
                    // 明显的背景像素
                    result.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    // 边界像素，使用更细致的判断
                    if (gray < threshold) {
                        result.setRGB(x, y, Color.BLACK.getRGB());
                    } else {
                        result.setRGB(x, y, Color.WHITE.getRGB());
                    }
                }
            }
        }
        
        return result;
     }
     
     /**
     * 判断是否为可能的中文字符区域
     */
    private boolean isLikelyChineseCharacterArea(BufferedImage image, int centerX, int centerY) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // 检查3x3区域内的像素变化
        int darkPixels = 0;
        int totalPixels = 0;
        
        for (int y = Math.max(0, centerY - 1); y <= Math.min(height - 1, centerY + 1); y++) {
            for (int x = Math.max(0, centerX - 1); x <= Math.min(width - 1, centerX + 1); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                
                if (gray < 128) {
                    darkPixels++;
                }
                totalPixels++;
            }
        }
        
        // 如果区域内有适中的暗像素比例，可能是中文字符
        double darkRatio = (double) darkPixels / totalPixels;
        return darkRatio > 0.2 && darkRatio < 0.8; // 20%-80%的暗像素比例
    }
    
    /**
     * 计算颜色与绿色的相似度
     */
    private double calculateGreenSimilarity(int r, int g, int b) {
        // 微信绿色参考值 (149, 236, 105)
        int refR = 149, refG = 236, refB = 105;
        
        // 计算欧几里得距离
        double distance = Math.sqrt(Math.pow(r - refR, 2) + Math.pow(g - refG, 2) + Math.pow(b - refB, 2));
        
        // 最大可能距离（从黑到白）
        double maxDistance = Math.sqrt(3 * Math.pow(255, 2));
        
        // 相似度 = 1 - (距离 / 最大距离)
        return 1.0 - (distance / maxDistance);
    }
    
    /**
     * 判断是否为纯绿色背景（更严格的检测）
     */
    private boolean isPureGreenBackground(int r, int g, int b) {
        // 微信绿色的RGB值大约是 (149, 236, 105)，即 #95ec69
        // 使用更严格的条件来检测纯绿色背景
        
        // 检查是否接近微信绿色
        int wechatR = 149, wechatG = 236, wechatB = 105;
        double distance = Math.sqrt(Math.pow(r - wechatR, 2) + Math.pow(g - wechatG, 2) + Math.pow(b - wechatB, 2));
        
        // 如果颜色距离微信绿色很近（阈值30），认为是纯绿色背景
        if (distance < 30) {
            return true;
        }
        
        // 检查是否为其他绿色背景变种
        // 绿色分量必须明显高于红色和蓝色，且在合理范围内
        boolean isGreenDominant = (g > r + 60) && (g > b + 60) && (g >= 200);
        
        // HSV检测作为辅助
        float[] hsv = new float[3];
        Color.RGBtoHSB(r, g, b, hsv);
        float hue = hsv[0] * 360;
        float saturation = hsv[1];
        float brightness = hsv[2];
        
        // 严格的HSV绿色检测
        boolean isHsvGreen = (hue >= 100 && hue <= 120) && 
                            (saturation >= 0.5) && 
                            (brightness >= 0.7);
        
        return isGreenDominant && isHsvGreen;
    }
    
    /**
     * 检测是否为明显的绿色背景（调整后支持微信绿色）
     */
    private boolean isObviousGreenBackground(int r, int g, int b) {
        // 微信绿色 RGB(149, 236, 105) 的特征：
        // 1. 绿色分量明显高于红色和蓝色分量
        // 2. 绿色分量要足够高
        // 3. 放宽红色和蓝色分量的限制以支持微信绿色
        return g > r + 60 && g > b + 100 && g > 200 && r < 180 && b < 130;
    }
    
    /**
     * 检测是否为明显的蓝色背景（优化聊天截图识别）
     */
    private boolean isObviousBlueBackground(int r, int g, int b) {
        // 专门检测聊天应用蓝色 #4696F6 (70, 150, 246) 及类似颜色
        
        // 1. 精确匹配聊天蓝色 #4696F6 及其变体（容差范围）
        boolean isChatBlue = isChatBlueBackground(r, g, b);
        
        // 2. 基本蓝色条件：蓝色分量占主导（降低阈值）
        boolean basicBlue = b > r + 20 && b > g + 10 && b > 100;
        
        // 3. 计算蓝色占比（降低阈值）
        float blueRatio = (float) b / (r + g + b);
        boolean blueRatioDominant = blueRatio > 0.35;
        
        // 4. HSV检测作为辅助（放宽条件）
        float[] hsv = new float[3];
        Color.RGBtoHSB(r, g, b, hsv);
        float hue = hsv[0] * 360;
        float saturation = hsv[1];
        float brightness = hsv[2];
        
        // 蓝色色相范围：180-280度（扩大范围）
        boolean hsvBlue = (hue >= 180 && hue <= 280) && 
                         (saturation >= 0.2) && 
                         (brightness >= 0.3);
        
        // 5. 任何偏蓝的像素
        boolean anyBlueish = b > Math.max(r, g) && b > 80;
        
        // 6. 相对蓝色（蓝色分量相对较高）
        boolean relativeBlue = b > (r + g) / 2 + 20 && b > 60;
        
        // 7. 新增：强蓝色检测（针对高饱和度蓝色）
        boolean strongBlue = b > 200 && b > r + 50 && b > g + 50;
        
        boolean isBlue = isChatBlue || basicBlue || (blueRatioDominant && hsvBlue) || anyBlueish || relativeBlue || strongBlue;
        
        // 添加调试输出
        if (Math.random() < 0.001) {
            log.debug("蓝色背景检测 - RGB({},{},{}) 蓝色占比:{:.2f} HSV({:.0f},{:.2f},{:.2f}) 结果:{} [聊天蓝:{} 基本蓝:{} HSV蓝:{} 任意蓝:{} 相对蓝:{} 强蓝:{}]", 
                     r, g, b, blueRatio, hue, saturation, brightness, isBlue,
                     isChatBlue, basicBlue, hsvBlue, anyBlueish, relativeBlue, strongBlue);
        }
        
        return isBlue;
    }
    
    /**
     * 专门检测聊天应用的蓝色背景 #4696F6 (70, 150, 246) 及其变体
     */
    private boolean isChatBlueBackground(int r, int g, int b) {
        // #4696F6 的RGB值：(70, 150, 246)
        // 允许一定的容差范围来匹配相似的蓝色
        
        // 方法1：直接范围匹配（最精确）
        boolean directMatch = (r >= 50 && r <= 90) &&     // 红色分量 70±20
                             (g >= 130 && g <= 170) &&    // 绿色分量 150±20  
                             (b >= 220 && b <= 255);      // 蓝色分量 246±9
        
        // 方法2：比例匹配（更灵活）
        // #4696F6 的比例特征：蓝色远大于绿色，绿色大于红色
        boolean proportionMatch = b > g + 80 &&           // 蓝色比绿色大80以上
                                 g > r + 60 &&            // 绿色比红色大60以上
                                 b > 200 &&               // 蓝色分量足够高
                                 r < 100;                 // 红色分量相对较低
        
        // 方法3：相对差值匹配
        int bgDiff = b - g;  // 蓝绿差值，#4696F6中约为96
        int grDiff = g - r;  // 绿红差值，#4696F6中约为80
        boolean relativeMatch = bgDiff >= 70 && bgDiff <= 120 &&  // 蓝绿差值在合理范围
                               grDiff >= 60 && grDiff <= 100 &&   // 绿红差值在合理范围
                               b > 180;                           // 蓝色分量足够高
        
        boolean isChatBlue = directMatch || proportionMatch || relativeMatch;
        
        // 特殊调试输出（针对聊天蓝色）
        if (isChatBlue && Math.random() < 0.01) { // 1%概率输出聊天蓝色检测结果
            log.debug("聊天蓝色检测 - RGB({},{},{}) 蓝绿差:{} 绿红差:{} 结果:{} [直接:{} 比例:{} 相对:{}]", 
                     r, g, b, bgDiff, grDiff, isChatBlue, directMatch, proportionMatch, relativeMatch);
        }
        
        return isChatBlue;
    }
    
    /**
     * 检测是否为明显的红色背景
     */
    private boolean isObviousRedBackground(int r, int g, int b) {
        // 红色分量必须大幅高于绿色和蓝色分量
        return r > g + 60 && r > b + 60 && r > 180 && g < 100 && b < 100;
    }
    
    /**
     * 检测是否为任何明显的彩色背景
     */
    public boolean hasColoredBackground(byte[] imageBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            return false;
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        // 采样检测：检查图像中的多个点
        int sampleCount = 0;
        int coloredBackgroundCount = 0;
        
        // 检查边缘区域（通常是背景）
        for (int y = 0; y < height; y += Math.max(1, height / 20)) {
            for (int x = 0; x < width; x += Math.max(1, width / 20)) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                if (isObviousGreenBackground(r, g, b) || 
                    isObviousBlueBackground(r, g, b) || 
                    isObviousRedBackground(r, g, b)) {
                    coloredBackgroundCount++;
                }
                sampleCount++;
            }
        }
        
        // 如果超过30%的采样点是彩色背景，则认为是彩色背景图像
        double coloredRatio = (double) coloredBackgroundCount / sampleCount;
        boolean hasColored = coloredRatio > 0.3;
        
        log.debug("彩色背景检测结果: 采样点={}, 彩色背景点={}, 比例={:.2f}, 结果={}", 
                sampleCount, coloredBackgroundCount, coloredRatio, hasColored);
        
        return hasColored;
    }
    
    /**
     * 通用彩色背景预处理
     * @param imageBytes 原始图像字节数组
     * @return 预处理后的图像字节数组
     * @throws IOException IO异常
     */
    public byte[] preprocessForColoredBackground(byte[] imageBytes) throws IOException {
        log.debug("开始通用彩色背景预处理");
        
        // 读取原始图像
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IOException("无法读取图像数据");
        }
        
        log.debug("原始图像尺寸: {}x{}", originalImage.getWidth(), originalImage.getHeight());
        
        // 应用通用彩色背景处理
        BufferedImage processedImage = originalImage;
        
        // 1. 彩色背景文字增强（核心步骤）
        processedImage = enhanceColoredBackgroundText(processedImage);
        
        // 2. 轻微的形态学操作来清理噪点
        processedImage = applyLightMorphology(processedImage);
        
        // 转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(processedImage, "PNG", baos);
        
        // 调试：保存预处理后的图片
        try {
            File debugFile = new File("debug_colored_processed_" + System.currentTimeMillis() + ".png");
            ImageIO.write(processedImage, "PNG", debugFile);
            log.info("调试：彩色背景预处理后的图片已保存到: {}", debugFile.getAbsolutePath());
        } catch (Exception e) {
            log.warn("保存调试图片失败: {}", e.getMessage());
        }
        
        log.debug("通用彩色背景预处理完成");
        return baos.toByteArray();
    }
    
    /**
     * 增强彩色背景上的文字（通用方法）
     */
    private BufferedImage enhanceColoredBackgroundText(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // 检测是否为彩色背景
                boolean isColoredBg = isObviousGreenBackground(r, g, b) || 
                                     isObviousBlueBackground(r, g, b) || 
                                     isObviousRedBackground(r, g, b);
                
                if (isColoredBg) {
                    // 彩色背景区域：转为白色
                    result.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    // 非彩色背景区域：判断是否为文字
                    // 计算亮度
                    int brightness = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    
                    // 检查是否为白色或接近白色（聊天界面的文字通常是白色）
                    boolean isWhiteText = r > 240 && g > 240 && b > 240;
                    
                    // 检查是否为深色文字
                    boolean isDarkText = brightness < 100;
                    
                    // 检查是否为灰色文字（中等亮度）
                    boolean isGrayText = brightness >= 100 && brightness <= 180 && 
                                        Math.abs(r - g) < 30 && Math.abs(g - b) < 30 && Math.abs(r - b) < 30;
                    
                    if (isDarkText || isGrayText) {
                        // 深色或灰色文字：转为黑色
                        result.setRGB(x, y, Color.BLACK.getRGB());
                    } else if (isWhiteText) {
                        // 白色文字：保持黑色（反转）
                        result.setRGB(x, y, Color.BLACK.getRGB());
                    } else {
                        // 其他颜色：根据亮度判断
                        if (brightness < 150) {
                            result.setRGB(x, y, Color.BLACK.getRGB());
                        } else {
                            result.setRGB(x, y, Color.WHITE.getRGB());
                        }
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 判断是否为绿色背景（原有方法，用于整体检测）
     */
    private boolean isGreenBackground(int r, int g, int b) {
        // 微信绿色的RGB值大约是 (149, 236, 105)，即 #95ec69
        // 检测绿色范围，更精确地匹配微信绿色
        
        // 基本绿色条件：绿色分量明显大于红色和蓝色
        boolean basicGreen = (g > r + 30) && (g > b + 30) && (g > 120);
        
        // 微信绿色特征检测
        boolean isWechatGreen = false;
        if (g >= 180 && g <= 255) { // 绿色分量较高
            float greenRatio = (float) g / (r + g + b);
            isWechatGreen = greenRatio > 0.45; // 绿色占比超过45%
        }
        
        // HSV检测作为辅助
        float[] hsv = new float[3];
        Color.RGBtoHSB(r, g, b, hsv);
        float hue = hsv[0] * 360;
        float saturation = hsv[1];
        float brightness = hsv[2];
        
        boolean hsvGreen = (hue >= 90 && hue <= 130) && 
                          (saturation >= 0.4 && saturation <= 0.8) && 
                          (brightness >= 0.5 && brightness <= 0.95);
        
        return basicGreen || isWechatGreen || hsvGreen;
    }

    /**
     * 智能绿色背景检测（更保守的方法，避免误判文字）
     */
    private boolean isSmartGreenBackground(int r, int g, int b) {
        // 微信绿色的RGB值大约是 (149, 236, 105)，即 #95ec69
        // 使用更严格的条件来避免误判文字
        
        // 1. 首先检查是否接近微信绿色
        int wechatR = 149, wechatG = 236, wechatB = 105;
        double distance = Math.sqrt(Math.pow(r - wechatR, 2) + Math.pow(g - wechatG, 2) + Math.pow(b - wechatB, 2));
        
        // 如果颜色距离微信绿色很近（阈值40），认为是绿色背景
        if (distance < 40) {
            return true;
        }
        
        // 2. 检查是否为高饱和度的绿色（避免低饱和度的绿色文字）
        float[] hsv = new float[3];
        Color.RGBtoHSB(r, g, b, hsv);
        float hue = hsv[0] * 360;
        float saturation = hsv[1];
        float brightness = hsv[2];
        
        // 严格的HSV绿色检测：高饱和度、适中亮度、绿色色相
        boolean isHighSaturationGreen = (hue >= 100 && hue <= 120) && 
                                       (saturation >= 0.6) && // 提高饱和度要求
                                       (brightness >= 0.7 && brightness <= 0.95);
        
        // 3. RGB检测：绿色分量必须显著高于红蓝分量，且绿色值较高
        boolean isRgbGreen = (g > r + 50) && (g > b + 50) && (g >= 200);
        
        // 4. 排除可能的文字颜色（通常饱和度较低或亮度极端）
        boolean isNotText = saturation >= 0.5 && brightness >= 0.6 && brightness <= 0.9;
        
        return (isHighSaturationGreen || isRgbGreen) && isNotText;
    }
    
    /**
     * 增强图像对比度
     */
    private BufferedImage enhanceContrast(BufferedImage image) {
        log.debug("应用对比度增强");
        
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // 计算对比度增强参数
        double contrast = 1.5; // 对比度因子
        double brightness = 10; // 亮度调整
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel = new Color(image.getRGB(x, y));
                
                int r = (int) Math.max(0, Math.min(255, contrast * pixel.getRed() + brightness));
                int g = (int) Math.max(0, Math.min(255, contrast * pixel.getGreen() + brightness));
                int b = (int) Math.max(0, Math.min(255, contrast * pixel.getBlue() + brightness));
                
                result.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        
        return result;
    }
    
    /**
     * 应用自适应阈值处理
     */
    private BufferedImage applyAdaptiveThreshold(BufferedImage image) {
        log.debug("应用自适应阈值处理");
        
        // 先转换为灰度图
        BufferedImage grayImage = convertToGrayscale(image);
        
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        int windowSize = 15; // 自适应窗口大小
        double c = 10; // 常数调整值
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 计算局部区域的平均值
                double localMean = calculateLocalMean(grayImage, x, y, windowSize);
                
                // 获取当前像素的灰度值
                int grayValue = new Color(grayImage.getRGB(x, y)).getRed();
                
                // 自适应阈值判断
                int binaryValue = (grayValue > localMean - c) ? 255 : 0;
                
                result.setRGB(x, y, new Color(binaryValue, binaryValue, binaryValue).getRGB());
            }
        }
        
        return result;
    }
    
    /**
     * 转换为灰度图
     */
    private BufferedImage convertToGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel = new Color(image.getRGB(x, y));
                int gray = (int) (0.299 * pixel.getRed() + 0.587 * pixel.getGreen() + 0.114 * pixel.getBlue());
                grayImage.setRGB(x, y, new Color(gray, gray, gray).getRGB());
            }
        }
        
        return grayImage;
    }
    
    /**
     * 计算局部区域的平均值
     */
    private double calculateLocalMean(BufferedImage image, int centerX, int centerY, int windowSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int halfWindow = windowSize / 2;
        
        int sum = 0;
        int count = 0;
        
        for (int y = Math.max(0, centerY - halfWindow); y <= Math.min(height - 1, centerY + halfWindow); y++) {
            for (int x = Math.max(0, centerX - halfWindow); x <= Math.min(width - 1, centerX + halfWindow); x++) {
                sum += new Color(image.getRGB(x, y)).getRed();
                count++;
            }
        }
        
        return count > 0 ? (double) sum / count : 0;
    }
    
    /**
     * 应用轻量级形态学操作
     */
    private BufferedImage applyLightMorphology(BufferedImage image) {
        log.debug("应用轻量级形态学操作");
        
        // 只应用轻微的闭运算来连接字符断裂部分
        return applyClosing(image, 1);
    }
    
    /**
     * 应用形态学操作（去噪和字符增强）
     */
    private BufferedImage applyMorphologicalOperations(BufferedImage image) {
        log.debug("应用形态学操作");
        
        // 应用开运算（先腐蚀后膨胀）去除小噪点
        BufferedImage opened = applyOpening(image, 1);
        
        // 应用闭运算（先膨胀后腐蚀）连接字符断裂部分
        BufferedImage closed = applyClosing(opened, 1);
        
        return closed;
    }
    
    /**
     * 开运算：先腐蚀后膨胀
     */
    private BufferedImage applyOpening(BufferedImage image, int kernelSize) {
        BufferedImage eroded = applyErosion(image, kernelSize);
        return applyDilation(eroded, kernelSize);
    }
    
    /**
     * 闭运算：先膨胀后腐蚀
     */
    private BufferedImage applyClosing(BufferedImage image, int kernelSize) {
        BufferedImage dilated = applyDilation(image, kernelSize);
        return applyErosion(dilated, kernelSize);
    }
    
    /**
     * 腐蚀操作
     */
    private BufferedImage applyErosion(BufferedImage image, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int minValue = 255;
                
                // 检查核心区域
                for (int ky = -kernelSize; ky <= kernelSize; ky++) {
                    for (int kx = -kernelSize; kx <= kernelSize; kx++) {
                        int nx = x + kx;
                        int ny = y + ky;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int value = new Color(image.getRGB(nx, ny)).getRed();
                            minValue = Math.min(minValue, value);
                        }
                    }
                }
                
                result.setRGB(x, y, new Color(minValue, minValue, minValue).getRGB());
            }
        }
        
        return result;
    }
    
    /**
     * 膨胀操作
     */
    private BufferedImage applyDilation(BufferedImage image, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int maxValue = 0;
                
                // 检查核心区域
                for (int ky = -kernelSize; ky <= kernelSize; ky++) {
                    for (int kx = -kernelSize; kx <= kernelSize; kx++) {
                        int nx = x + kx;
                        int ny = y + ky;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int value = new Color(image.getRGB(nx, ny)).getRed();
                            maxValue = Math.max(maxValue, value);
                        }
                    }
                }
                
                result.setRGB(x, y, new Color(maxValue, maxValue, maxValue).getRGB());
            }
        }
        
        return result;
    }
    
    /**
     * 检测图像是否包含绿色背景
     * @param imageBytes 图像字节数组
     * @return 是否包含绿色背景
     */
    public boolean hasGreenBackground(byte[] imageBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            return false;
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        int greenPixelCount = 0;
        int totalPixels = width * height;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel = new Color(image.getRGB(x, y));
                if (isSmartGreenBackground(pixel.getRed(), pixel.getGreen(), pixel.getBlue())) {
                    greenPixelCount++;
                }
            }
        }
        
        // 如果绿色背景像素超过15%，认为是绿色背景图像（降低阈值）
        double greenRatio = (double) greenPixelCount / totalPixels;
        log.debug("绿色背景像素比例: {:.2f}%", greenRatio * 100);
        
        return greenRatio > 0.15;
    }

    /**
     * 检测图像是否包含蓝色背景
     * @param imageBytes 图像字节数组
     * @return 是否包含蓝色背景
     */
    public boolean hasBlueBackground(byte[] imageBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            return false;
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        int bluePixelCount = 0;
        int totalPixels = width * height;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel = new Color(image.getRGB(x, y));
                if (isObviousBlueBackground(pixel.getRed(), pixel.getGreen(), pixel.getBlue())) {
                    bluePixelCount++;
                }
            }
        }
        
        // 如果蓝色背景像素超过15%，认为是蓝色背景图像
        double blueRatio = (double) bluePixelCount / totalPixels;
        log.debug("蓝色背景像素比例: {:.2f}%", blueRatio * 100);
        
        return blueRatio > 0.15;
    }

    /**
     * 专门针对蓝色背景的预处理
     * @param imageBytes 原始图像字节数组
     * @return 预处理后的图像字节数组
     * @throws IOException IO异常
     */
    public byte[] preprocessForBlueBackground(byte[] imageBytes) throws IOException {
        log.debug("开始蓝色背景预处理");
        
        // 读取原始图像
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IOException("无法读取图像数据");
        }
        
        log.debug("原始图像尺寸: {}x{}", originalImage.getWidth(), originalImage.getHeight());
        
        // 应用专门的蓝色背景处理
        BufferedImage processedImage = originalImage;
        
        // 1. 如果图像太大，先缩放到合适尺寸（提高OCR效率和准确性）
        if (processedImage.getWidth() > 1500 || processedImage.getHeight() > 1500) {
            processedImage = scaleImageForOCR(processedImage);
            log.debug("图像已缩放到: {}x{}", processedImage.getWidth(), processedImage.getHeight());
        }
        
        // 2. 跳过复杂的预处理，直接使用原始图像
        // 让Tesseract自己处理，避免过度预处理导致的识别错误
        log.debug("跳过复杂预处理，使用原始图像进行OCR");
        
        // 转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(processedImage, "PNG", baos);
        
        // 调试：保存预处理后的图片
        try {
            File debugFile = new File("debug_blue_processed_" + System.currentTimeMillis() + ".png");
            ImageIO.write(processedImage, "PNG", debugFile);
            log.info("调试：蓝色背景预处理后的图片已保存到: {}", debugFile.getAbsolutePath());
        } catch (Exception e) {
            log.warn("保存调试图片失败: {}", e.getMessage());
        }
        
        log.debug("蓝色背景预处理完成");
        return baos.toByteArray();
    }
    
    /**
     * 缩放图像到适合OCR的尺寸
     */
    private BufferedImage scaleImageForOCR(BufferedImage image) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        
        // 计算缩放比例，保持宽高比
        double scale = Math.min(1200.0 / originalWidth, 1200.0 / originalHeight);
        if (scale >= 1.0) {
            return image; // 不需要缩放
        }
        
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);
        
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        
        // 使用高质量缩放
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return scaledImage;
    }

    /**
     * 专门处理蓝色背景，增强文字对比度（修复版本，正确处理蓝底白字）
     */
    private BufferedImage enhanceBlueBackgroundText(BufferedImage image) {
        log.debug("应用修复的蓝色背景文字增强算法，正确处理蓝底白字");
        
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // 使用轻度图像增强
        BufferedImage enhancedImage = enhanceImageContrastGentle(image);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = enhancedImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // 检测是否在蓝色背景区域
                boolean isInBlueArea = isObviousBlueBackground(r, g, b);
                
                if (isInBlueArea) {
                    // 在蓝色背景区域内，需要区分背景和白字
                    boolean isWhiteText = isWhiteTextOnBlue(r, g, b);
                    
                    if (isWhiteText) {
                        // 蓝底上的白字 -> 转换为黑字
                        result.setRGB(x, y, Color.BLACK.getRGB());
                    } else {
                        // 纯蓝色背景 -> 转换为白色背景
                        result.setRGB(x, y, Color.WHITE.getRGB());
                    }
                } else {
                    // 非蓝色区域，使用自适应阈值处理
                    int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    
                    // 使用自适应阈值，提高文字识别准确性
                    int adaptiveThreshold = calculateChatThreshold(enhancedImage, x, y, 15);
                    
                    // 对于聊天文本，使用更敏感的阈值
                    if (gray < adaptiveThreshold - 10) {
                        // 明显的文字像素
                        result.setRGB(x, y, Color.BLACK.getRGB());
                    } else if (gray > adaptiveThreshold + 20) {
                        // 明显的背景像素
                        result.setRGB(x, y, Color.WHITE.getRGB());
                    } else {
                        // 边界像素，根据周围像素决定
                        boolean isTextLikely = isTextPixelLikely(enhancedImage, x, y, 3);
                        if (isTextLikely) {
                            result.setRGB(x, y, Color.BLACK.getRGB());
                        } else {
                            result.setRGB(x, y, Color.WHITE.getRGB());
                        }
                    }
                }
            }
        }
        
        // 应用轻度形态学操作，清理噪点但保留文字结构
        result = applyLightMorphology(result);
        
        return result;
    }
    
    /**
     * 检测是否为蓝色背景上的白色文字（专门针对聊天蓝色#4696F6优化）
     */
    private boolean isWhiteTextOnBlue(int r, int g, int b) {
        // 1. 首先检查是否为聊天蓝色背景上的白字
        boolean isChatWhiteText = isWhiteTextOnChatBlue(r, g, b);
        
        // 2. 通用白字检测（保留原有逻辑）
        // 计算亮度
        int brightness = (int) (0.299 * r + 0.587 * g + 0.114 * b);
        
        // 计算RGB的标准差，判断是否均衡
        double mean = (r + g + b) / 3.0;
        double variance = ((r - mean) * (r - mean) + (g - mean) * (g - mean) + (b - mean) * (b - mean)) / 3.0;
        double stdDev = Math.sqrt(variance);
        
        // 检查是否偏向蓝色（大幅放宽限制）
        boolean notBlueBiased = b <= r + 60 && b <= g + 60;
        
        // 极度敏感的多层次检测（大幅降低所有阈值）：
        
        // 1. 高亮度白字（接近纯白）- 降低阈值
        boolean pureWhiteText = brightness > 150 && stdDev < 40 && Math.min(Math.min(r, g), b) > 120;
        
        // 2. 中等亮度白字（稍微偏灰的白字）- 大幅降低阈值
        boolean grayishWhiteText = brightness > 100 && brightness <= 150 && 
                                  stdDev < 60 && Math.min(Math.min(r, g), b) > 70 && notBlueBiased;
        
        // 3. 相对白字（相对于蓝色背景较亮的像素）- 大幅降低阈值
        boolean relativeWhiteText = brightness > 80 && 
                                   Math.min(Math.min(r, g), b) > 50 && 
                                   notBlueBiased && 
                                   (r + g) > b; // 红绿分量之和大于蓝色分量
        
        // 4. 浅色文字（更低的阈值）- 进一步降低
        boolean lightText = brightness > 70 && 
                           Math.max(Math.max(r, g), b) - Math.min(Math.min(r, g), b) < 80 && // 颜色相对均衡
                           notBlueBiased &&
                           (r + g + b) > 200; // 总亮度足够
        
        // 5. 极低阈值检测（针对非常暗的白字）
        boolean veryLightText = brightness > 60 && 
                               (r > 50 || g > 50) && // 至少红色或绿色分量不太低
                               notBlueBiased &&
                               Math.abs(r - g) < 50; // 红绿分量相对接近
        
        // 6. 任何相对较亮的非蓝色像素
        boolean anyLighterPixel = brightness > 50 && 
                                 (r + g) > b + 10 && // 红绿分量之和略大于蓝色
                                 Math.max(Math.max(r, g), b) > 60; // 至少有一个分量不太低
        
        boolean isWhite = isChatWhiteText || pureWhiteText || grayishWhiteText || relativeWhiteText || lightText || veryLightText || anyLighterPixel;
        
        // 添加调试输出
        if (Math.random() < 0.001) {
            log.debug("白字检测 - RGB({},{},{}) 亮度:{} 标准差:{:.1f} 非蓝偏:{} 结果:{} [聊天白:{} 纯白:{} 灰白:{} 相对白:{} 浅色:{} 极浅:{} 任意亮:{}]", 
                     r, g, b, brightness, stdDev, notBlueBiased, isWhite,
                     isChatWhiteText, pureWhiteText, grayishWhiteText, relativeWhiteText, lightText, veryLightText, anyLighterPixel);
        }
        
        return isWhite;
    }
    
    /**
     * 专门检测聊天蓝色背景上的白字文本
     * 针对 #4696F6 (70, 150, 246) 背景优化
     */
    private boolean isWhiteTextOnChatBlue(int r, int g, int b) {
        // 聊天应用中白字的特征：
        // 1. 通常是纯白色或接近白色
        // 2. RGB值相对均衡且较高
        // 3. 与蓝色背景形成强烈对比
        
        // 方法1：纯白色检测（最严格）
        boolean pureWhite = r >= 240 && g >= 240 && b >= 240 && 
                           Math.abs(r - g) <= 10 && Math.abs(g - b) <= 10 && Math.abs(r - b) <= 10;
        
        // 方法2：接近白色检测（稍宽松）
        boolean nearWhite = r >= 220 && g >= 220 && b >= 220 && 
                           Math.abs(r - g) <= 20 && Math.abs(g - b) <= 20 && Math.abs(r - b) <= 20;
        
        // 方法3：浅色文字检测（更宽松，包括浅灰色）
        boolean lightGray = r >= 180 && g >= 180 && b >= 180 && 
                           Math.abs(r - g) <= 30 && Math.abs(g - b) <= 30 && Math.abs(r - b) <= 30;
        
        // 方法4：相对亮色检测（最宽松）
        // 相对于蓝色背景 #4696F6 (70, 150, 246) 明显更亮的像素
        boolean relativeBright = (r > 120 || g > 180 || (r > 100 && g > 160)) && 
                                b < 200 && // 蓝色分量不能太高（避免误判蓝色）
                                (r + g) > b; // 红绿之和大于蓝色
        
        // 方法5：高对比度检测
        // 与典型聊天蓝色形成高对比度的像素
        int contrastWithChatBlue = Math.abs(r - 70) + Math.abs(g - 150) + Math.abs(b - 246);
        boolean highContrast = contrastWithChatBlue > 300 && // 与聊天蓝色差异很大
                              (r > 100 || g > 100) && // 至少有一个分量较高
                              b < Math.max(r, g) + 50; // 蓝色分量不占绝对优势
        
        // 方法6：极度宽松检测（针对任何可能的白字）
        boolean veryLoose = (r > 80 || g > 80) && // 至少红色或绿色分量不太低
                           b < r + g && // 蓝色分量小于红绿之和
                           (r + g + b) > 180; // 总亮度足够
        
        boolean isChatWhite = pureWhite || nearWhite || lightGray || relativeBright || highContrast || veryLoose;
        
        // 特殊调试输出（针对聊天白字）
        if (isChatWhite && Math.random() < 0.01) { // 1%概率输出聊天白字检测结果
            log.debug("聊天白字检测 - RGB({},{},{}) 对比度:{} 结果:{} [纯白:{} 近白:{} 浅灰:{} 相对亮:{} 高对比:{} 极宽松:{}]", 
                     r, g, b, contrastWithChatBlue, isChatWhite, pureWhite, nearWhite, lightGray, relativeBright, highContrast, veryLoose);
        }
        
        return isChatWhite;
    }
    
    /**
     * 判断像素是否可能是文字的一部分
     */
    private boolean isTextPixelLikely(BufferedImage image, int centerX, int centerY, int radius) {
        int width = image.getWidth();
        int height = image.getHeight();
        int darkPixelCount = 0;
        int totalPixels = 0;
        
        for (int y = Math.max(0, centerY - radius); y <= Math.min(height - 1, centerY + radius); y++) {
            for (int x = Math.max(0, centerX - radius); x <= Math.min(width - 1, centerX + radius); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                
                if (gray < 140) { // 较暗的像素
                    darkPixelCount++;
                }
                totalPixels++;
            }
        }
        
        // 如果周围有足够多的暗像素，则认为当前像素可能是文字
        return (double) darkPixelCount / totalPixels > 0.3;
    }
    
    /**
     * 保守的图像对比度增强，避免过度处理
     */
    private BufferedImage enhanceImageContrastConservative(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // 使用最保守的对比度增强参数
        double contrast = 1.1; // 极低的对比度增强
        double brightness = 2; // 极低的亮度调整
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel = new Color(image.getRGB(x, y));
                
                int r = (int) Math.max(0, Math.min(255, contrast * pixel.getRed() + brightness));
                int g = (int) Math.max(0, Math.min(255, contrast * pixel.getGreen() + brightness));
                int b = (int) Math.max(0, Math.min(255, contrast * pixel.getBlue() + brightness));
                
                result.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        
        return result;
    }
    
    /**
     * 温和的图像对比度增强，适合聊天界面
     */
    private BufferedImage enhanceImageContrastGentle(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // 使用更温和的对比度增强参数
        double contrast = 1.2; // 降低对比度增强强度
        double brightness = 5; // 降低亮度调整
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel = new Color(image.getRGB(x, y));
                
                int r = (int) Math.max(0, Math.min(255, contrast * pixel.getRed() + brightness));
                int g = (int) Math.max(0, Math.min(255, contrast * pixel.getGreen() + brightness));
                int b = (int) Math.max(0, Math.min(255, contrast * pixel.getBlue() + brightness));
                
                result.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        
        return result;
    }
    

    
    /**
     * 聊天界面专用的阈值计算
     */
    private int calculateChatThreshold(BufferedImage image, int centerX, int centerY, int windowSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int halfWindow = windowSize / 2;
        
        int sum = 0;
        int count = 0;
        
        // 计算局部区域的平均亮度
        for (int y = Math.max(0, centerY - halfWindow); y <= Math.min(height - 1, centerY + halfWindow); y++) {
            for (int x = Math.max(0, centerX - halfWindow); x <= Math.min(width - 1, centerX + halfWindow); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                sum += gray;
                count++;
            }
        }
        
        if (count == 0) return 140; // 提高默认阈值
        
        int mean = sum / count;
        
        // 使用更宽松的阈值，确保聊天文字不被误判
        return Math.max(120, mean - 10); // 提高最小阈值
    }
    
    /**
     * 增强图像对比度，提高文字清晰度
     */
    private BufferedImage enhanceImageContrast(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // 计算图像直方图
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                histogram[gray]++;
            }
        }
        
        // 计算累积分布函数
        int[] cdf = new int[256];
        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i-1] + histogram[i];
        }
        
        // 直方图均衡化
        int totalPixels = width * height;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                
                int newGray = (cdf[gray] * 255) / totalPixels;
                
                // 应用对比度增强
                newGray = Math.max(0, Math.min(255, (newGray - 128) * 2 + 128));
                
                result.setRGB(x, y, new Color(newGray, newGray, newGray).getRGB());
            }
        }
        
        return result;
    }
    
    /**
     * 计算自适应阈值，保护中文字符细节
     */
    private int calculateAdaptiveThreshold(BufferedImage image, int centerX, int centerY, int windowSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int halfWindow = windowSize / 2;
        
        int sum = 0;
        int count = 0;
        
        // 计算局部区域的平均亮度
        for (int y = Math.max(0, centerY - halfWindow); y <= Math.min(height - 1, centerY + halfWindow); y++) {
            for (int x = Math.max(0, centerX - halfWindow); x <= Math.min(width - 1, centerX + halfWindow); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                sum += gray;
                count++;
            }
        }
        
        if (count == 0) return 128; // 默认阈值
        
        int mean = sum / count;
        
        // 使用更保守的阈值，保护中文字符的细节
        return Math.max(100, mean - 20); // 确保阈值不会太低
    }
    
    /**
     * 计算颜色饱和度
     */
    private double calculateSaturation(int r, int g, int b) {
        int max = Math.max(Math.max(r, g), b);
        int min = Math.min(Math.min(r, g), b);
        if (max == 0) return 0;
        return (double)(max - min) / max;
    }
    
    /**
     * 清理噪声的形态学操作
     */
    private BufferedImage cleanupNoise(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage cleaned = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        // 使用3x3核进行形态学开运算（先腐蚀后膨胀）
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int blackCount = 0;
                int totalCount = 0;
                
                // 检查3x3邻域
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int pixel = image.getRGB(nx, ny) & 0xFF;
                            if (pixel < 128) blackCount++;
                            totalCount++;
                        }
                    }
                }
                
                // 如果邻域中黑色像素比例足够高，保留黑色
                if (blackCount >= 3) {
                    cleaned.setRGB(x, y, 0x000000);
                } else {
                    cleaned.setRGB(x, y, 0xFFFFFF);
                }
            }
        }
        
        return cleaned;
    }
}