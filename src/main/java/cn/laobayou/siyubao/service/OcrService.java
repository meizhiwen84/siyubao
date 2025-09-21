package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.util.ImagePreprocessor;
import cn.laobayou.siyubao.util.CharacterEncodingUtils;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OCR文字识别服务
 */
@Slf4j
@Service
public class OcrService {

    @Autowired
    private ImagePreprocessor imagePreprocessor;

    @Autowired
    private TextInMcpService textInMcpService;

    @Value("${ocr.engine:textin}")
    private String ocrEngine; // "tesseract" 或 "textin"

    private ITesseract tesseract;
    
    // 线程池用于隔离OCR操作
    private ExecutorService ocrExecutor;
    
    // 统计信息
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastSuccessTime = new AtomicLong(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    
    // 配置参数
    private static final int OCR_TIMEOUT_SECONDS = 30;
    private static final int MAX_CONCURRENT_OCR = 2;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_IMAGE_DIMENSION = 4000;

    @PostConstruct
    public void init() {
        try {
            // 初始化线程池
            ocrExecutor = new ThreadPoolExecutor(
                1, // 核心线程数
                MAX_CONCURRENT_OCR, // 最大线程数
                60L, TimeUnit.SECONDS, // 空闲线程存活时间
                new LinkedBlockingQueue<>(10), // 任务队列
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "OCR-Worker-" + threadNumber.getAndIncrement());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
            );
            
            // 配置Tesseract库路径
            configureLibraryPath();
            
            // 初始化Tesseract实例
            initializeTesseract();
            
            log.info("OCR服务初始化成功，线程池大小: {}", MAX_CONCURRENT_OCR);
        } catch (Exception e) {
            log.error("OCR服务初始化失败", e);
            tesseract = null; // 标记服务不可用
        }
    }
    
    /**
     * 初始化Tesseract实例
     */
    private void initializeTesseract() {
        try {
            tesseract = new Tesseract();
            
            // 设置语言为中文和英文
            tesseract.setLanguage("chi_sim+eng");
            
            // 设置OCR引擎模式 (LSTM OCR Engine)
            tesseract.setOcrEngineMode(1);
            
            // 设置页面分割模式 (Uniform block of text)
            tesseract.setPageSegMode(6);
            
            // 设置变量以提高稳定性
            tesseract.setVariable("user_defined_dpi", "300");
            // 移除字符白名单限制，允许识别所有字符
            // tesseract.setVariable("tessedit_char_whitelist", "...");
            
            log.info("Tesseract实例初始化成功");
        } catch (Exception e) {
            log.error("Tesseract实例初始化失败，尝试英文模式", e);
            // 如果中文语言包不存在，尝试只使用英文
            try {
                tesseract = new Tesseract();
                tesseract.setLanguage("eng");
                tesseract.setOcrEngineMode(1);
                tesseract.setPageSegMode(1);
                tesseract.setVariable("user_defined_dpi", "300");
                log.warn("中文语言包不可用，已切换到英文模式");
            } catch (Exception e2) {
                log.error("Tesseract实例初始化完全失败", e2);
                throw new RuntimeException("无法初始化Tesseract", e2);
            }
        }
    }
    
    /**
     * 清理资源
     */
    @PreDestroy
    public void destroy() {
        if (ocrExecutor != null && !ocrExecutor.isShutdown()) {
            log.info("正在关闭OCR线程池...");
            ocrExecutor.shutdown();
            try {
                if (!ocrExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("OCR线程池未能在10秒内正常关闭，强制关闭");
                    ocrExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("等待OCR线程池关闭时被中断", e);
                ocrExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("OCR服务资源清理完成");
    }

    /**
     * 配置Tesseract库路径
     */
    private void configureLibraryPath() {
        try {
            // 检测操作系统
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("mac")) {
                // macOS配置
                String libraryPath = "/opt/homebrew/lib";
                System.setProperty("jna.library.path", libraryPath);
                log.info("已配置macOS Tesseract库路径: {}", libraryPath);
                
                // 设置tessdata路径
                String tessdataPath = "/opt/homebrew/share/tessdata";
                System.setProperty("TESSDATA_PREFIX", tessdataPath);
                log.info("已配置tessdata路径: {}", tessdataPath);
            } else if (osName.contains("linux")) {
                // Linux配置
                System.setProperty("jna.library.path", "/usr/local/lib:/usr/lib");
                System.setProperty("TESSDATA_PREFIX", "/usr/share/tessdata");
                log.info("已配置Linux Tesseract库路径");
            } else if (osName.contains("windows")) {
                // Windows配置
                log.info("Windows系统，使用默认库路径配置");
            }
        } catch (Exception e) {
            log.warn("配置Tesseract库路径时出现异常", e);
        }
    }

    /**
     * 识别图片中的文字（支持多种OCR引擎）
     * @param imageFile 图片文件
     * @return 识别出的文字
     * @throws IOException IO异常
     * @throws TesseractException OCR异常
     */
    public String recognizeText(MultipartFile imageFile) throws IOException, TesseractException {
        long startTime = System.currentTimeMillis();
        log.info("开始OCR识别，引擎: {}, 文件名: {}, 文件大小: {} bytes", 
                ocrEngine, imageFile.getOriginalFilename(), imageFile.getSize());
        
        // 根据配置选择OCR引擎
        if ("textin".equalsIgnoreCase(ocrEngine)) {
            return recognizeTextWithTextIn(imageFile);
        } else {
            return recognizeTextWithTesseract(imageFile);
        }
    }

    /**
     * 使用TextIn MCP进行文字识别
     */
    private String recognizeTextWithTextIn(MultipartFile imageFile) throws IOException, TesseractException {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("使用TextIn MCP进行文字识别");
            String result = textInMcpService.recognizeText(imageFile);
            
            // 记录成功统计
            successCount.incrementAndGet();
            lastSuccessTime.set(System.currentTimeMillis());
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("TextIn MCP识别成功完成，耗时: {} ms，结果长度: {}", 
                    duration, result != null ? result.length() : 0);
            
            return result;
            
        } catch (Exception e) {
            log.error("TextIn MCP识别失败: {}", e.getMessage(), e);
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new TesseractException("TextIn MCP识别失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 使用Tesseract进行文字识别（原有方法）
     */
    private String recognizeTextWithTesseract(MultipartFile imageFile) throws IOException, TesseractException {
        long startTime = System.currentTimeMillis();
        log.info("使用Tesseract进行文字识别（进程隔离）");
        
        // 检查服务状态
        if (ocrExecutor == null || ocrExecutor.isShutdown()) {
            log.error("OCR服务未正确初始化或已关闭");
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            throw new RuntimeException("OCR服务不可用");
        }

        // 验证文件大小
        if (imageFile.getSize() > MAX_FILE_SIZE) {
            log.error("图片文件过大: {} bytes，限制: {} bytes", imageFile.getSize(), MAX_FILE_SIZE);
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            throw new IOException("图片文件过大，请上传小于10MB的图片");
        }

        // 验证文件类型
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.error("不支持的文件类型: {}", contentType);
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            throw new IOException("不支持的文件类型，请上传图片文件");
        }

        // 在独立线程中执行进程隔离的OCR识别
        Future<String> future = ocrExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return recognizeTextWithProcess(imageFile);
            }
        });

        try {
            log.info("等待OCR识别结果，超时时间: {} 秒", OCR_TIMEOUT_SECONDS);
            String result = future.get(OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // 记录成功统计
            successCount.incrementAndGet();
            lastSuccessTime.set(System.currentTimeMillis());
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("OCR识别成功完成，耗时: {} ms，结果长度: {}", 
                    duration, result != null ? result.length() : 0);
            
            if (result != null && result.trim().isEmpty()) {
                log.warn("OCR识别结果为空");
            }
            
            return result;
            
        } catch (TimeoutException e) {
            log.error("OCR识别超时，取消任务");
            future.cancel(true);
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            throw new TesseractException("OCR识别超时，请尝试上传更小的图片", e);
            
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.error("OCR识别执行异常: {}", cause.getMessage(), cause);
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            
            if (cause instanceof TesseractException) {
                throw (TesseractException) cause;
            } else if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new TesseractException("OCR识别失败: " + cause.getMessage(), cause);
            }
            
        } catch (InterruptedException e) {
            log.error("OCR识别被中断");
            future.cancel(true);
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            Thread.currentThread().interrupt();
            throw new TesseractException("OCR识别被中断", e);
        }
    }

    /**
     * 使用外部进程进行OCR识别（避免JVM崩溃）
     */
    private String recognizeTextWithProcess(MultipartFile imageFile) throws IOException, TesseractException {
        File tempImageFile = null;
        File tempOutputFile = null;
        
        try {
            // 创建临时图片文件
            String originalFilename = imageFile.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
            
            tempImageFile = File.createTempFile("ocr_input_", extension);
            tempOutputFile = File.createTempFile("ocr_output_", ".txt");
            
            log.debug("创建临时文件: 输入={}, 输出={}", tempImageFile.getAbsolutePath(), tempOutputFile.getAbsolutePath());
            
            // 获取原始图像数据
            byte[] originalImageBytes = imageFile.getBytes();
            
            // 检测是否为彩色背景图像并进行预处理
            byte[] processedImageBytes = originalImageBytes;
            try {
                if (imagePreprocessor.hasBlueBackground(originalImageBytes)) {
                    log.info("检测到蓝色背景图像，应用专门的蓝色背景预处理算法");
                    processedImageBytes = imagePreprocessor.preprocessForBlueBackground(originalImageBytes);
                } else if (imagePreprocessor.hasGreenBackground(originalImageBytes)) {
                    log.info("检测到绿色背景图像，应用专门的绿色背景预处理算法");
                    processedImageBytes = imagePreprocessor.preprocessForGreenBackground(originalImageBytes);
                } else if (imagePreprocessor.hasColoredBackground(originalImageBytes)) {
                    log.info("检测到彩色背景图像，应用通用彩色背景预处理算法");
                    processedImageBytes = imagePreprocessor.preprocessForColoredBackground(originalImageBytes);
                } else {
                    log.debug("未检测到彩色背景，使用原始图像");
                }
            } catch (Exception e) {
                log.warn("图像预处理失败，使用原始图像: {}", e.getMessage());
                processedImageBytes = originalImageBytes;
            }
            
            // 保存预处理后的图片到临时文件
            try (FileOutputStream fos = new FileOutputStream(tempImageFile)) {
                fos.write(processedImageBytes);
            }
            
            // 验证图片尺寸
            BufferedImage image = ImageIO.read(tempImageFile);
            if (image == null) {
                throw new IOException("无法读取图片文件，请确保文件格式正确");
            }
            
            log.info("图片解析成功，尺寸: {}x{}", image.getWidth(), image.getHeight());
            
            if (image.getWidth() > MAX_IMAGE_DIMENSION || image.getHeight() > MAX_IMAGE_DIMENSION) {
                throw new IOException("图片尺寸过大，请上传小于4000x4000像素的图片");
            }
            
            // 构建tesseract命令
            List<String> command = new ArrayList<>();
            command.add("tesseract");
            command.add(tempImageFile.getAbsolutePath());
            command.add(tempOutputFile.getAbsolutePath().replace(".txt", "")); // tesseract会自动添加.txt
            command.add("-l");
            command.add("chi_sim+eng");
            command.add("--oem");
            command.add("1");
            command.add("--psm");
            command.add("6"); // 单一文本块模式，适合聊天截图中的独立文本区域
            
            // 优化的OCR配置参数，提高对聊天截图的识别准确性
            command.add("-c");
            command.add("tessedit_char_blacklist="); // 清空字符黑名单
            command.add("-c");
            command.add("preserve_interword_spaces=1"); // 保留词间空格
            command.add("-c");
            command.add("user_defined_dpi=300");
            command.add("-c");
            command.add("tessedit_pageseg_mode=6"); // 确保使用单一文本块模式
            command.add("-c");
            command.add("tessedit_ocr_engine_mode=1"); // 使用LSTM引擎
            command.add("-c");
            command.add("load_system_dawg=0"); // 禁用系统词典，提高对非标准文本的识别
            command.add("-c");
            command.add("load_freq_dawg=0"); // 禁用频率词典
            command.add("-c");
            command.add("textord_really_old_xheight=1"); // 改进行高检测 // 设置DPI提高识别精度
            
            log.debug("执行tesseract命令: {}", String.join(" ", command));
            
            // 执行tesseract命令
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 读取命令输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // 等待进程完成
            boolean finished = process.waitFor(OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new TesseractException("OCR进程超时");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("tesseract进程异常退出，退出码: {}, 输出: {}", exitCode, output.toString());
                throw new TesseractException("OCR识别失败，退出码: " + exitCode);
            }
            
            // 读取识别结果
            if (!tempOutputFile.exists()) {
                log.warn("OCR输出文件不存在，可能识别结果为空");
                return "";
            }
            
            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tempOutputFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }
            
            String resultText = result.toString().trim();
            log.debug("OCR识别完成，结果长度: {}", resultText.length());
            
            // 字符编码修复：确保中文字符正确显示
            if (resultText != null && !resultText.isEmpty()) {
                // 使用专门的字符编码工具类进行修复
                resultText = CharacterEncodingUtils.detectAndFixEncoding(resultText);
                log.debug("字符编码修复后，结果长度: {}, 语言类型: {}", 
                        resultText.length(), CharacterEncodingUtils.detectLanguage(resultText));
            }
            
            return resultText;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TesseractException("OCR进程被中断", e);
        } finally {
            // 清理临时文件
            if (tempImageFile != null && tempImageFile.exists()) {
                try {
                    Files.delete(tempImageFile.toPath());
                    log.debug("删除临时输入文件: {}", tempImageFile.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("删除临时输入文件失败: {}", e.getMessage());
                }
            }
            
            if (tempOutputFile != null && tempOutputFile.exists()) {
                try {
                    Files.delete(tempOutputFile.toPath());
                    log.debug("删除临时输出文件: {}", tempOutputFile.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("删除临时输出文件失败: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 创建Tesseract实例（线程安全）
     */
    private ITesseract createTesseractInstance() throws Exception {
        ITesseract instance = new Tesseract();
        
        // 复制主实例的配置
        if (tesseract != null) {
            try {
                // 获取当前语言设置
                String language = "chi_sim+eng"; // 默认语言
                instance.setLanguage(language);
                instance.setOcrEngineMode(1);
                instance.setPageSegMode(1);
                instance.setVariable("user_defined_dpi", "300");
                
                log.debug("创建Tesseract实例成功，语言: {}", language);
            } catch (Exception e) {
                log.warn("复制Tesseract配置失败，使用默认配置", e);
                instance.setLanguage("eng");
                instance.setOcrEngineMode(1);
                instance.setPageSegMode(1);
            }
        }
        
        return instance;
    }

    /**
     * 检查OCR服务是否健康
     * @return 服务状态
     */
    public boolean isServiceHealthy() {
        // 根据配置的OCR引擎检查相应服务的健康状态
        if ("textin".equalsIgnoreCase(ocrEngine)) {
            if (!textInMcpService.isServiceAvailable()) {
                return false;
            }
        } else {
            if (tesseract == null || ocrExecutor == null || ocrExecutor.isShutdown()) {
                return false;
            }
        }
        
        // 检查最近的失败率
        long totalRequests = successCount.get() + failureCount.get();
        if (totalRequests > 10) {
            double failureRate = (double) failureCount.get() / totalRequests;
            if (failureRate > 0.5) { // 失败率超过50%
                log.warn("OCR服务健康状况不佳，失败率: {:.2f}%", failureRate * 100);
                return false;
            }
        }
        
        return true;
    }

    /**
     * 获取OCR服务统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("ocrEngine", ocrEngine);
        stats.put("successCount", successCount.get());
        stats.put("failureCount", failureCount.get());
        stats.put("lastSuccessTime", lastSuccessTime.get());
        stats.put("lastFailureTime", lastFailureTime.get());
        stats.put("isHealthy", isServiceHealthy());
        
        // 添加引擎特定的状态信息
        if ("textin".equalsIgnoreCase(ocrEngine)) {
            stats.put("textInStatus", textInMcpService.getServiceStatus());
        }
        
        if (ocrExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) ocrExecutor;
            stats.put("threadPoolActive", tpe.getActiveCount());
            stats.put("threadPoolQueue", tpe.getQueue().size());
            stats.put("threadPoolCompleted", tpe.getCompletedTaskCount());
        } else {
            stats.put("threadPoolActive", 0);
            stats.put("threadPoolQueue", 0);
            stats.put("threadPoolCompleted", 0);
        }
        
        long totalRequests = successCount.get() + failureCount.get();
        if (totalRequests > 0) {
            double successRate = (double) successCount.get() / totalRequests;
            stats.put("successRate", String.format("%.2f%%", successRate * 100));
        } else {
            stats.put("successRate", "N/A");
        }
        
        return stats;
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        successCount.set(0);
        failureCount.set(0);
        lastSuccessTime.set(0);
        lastFailureTime.set(0);
        log.info("OCR服务统计信息已重置");
    }

    /**
     * 强制垃圾回收和内存清理
     */
    public void forceCleanup() {
        log.info("执行强制内存清理");
        System.gc();
        System.runFinalization();
        
        // 清理线程池中的已完成任务
        if (ocrExecutor instanceof ThreadPoolExecutor) {
            ((ThreadPoolExecutor) ocrExecutor).purge();
        }
    }

    /**
     * 获取支持的语言列表
     * @return 语言列表
     */
    public String getSupportedLanguages() {
        if (tesseract == null) {
            return "OCR服务未初始化";
        }
        
        // 返回配置的语言信息
        return "当前支持的语言: chi_sim+eng (中文简体+英文)";
    }
    
}