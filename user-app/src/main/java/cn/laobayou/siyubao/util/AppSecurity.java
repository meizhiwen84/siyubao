package cn.laobayou.siyubao.util;

import java.io.File;

public class AppSecurity {

    // 关键路径拆分，不写完整明文
    private static final String DIR_JCEF    = CommonUilts.getExeDirectory()+"./" + "jce" + "f";
    private static final String DIR_ASSETS  = CommonUilts.getExeDirectory()+"./" + "asse" + "ts";
    private static final String KEY_IMAGE   = DIR_ASSETS + "/images/test.png";
    private static final String CHECK_FILE  = CommonUilts.getExeDirectory()+"./" + ".te" + "st1";

    public static void check() {
        boolean pass = true;

        // 1. 检查 JCEF 目录
        File jcefDir = new File(DIR_JCEF);
        if (!jcefDir.exists() || !jcefDir.isDirectory()) {
            pass = false;
        }

        // 2. 检查关键图片存在（防删减）
        File keyImg = new File(KEY_IMAGE);
        if (!keyImg.exists() || keyImg.length() < 100) {
            pass = false;
        }

        // 3. 检查授权标记文件
        File checkFile = new File(CHECK_FILE);
        if (!checkFile.exists()) {
            pass = false;
        }

        if (!pass) {
            System.exit(-1);
        }
    }
}