package cn.laobayou.siyubao.bridge;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

public class JcefBridgeHandler extends CefMessageRouterHandlerAdapter {
    private final JsBridgeDispatcher dispatcher;

    public JcefBridgeHandler(JsBridgeDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request, boolean persistent, CefQueryCallback callback) {
        try {
            String resp = dispatcher.dispatchToJson(request);
            callback.success(resp);
        } catch (Exception e) {
            callback.failure(1, e.getMessage() == null ? "bridge error" : e.getMessage());
        }
        return true;
    }
}
