package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bean.CardKey;
import cn.laobayou.siyubao.service.CardKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/card")
public class CardKeyApiController {
    @Autowired
    private CardKeyService cardKeyService;

    @GetMapping("/{code}")
    public ResponseEntity<CardKey> findByCode(@PathVariable String code) {
        Optional<CardKey> opt = cardKeyService.findByCode(code);
        if (!opt.isPresent()) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.ok(opt.get());
    }

    @PostMapping("/{code}/decrement")
    public ResponseEntity<Map<String, Object>> decrement(@PathVariable String code) {
        Map<String, Object> result = new HashMap<>();
        boolean ok = cardKeyService.decrementIfAvailable(code);
        result.put("success", ok);
        if (!ok) {
            result.put("message", "扣减失败（可能卡密无效、停用或次数不足）");
        }
        return ok ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }
}
