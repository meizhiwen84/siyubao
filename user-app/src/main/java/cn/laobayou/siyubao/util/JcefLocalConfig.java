package cn.laobayou.siyubao.util;
import java.io.File;

public class JcefLocalConfig {

    public static void setup() {
        String exeDir = CommonUilts.getExeDirectory();
        String jcefPath = exeDir + "/" + "jcef";
        File localJcefDir = new File(jcefPath);
        
        System.out.println("========================================");
        System.out.println("JCEF 本地配置初始化");
        System.out.println("EXE目录: " + exeDir);
        System.out.println("JCEF目录: " + jcefPath);
        System.out.println("JCEF目录是否存在: " + localJcefDir.exists());
        
        if (localJcefDir.exists()) {
            System.out.println("发现本地 JCEF，强制使用本地模式，禁止下载");
            // 强制本地，彻底禁止下载
            System.setProperty("jcefmaven.mode", "local");
            System.setProperty("jcefmaven.download.enabled", "false");
            System.setProperty("jcefmaven.download.allowed", "false");
            System.setProperty("jcefmaven.binary.path", localJcefDir.getAbsolutePath());
            System.out.println("jcefmaven.binary.path = " + System.getProperty("jcefmaven.binary.path"));
            System.out.println("jcefmaven.mode = " + System.getProperty("jcefmaven.mode"));
        } else {
            System.out.println("未找到本地 JCEF，使用自动模式");
            // 开发环境自动下载
            System.setProperty("jcefmaven.mode", "auto");
        }
        System.out.println("========================================");
    }
}

