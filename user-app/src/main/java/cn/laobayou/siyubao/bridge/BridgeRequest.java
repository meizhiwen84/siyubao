package cn.laobayou.siyubao.bridge;

import com.fasterxml.jackson.databind.JsonNode;

public class BridgeRequest {
    private String method;
    private JsonNode params;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JsonNode getParams() {
        return params;
    }

    public void setParams(JsonNode params) {
        this.params = params;
    }
}
