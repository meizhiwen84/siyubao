package cn.laobayou.siyubao.desktop;

public final class DesktopRuntime {
    private static volatile boolean desktopMode = false;
    private static volatile String cardCode;

    private DesktopRuntime() {
    }

    public static void enableDesktopMode() {
        desktopMode = true;
    }

    public static boolean isDesktopMode() {
        return desktopMode;
    }

    public static void setCardCode(String code) {
        cardCode = code;
    }

    public static String getCardCode() {
        return cardCode;
    }
}
