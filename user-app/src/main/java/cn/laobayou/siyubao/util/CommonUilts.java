package cn.laobayou.siyubao.util;

import cn.laobayou.siyubao.config.WebMvcCardConfig;

public class CommonUilts {

    public static String getExeDirectory() {
        try {
            java.nio.file.Path codeSourcePath = java.nio.file.Paths.get(
                    WebMvcCardConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            java.nio.file.Path parentDir = codeSourcePath.getParent();
            // 如果是jar文件，返回jar所在目录；否则返回代码所在目录
            if (parentDir != null) {
                return parentDir.toAbsolutePath().toString().replace("\\", "/");
            }
        } catch (Exception e) {
            // 降级：使用当前工作目录
        }
        return System.getProperty("user.dir").replace("\\", "/");
    }

}
