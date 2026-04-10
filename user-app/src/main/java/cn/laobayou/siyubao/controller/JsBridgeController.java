package cn.laobayou.siyubao.controller;

import cn.laobayou.siyubao.bridge.BridgeRequest;
import cn.laobayou.siyubao.bridge.JsBridgeDispatcher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JsBridgeController {
    private final JsBridgeDispatcher dispatcher;

    public JsBridgeController(JsBridgeDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @PostMapping("/api/jsbridge/invoke")
    public Map<String, Object> invoke(@RequestBody BridgeRequest request) {
        return dispatcher.dispatch(request);
    }
}
