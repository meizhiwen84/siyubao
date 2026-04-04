package cn.laobayou.siyubao;

import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;

import javax.swing.*;
import java.awt.*;
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

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        if (!checkSingleInstance()) {
            JOptionPane.showMessageDialog(null, "应用已在运行", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                SpringApplication.run(DesktopApp.class, args);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "启动失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }, "spring-boot").start();

        JFrame frame = new JFrame("管理系统");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new JLabel("加载中...", SwingConstants.CENTER), BorderLayout.CENTER);
        frame.setSize(1200, 768);
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);

        new Thread(() -> {
            try {
                if (!serverStartedLatch.await(30, TimeUnit.SECONDS)) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "服务启动超时", "错误", JOptionPane.ERROR_MESSAGE));
                    System.exit(1);
                }

                File installDir = new File(System.getProperty("user.home"), ".siyubao/jcef");
                installDir.mkdirs();

                CefAppBuilder builder = new CefAppBuilder();
                builder.setInstallDir(installDir);

                CefSettings settings = builder.getCefSettings();
                settings.windowless_rendering_enabled = false;

                builder.addJcefArgs("--disable-gpu");
                builder.addJcefArgs("--disable-gpu-compositing");
                builder.addJcefArgs("--no-sandbox");
                builder.addJcefArgs("--disable-dev-shm-usage");
                builder.addJcefArgs("--disable-extensions");
                builder.addJcefArgs("--disable-background-networking");
                builder.addJcefArgs("--disable-default-apps");
                builder.addJcefArgs("--disable-sync");
                builder.addJcefArgs("--disable-translate");
                builder.addJcefArgs("--disable-features=VizDisplayCompositor");

                CefApp cefApp = builder.build();
                CefClient client = cefApp.createClient();

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

                String url = "http://127.0.0.1:" + port + "/";
                System.out.println("加载页面：" + url);
                CefBrowser browser = client.createBrowser(url, false, false);

                SwingUtilities.invokeLater(() -> {
                    frame.getContentPane().removeAll();
                    frame.getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);
                    frame.revalidate();
                    frame.repaint();

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

    @EventListener
    public void onStart(ServletWebServerInitializedEvent event) {
        port = event.getWebServer().getPort();
        System.out.println("服务启动端口：" + port);
        serverStartedLatch.countDown();
    }
}