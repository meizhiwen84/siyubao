package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * OCR文字识别控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    /**
     * 图片文字识别API
     * @param image 上传的图片文件
     * @return 识别结果
     */
    @PostMapping("/ocr")
    public ResponseEntity<Map<String, Object>> performOcr(@RequestParam("image") MultipartFile image) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("收到OCR请求，文件名: {}, 文件大小: {} bytes", 
                    image.getOriginalFilename(), image.getSize());
            
            // 验证文件
            if (image.isEmpty()) {
                result.put("success", false);
                result.put("message", "图片文件不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 验证文件类型
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("success", false);
                result.put("message", "请上传有效的图片文件");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 验证文件大小（限制为10MB）
            if (image.getSize() > 10 * 1024 * 1024) {
                result.put("success", false);
                result.put("message", "图片文件大小不能超过10MB");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 执行OCR识别
            String recognizedText = ocrService.recognizeText(image);
            
            if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                result.put("success", true);
                result.put("text", recognizedText.trim());
                result.put("message", "识别成功");
                log.info("OCR识别成功，识别出 {} 个字符", recognizedText.length());
            } else {
                result.put("success", false);
                result.put("text", "");
                result.put("message", "未识别到文字内容");
                log.warn("OCR识别完成但未识别到文字内容");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("OCR识别失败", e);
            result.put("success", false);
            result.put("message", "识别失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 健康检查接口
     * @return 服务状态
     */
    @GetMapping("/ocr/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean isHealthy = ocrService.isServiceHealthy();
            result.put("success", isHealthy);
            result.put("message", isHealthy ? "OCR服务正常" : "OCR服务异常");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("OCR健康检查失败", e);
            result.put("success", false);
            result.put("message", "健康检查失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 获取OCR服务统计信息
     * @return 统计信息
     */
    @GetMapping("/ocr/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = ocrService.getStatistics();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", stats);
            result.put("message", "获取统计信息成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取OCR统计信息失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 重置OCR服务统计信息
     * @return 操作结果
     */
    @PostMapping("/ocr/reset-stats")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            ocrService.resetStatistics();
            result.put("success", true);
            result.put("message", "统计信息重置成功");
            log.info("OCR统计信息已重置");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("重置OCR统计信息失败", e);
            result.put("success", false);
            result.put("message", "重置统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 强制内存清理
     * @return 操作结果
     */
    @PostMapping("/ocr/cleanup")
    public ResponseEntity<Map<String, Object>> forceCleanup() {
        Map<String, Object> result = new HashMap<>();
        try {
            ocrService.forceCleanup();
            result.put("success", true);
            result.put("message", "内存清理完成");
            log.info("OCR服务强制内存清理完成");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("OCR强制内存清理失败", e);
            result.put("success", false);
            result.put("message", "内存清理失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}