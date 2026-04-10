package cn.laobayou.siyubao.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class UploadService {
    private final Path baseDir = Paths.get(System.getProperty("user.home"), ".siyubao", "uploads").toAbsolutePath().normalize();
    private static volatile boolean migrated = false;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    public String saveImage(Long userId, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new RuntimeException("文件为空");
        String originalName = file.getOriginalFilename();
        byte[] bytes = file.getBytes();
        return saveImageBytes(userId, originalName, bytes);
    }

    public String saveImageBytes(Long userId, String originalName, byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0) throw new RuntimeException("文件为空");
        if (bytes.length > MAX_IMAGE_BYTES) throw new RuntimeException("图片过大");
        String owner = ownerKey(userId);

        migrateLegacyIfNeeded();

        String ext = extOf(originalName);
        String detected = detectImageExt(bytes);
        if (detected == null) throw new RuntimeException("只支持图片格式：jpg, jpeg, png, gif, webp");
        ext = detected;
        String name = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir = baseDir.resolve(owner).normalize();
        if (!dir.startsWith(baseDir)) throw new RuntimeException("路径非法");
        Files.createDirectories(dir);

        Path target = dir.resolve(name).normalize();
        if (!target.startsWith(dir)) throw new RuntimeException("路径非法");
        Files.write(target, bytes);
        return "/uploads/" + owner + "/" + name;
    }

    private String ownerKey(Long userId) {
        if (userId == null || userId <= 0) throw new RuntimeException("未登录");
        return "u" + userId;
    }

    private void migrateLegacyIfNeeded() {
        if (migrated) return;
        synchronized (UploadService.class) {
            if (migrated) return;
            migrated = true;
            try {
                Path legacy = Paths.get("data", "uploads").toAbsolutePath().normalize();
                if (Files.exists(baseDir) || !Files.exists(legacy)) return;
                Files.createDirectories(baseDir.getParent());
                try {
                    Files.move(legacy, baseDir);
                    return;
                } catch (Exception ignored) {
                }
                copyDir(legacy, baseDir);
            } catch (Exception ignored) {
            }
        }
    }

    private void copyDir(Path from, Path to) throws Exception {
        Files.walk(from).forEach(p -> {
            try {
                Path rel = from.relativize(p);
                Path target = to.resolve(rel).normalize();
                if (Files.isDirectory(p)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private String extOf(String filename) {
        if (!StringUtils.hasText(filename)) return ".png";
        String f = filename.trim();
        int i = f.lastIndexOf('.');
        if (i < 0) return ".png";
        String ext = f.substring(i).toLowerCase(Locale.ROOT);
        if (ext.length() > 8) return ".png";
        if (ext.equals(".png") || ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".webp") || ext.equals(".gif")) return ext;
        return ".png";
    }

    private String detectImageExt(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;

        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return ".png";

        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) return ".jpg";

        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38) return ".gif";

        if (bytes.length >= 12 &&
                bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 &&
                bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return ".webp";

        return null;
    }
}
