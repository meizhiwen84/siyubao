package cn.laobayou.siyubao.tools;

import cn.laobayou.siyubao.ui.AppIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ExeIconGenerator {
    public static void main(String[] args) throws Exception {
        String out = (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty())
                ? args[0].trim()
                : "target/app.ico";
        File outFile = new File(out);
        File parent = outFile.getAbsoluteFile().getParentFile();
        if (parent != null) parent.mkdirs();

        BufferedImage img = AppIcon.render(256);
        byte[] png = toPng(img);
        byte[] ico = toSinglePngIco(png);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(ico);
        }
    }

    private static byte[] toPng(BufferedImage img) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            return bos.toByteArray();
        }
    }

    private static byte[] toSinglePngIco(byte[] pngBytes) {
        int headerSize = 6;
        int entrySize = 16;
        int offset = headerSize + entrySize;
        int total = offset + pngBytes.length;

        ByteBuffer b = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short) 0);
        b.putShort((short) 1);
        b.putShort((short) 1);

        b.put((byte) 0);
        b.put((byte) 0);
        b.put((byte) 0);
        b.put((byte) 0);
        b.putShort((short) 1);
        b.putShort((short) 32);
        b.putInt(pngBytes.length);
        b.putInt(offset);

        b.put(pngBytes);
        return b.array();
    }
}

