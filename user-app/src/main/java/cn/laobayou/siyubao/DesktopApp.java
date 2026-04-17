package cn.laobayou.siyubao;

import cn.laobayou.siyubao.bridge.JcefBridgeHandler;
import cn.laobayou.siyubao.bridge.JsBridgeDispatcher;
import cn.laobayou.siyubao.desktop.DesktopRuntime;
import cn.laobayou.siyubao.util.AppSecurity;
import cn.laobayou.siyubao.util.CommonUilts;
import cn.laobayou.siyubao.util.JcefLocalConfig;
import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class DesktopApp {
    private static int port = 6942;
    private static FileLock lock;
    private static final CountDownLatch serverStartedLatch = new CountDownLatch(1);
    private static volatile ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        // 1. 安全校验
        AppSecurity.check();
        // 2. JCEF 强制本地
        JcefLocalConfig.setup();

        System.setProperty("java.awt.headless", "false");
        DesktopRuntime.enableDesktopMode();

        File appBaseDir = resolveBaseDir();
        File dataDir = new File(appBaseDir, "data");
        dataDir.mkdirs();
        System.setProperty("SIYUBAO_DB_PATH", new File(dataDir, "siyubao.db").getAbsolutePath());

        if (!checkSingleInstance()) {
            JOptionPane.showMessageDialog(null, "应用已在运行", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                applicationContext = SpringApplication.run(DesktopApp.class, args);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "启动失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }, "spring-boot").start();

        JFrame frame = new JFrame("私信截图王");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        try {
            Image icon = createAppIcon();
            frame.setIconImage(icon);
            try {
                Class<?> taskbarClz = Class.forName("java.awt.Taskbar");
                Object taskbar = taskbarClz.getMethod("getTaskbar").invoke(null);
                taskbarClz.getMethod("setIconImage", Image.class).invoke(taskbar, icon);
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
        frame.getContentPane().setLayout(new BorderLayout());
        LoadingPanel loadingPanel = new LoadingPanel();
        frame.getContentPane().add(loadingPanel, BorderLayout.CENTER);
        frame.setResizable(false);
        frame.setSize(920, 560);
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.NORMAL);
        frame.setVisible(true);

        new Thread(() -> {
            try {
                if (!serverStartedLatch.await(30, TimeUnit.SECONDS)) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "服务启动超时", "错误", JOptionPane.ERROR_MESSAGE));
                    System.exit(1);
                }

                String exeDir = CommonUilts.getExeDirectory();
                File localJcefDir = new File(exeDir, "jcef");
                File installDir = localJcefDir.exists() ? localJcefDir : new File(resolveBaseDir(), "jcef");
                installDir.mkdirs();
                File userDataDir = new File(resolveBaseDir(), "jcef-user-data");
                userDataDir.mkdirs();
                
                System.out.println("========================================");
                System.out.println("JCEF 初始化");
                System.out.println("本地JCEF目录: " + localJcefDir.getAbsolutePath());
                System.out.println("本地JCEF目录是否存在: " + localJcefDir.exists());
                System.out.println("使用JCEF目录: " + installDir.getAbsolutePath());
                System.out.println("========================================");

                CefAppBuilder builder = new CefAppBuilder();
                builder.setInstallDir(installDir);

                CefSettings settings = builder.getCefSettings();
                settings.windowless_rendering_enabled = false;
                settings.cache_path = userDataDir.getAbsolutePath();

                builder.addJcefArgs("--allow-file-access-from-files");
                builder.addJcefArgs("--disable-web-security");
                builder.addJcefArgs("--enable-features=WebP");
                builder.addJcefArgs("--autoplay-policy=no-user-gesture-required");

                builder.addJcefArgs("--disable-gpu");
                builder.addJcefArgs("--disable-gpu-compositing");
                builder.addJcefArgs("--no-sandbox");
                builder.addJcefArgs("--user-data-dir=" + userDataDir.getAbsolutePath());
                builder.addJcefArgs("--disable-dev-shm-usage");
                builder.addJcefArgs("--disable-extensions");
                builder.addJcefArgs("--disable-background-networking");
                builder.addJcefArgs("--disable-default-apps");
                builder.addJcefArgs("--disable-sync");
                builder.addJcefArgs("--disable-translate");
                builder.addJcefArgs("--disable-features=VizDisplayCompositor");

                CefApp cefApp = builder.build();
                CefClient client = cefApp.createClient();

                if (applicationContext != null) {
                    JsBridgeDispatcher dispatcher = applicationContext.getBean(JsBridgeDispatcher.class);
                    CefMessageRouter.CefMessageRouterConfig config = new CefMessageRouter.CefMessageRouterConfig("cefQuery", "cefQueryCancel");
                    CefMessageRouter router = CefMessageRouter.create(config);
                    router.addHandler(new JcefBridgeHandler(dispatcher), true);
                    client.addMessageRouter(router);
                }

                client.addLoadHandler(new CefLoadHandlerAdapter() {
                    @Override
                    public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                        System.err.println("JCEF LoadError: " + errorCode + " " + errorText + " url=" + failedUrl);
                    }
                });

                client.addDisplayHandler(new CefDisplayHandlerAdapter() {
                    @Override
                    public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                        System.out.println("JCEF Console[" + level + "]: " + message + " (" + source + ":" + line + ")");
                        return false;
                    }
                });

                String url = "http://127.0.0.1:" + port + "/app/login?v=" + System.currentTimeMillis();
                System.out.println("加载页面：" + url);
                CefBrowser browser = client.createBrowser(url, false, false);

                SwingUtilities.invokeLater(() -> {
                    frame.getContentPane().removeAll();
                    try {
                        loadingPanel.stop();
                    } catch (Exception ignored) {
                    }
                    frame.setResizable(true);
                    frame.setExtendedState(JFrame.NORMAL);
                    frame.setSize(1200, 768);
                    frame.setLocationRelativeTo(null);
                    frame.getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);
                    frame.revalidate();
                    frame.repaint();
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

                    frame.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                            CefApp instance = CefApp.getInstance();
                            if (instance != null) {
                                instance.dispose();
                            }
                        }
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "界面启动失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE));
                System.exit(1);
            }
        }, "jcef-init").start();
    }

    private static boolean checkSingleInstance() {
        try {
            File lockFile = new File(System.getProperty("user.home"), ".app.lock");
            RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
            FileChannel channel = raf.getChannel();
            lock = channel.tryLock();

            if (lock == null) {
                return false;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        lock.release();
                        channel.close();
                        raf.close();
                    } catch (Exception ignored) {
                    }
                }
            }));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static File resolveBaseDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        File baseDir = (localAppData == null || localAppData.trim().isEmpty())
                ? new File(System.getProperty("user.home"), ".siyubao")
                : new File(localAppData, "SiyuBao");
        baseDir.mkdirs();
        return baseDir;
    }

    @EventListener
    public void onStart(ServletWebServerInitializedEvent event) {
        port = event.getWebServer().getPort();
        System.out.println("服务启动端口：" + port);
        serverStartedLatch.countDown();
    }

    private static Image createAppIcon() {
        return cn.laobayou.siyubao.ui.AppIcon.render(256);
    }

    private static final class LoadingPanel extends JPanel {
        private final Timer timer;
        private volatile double t = 0;

        private LoadingPanel() {
            setOpaque(true);
            setBackground(new Color(7, 10, 18));
            timer = new Timer(16, e -> {
                t += 0.016;
                repaint();
            });
            timer.setRepeats(true);
            timer.start();
        }

        private void stop() {
            timer.stop();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(new Color(7, 10, 18));
                g2.fillRect(0, 0, w, h);
                drawNeonBackground(g2, w, h);
                drawRibbonMesh(g2, w, h);

                int cardW = Math.min(860, (int) (w * 0.86));
                int cardH = Math.min(360, (int) (h * 0.56));
                int cardX = (w - cardW) / 2;
                int cardY = (h - cardH) / 2;

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.86f));
                g2.setColor(new Color(15, 23, 42));
                g2.fillRoundRect(cardX, cardY, cardW, cardH, 22, 22);
                g2.setComposite(AlphaComposite.SrcOver);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(148, 163, 184, 50));
                g2.drawRoundRect(cardX, cardY, cardW, cardH, 22, 22);

                int pad = 26;
                int x0 = cardX + pad;
                int y0 = cardY + pad;

                drawBadge(g2, x0, y0, "私域 + 电商");
                drawBadge(g2, x0 + 104, y0, "微商 + 知识付费");
                drawBadge(g2, x0 + 236, y0, "探店 + 医美减肥");
                drawBadge(g2, x0 + 366, y0, "营销引流 + 刚需");

                g2.setColor(new Color(226, 232, 240));
                g2.setFont(getFont().deriveFont(Font.BOLD, 32f));
                g2.drawString("私信截图王", x0, y0 + 62);

                g2.setColor(new Color(96, 215, 255, 245));
                g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
                g2.drawString("一键生成抖音、小红书、视频号、快手逼真聊天记录", x0, y0 + 88);

                int chip = 64;
                int chipX = cardX + cardW - pad - chip;
                int chipY = y0;
                g2.setColor(new Color(2, 6, 23, 120));
                g2.fillRoundRect(chipX, chipY, chip, chip, 18, 18);
                g2.setColor(new Color(148, 163, 184, 60));
                g2.drawRoundRect(chipX, chipY, chip, chip, 18, 18);
                drawScanBox(g2, chipX + 12, chipY + 12, chip - 24, chip - 24);

                int barX = x0;
                int barY = cardY + cardH - pad - 50;
                int barW = cardW - pad * 2;
                int barH = 10;

                g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
                g2.setColor(new Color(203, 213, 225));
                g2.drawString("加载中 · 初始化模块", barX, barY - 10);

                g2.setColor(new Color(2, 6, 23, 120));
                g2.fillRoundRect(barX, barY, barW, barH, 999, 999);
                g2.setColor(new Color(148, 163, 184, 60));
                g2.drawRoundRect(barX, barY, barW, barH, 999, 999);
                drawScanLine(g2, barX, barY, barW, barH);

                drawDots(g2, barX + barW - 46, barY - 20);

                g2.setColor(new Color(148, 163, 184, 200));
                g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
                g2.drawString("正在启动引擎与界面资源，请稍候…", barX, barY + 30);
            } finally {
                g2.dispose();
            }
        }

        private void drawNeonBackground(Graphics2D g2, int w, int h) {
            double p = t * 0.12;
            float x1 = (float) (w * (0.18 + 0.03 * Math.sin(p)));
            float y1 = (float) (h * (0.20 + 0.03 * Math.cos(p * 1.2)));
            float x2 = (float) (w * (0.82 + 0.03 * Math.cos(p * 0.9)));
            float y2 = (float) (h * (0.26 + 0.03 * Math.sin(p * 1.1)));
            float x3 = (float) (w * 0.52);
            float y3 = (float) (h * (0.92 + 0.02 * Math.sin(p * 0.8)));

            Composite oc = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
            g2.setPaint(new RadialGradientPaint(
                    x1, y1, (float) (Math.min(w, h) * 0.75),
                    new float[]{0f, 1f},
                    new Color[]{new Color(59, 130, 246, 90), new Color(0, 0, 0, 0)}
            ));
            g2.fillRect(0, 0, w, h);

            g2.setPaint(new RadialGradientPaint(
                    x2, y2, (float) (Math.min(w, h) * 0.70),
                    new float[]{0f, 1f},
                    new Color[]{new Color(168, 85, 247, 90), new Color(0, 0, 0, 0)}
            ));
            g2.fillRect(0, 0, w, h);

            g2.setPaint(new RadialGradientPaint(
                    x3, y3, (float) (Math.min(w, h) * 0.65),
                    new float[]{0f, 1f},
                    new Color[]{new Color(16, 185, 129, 70), new Color(0, 0, 0, 0)}
            ));
            g2.fillRect(0, 0, w, h);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g2.setPaint(new GradientPaint(0, 0, new Color(11, 16, 32, 220), 0, h, new Color(7, 10, 18, 255)));
            g2.fillRect(0, 0, w, h);

            g2.setComposite(oc);
        }

        private void drawSubtleGrid(Graphics2D g2, int w, int h) {
            Composite oc = g2.getComposite();
            AffineTransform ot = g2.getTransform();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
            g2.setColor(new Color(148, 163, 184));
            g2.translate(w * 0.02, h * 0.03);
            g2.rotate(Math.toRadians(-4), w / 2.0, h / 2.0);
            int grid = 48;
            for (int x = -grid * 2; x < w + grid * 2; x += grid) g2.drawLine(x, -grid * 2, x, h + grid * 2);
            for (int y = -grid * 2; y < h + grid * 2; y += grid) g2.drawLine(-grid * 2, y, w + grid * 2, y);
            g2.setTransform(ot);
            g2.setComposite(oc);
        }

        private void drawRibbonMesh(Graphics2D g2, int w, int h) {
            double base = t * 0.25;
            double scale = Math.min(w, h) * 0.42;
            double cx0 = w * 0.70;
            double cy0 = h * 0.50;
            double rot = -0.18;

            int steps = 160;
            double[] px = new double[steps];
            double[] py = new double[steps];
            double[] nx = new double[steps];
            double[] ny = new double[steps];
            double[] ww = new double[steps];

            for (int i = 0; i < steps; i++) {
                double u = i / (double) (steps - 1);
                double a = (u * Math.PI * 2.0) + base;
                double x = Math.sin(a);
                double y = 0.62 * Math.sin(2 * a);
                double rx = x * Math.cos(rot) - y * Math.sin(rot);
                double ry = x * Math.sin(rot) + y * Math.cos(rot);
                px[i] = cx0 + rx * scale;
                py[i] = cy0 + ry * scale * 0.72;
                ww[i] = scale * 0.16 * (0.65 + 0.35 * Math.cos(a * 2.0));
            }

            for (int i = 0; i < steps; i++) {
                int i0 = Math.max(0, i - 1);
                int i1 = Math.min(steps - 1, i + 1);
                double tx = px[i1] - px[i0];
                double ty = py[i1] - py[i0];
                double len = Math.hypot(tx, ty);
                if (len < 1e-6) len = 1;
                tx /= len;
                ty /= len;
                nx[i] = -ty;
                ny[i] = tx;
            }

            Composite oc = g2.getComposite();
            Stroke os = g2.getStroke();

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
            g2.setStroke(new BasicStroke(28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < steps - 1; i++) {
                double u = i / (double) (steps - 1);
                Color c = blend(new Color(59, 130, 246), new Color(168, 85, 247), (float) u);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));
                g2.draw(new java.awt.geom.Line2D.Double(px[i], py[i], px[i + 1], py[i + 1]));
            }

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f));
            g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < steps - 1; i++) {
                double u = i / (double) (steps - 1);
                Color c = blend(new Color(0, 164, 255), new Color(255, 0, 170), (float) u);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));
                g2.draw(new java.awt.geom.Line2D.Double(px[i], py[i], px[i + 1], py[i + 1]));
            }

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.34f));
            g2.setStroke(new BasicStroke(1.2f));
            int crossEvery = 10;
            for (int i = 0; i < steps; i += crossEvery) {
                double u = i / (double) (steps - 1);
                Color c = blend(new Color(84, 193, 255), new Color(255, 95, 205), (float) u);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 170));
                double dx = nx[i] * ww[i];
                double dy = ny[i] * ww[i];
                g2.draw(new java.awt.geom.Line2D.Double(px[i] - dx, py[i] - dy, px[i] + dx, py[i] + dy));
            }

            double[] vs = new double[]{-0.6, -0.3, 0.0, 0.3, 0.6};
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            g2.setStroke(new BasicStroke(1.0f));
            for (double v : vs) {
                java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
                for (int i = 0; i < steps; i++) {
                    double ox = px[i] + nx[i] * ww[i] * v;
                    double oy = py[i] + ny[i] * ww[i] * v;
                    if (i == 0) path.moveTo(ox, oy);
                    else path.lineTo(ox, oy);
                }
                g2.setColor(new Color(148, 163, 184, 180));
                g2.draw(path);
            }

            g2.setComposite(oc);
            g2.setStroke(os);
        }

        private Color blend(Color a, Color b, float t) {
            float tt = Math.max(0f, Math.min(1f, t));
            int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * tt);
            int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * tt);
            int bb = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * tt);
            return new Color(r, g, bb);
        }

        private void drawBadge(Graphics2D g2, int x, int y, String text) {
            Font f = getFont().deriveFont(Font.PLAIN, 12f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics(f);
            int w = fm.stringWidth(text) + 18;
            int h = 24;
            g2.setColor(new Color(2, 6, 23, 140));
            g2.fillRoundRect(x, y, w, h, 999, 999);
            g2.setColor(new Color(100, 116, 139, 90));
            g2.drawRoundRect(x, y, w, h, 999, 999);
            g2.setColor(new Color(96, 215, 255, 245));
            g2.drawString(text, x + 9, y + 16);
        }

        private void drawScanBox(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(new Color(2, 6, 23, 150));
            g2.fillRoundRect(x, y, w, h, 14, 14);
            g2.setColor(new Color(148, 163, 184, 70));
            g2.drawRoundRect(x, y, w, h, 14, 14);
            drawScanLine(g2, x, y, w, h);
        }

        private void drawScanLine(Graphics2D g2, int x, int y, int w, int h) {
            double phase = (t % 1.6) / 1.6;
            int cx = x + (int) Math.round((phase * 2 - 0.5) * w);
            int lineW = Math.max(60, w / 3);
            int sx = cx - lineW / 2;
            float[] dist = new float[]{0f, 0.5f, 1f};
            Color[] cols = new Color[]{new Color(0, 0, 0, 0), new Color(59, 130, 246, 150), new Color(0, 0, 0, 0)};
            java.awt.LinearGradientPaint gp = new java.awt.LinearGradientPaint(sx, 0, sx + lineW, 0, dist, cols);
            Paint old = g2.getPaint();
            Composite oc = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, w, h, 999, 999);
            g2.setComposite(oc);
            g2.setPaint(old);
        }

        private void drawDots(Graphics2D g2, int x, int y) {
            int r = 6;
            for (int i = 0; i < 3; i++) {
                double p = (t * 1.2 + i * 0.18) % 1.0;
                float a = (float) (0.25 + 0.75 * (0.5 - 0.5 * Math.cos(p * Math.PI * 2)));
                g2.setColor(new Color(148, 163, 184, Math.min(255, Math.max(0, (int) (a * 255)))));
                g2.fillOval(x + i * 14, y, r, r);
            }
        }
    }
}
