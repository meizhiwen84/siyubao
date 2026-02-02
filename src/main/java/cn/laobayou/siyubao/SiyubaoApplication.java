package cn.laobayou.siyubao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.bridge.SLF4JBridgeHandler;

@SpringBootApplication
public class SiyubaoApplication {

    public static void main(String[] args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        SpringApplication.run(SiyubaoApplication.class, args);
    }

}
