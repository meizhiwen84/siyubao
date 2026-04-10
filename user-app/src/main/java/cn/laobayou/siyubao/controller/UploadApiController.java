package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.service.LocalUserSessionService;
import cn.laobayou.siyubao.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UploadApiController {
    private final UploadService uploadService;
    private final LocalUserSessionService localUserSessionService;

    public UploadApiController(UploadService uploadService, LocalUserSessionService localUserSessionService) {
        this.uploadService = uploadService;
        this.localUserSessionService = localUserSessionService;
    }

    @PostMapping("/api/upload/image")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> resp = new HashMap<>();
        Long userId = localUserSessionService.userId();
        if (userId == null || userId <= 0) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return ResponseEntity.status(401).body(resp);
        }
        try {
            String url = uploadService.saveImage(userId, file);
            resp.put("success", true);
            resp.put("url", url);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }
}
