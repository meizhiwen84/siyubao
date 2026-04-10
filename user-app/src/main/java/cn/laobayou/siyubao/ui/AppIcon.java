package cn.laobayou.siyubao.ui;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class AppIcon {
    private AppIcon() {
    }

    public static BufferedImage render(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(new Color(0, 0, 0, 0));
            g2.fillRect(0, 0, size, size);

            int pad = Math.max(6, Math.round(size * 0.04f));
            int r = Math.max(18, Math.round(size * 0.19f));
            g2.setColor(new Color(7, 10, 18, 255));
            g2.fillRoundRect(pad, pad, size - pad * 2, size - pad * 2, r, r);

            float cx1 = size * 0.34f;
            float cy1 = size * 0.28f;
            float cx2 = size * 0.72f;
            float cy2 = size * 0.34f;
            float radius = size * 0.85f;
            g2.setPaint(new RadialGradientPaint(cx1, cy1, radius, new float[]{0f, 1f}, new Color[]{new Color(59, 130, 246, 130), new Color(0, 0, 0, 0)}));
            g2.fillRect(0, 0, size, size);
            g2.setPaint(new RadialGradientPaint(cx2, cy2, radius, new float[]{0f, 1f}, new Color[]{new Color(168, 85, 247, 120), new Color(0, 0, 0, 0)}));
            g2.fillRect(0, 0, size, size);

            float stroke = Math.max(10f, size * 0.07f);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setPaint(new LinearGradientPaint(size * 0.16f, size * 0.24f, size * 0.86f, size * 0.82f, new float[]{0f, 0.55f, 1f}, new Color[]{new Color(18, 75, 170), new Color(0, 200, 255), new Color(13, 35, 90)}));

            java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
            path.moveTo(size * 0.23, size * 0.25);
            path.lineTo(size * 0.44, size * 0.82);
            path.lineTo(size * 0.53, size * 0.60);
            path.lineTo(size * 0.62, size * 0.82);
            path.lineTo(size * 0.83, size * 0.25);
            g2.draw(path);

            java.awt.geom.Path2D.Double z = new java.awt.geom.Path2D.Double();
            z.moveTo(size * 0.28, size * 0.35);
            z.lineTo(size * 0.80, size * 0.35);
            z.lineTo(size * 0.44, size * 0.65);
            z.lineTo(size * 0.80, size * 0.65);
            g2.draw(z);

            g2.setColor(new Color(148, 163, 184, 60));
            g2.setStroke(new BasicStroke(Math.max(4f, size * 0.025f)));
            g2.drawRoundRect(pad, pad, size - pad * 2, size - pad * 2, r, r);
        } finally {
            g2.dispose();
        }
        return img;
    }
}

