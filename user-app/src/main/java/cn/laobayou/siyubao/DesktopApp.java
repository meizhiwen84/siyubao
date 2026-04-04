package cn.laobayou.siyubao;
import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
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

@SpringBootApplication
public class DesktopApp {

    private static int port = 6942;
    private static FileLock lock;
    private static CountDownLatch serverStartedLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        if (!checkSingleInstance()) {
            JOptionPane.showMessageDialog(null, "应用程序已经在运行中", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                SpringApplication.run(DesktopApp.class, args);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Spring Boot应用启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }).start();

        try {
            // 等待Spring Boot服务器启动完成
            if (!serverStartedLatch.await(30, java.util.concurrent.TimeUnit.SECONDS)) {
                JOptionPane.showMessageDialog(null, "Spring Boot服务器启动超时", "错误", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }

            // 初始化JCEF
            CefAppBuilder builder = new CefAppBuilder();
            builder.setInstallDir(new File("./jcef"));
            CefApp cefApp = builder.build();
            CefClient client = cefApp.createClient();

            // 创建浏览器窗口
            JFrame frame = new JFrame("管理系统");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            CefBrowser browser = client.createBrowser("http://localhost:" + port, false, false);
            frame.add(browser.getUIComponent(), BorderLayout.CENTER);
            frame.setSize(1200, 768);
            frame.setLocationRelativeTo(null);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "应用程序启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
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
                        if (lock != null) {
                            lock.release();
                        }
                        channel.close();
                        raf.close();
                    } catch (Exception ignored) {
                    }
                }
            }));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "单例检查失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    @EventListener
    public void onStart(ServletWebServerInitializedEvent event) {
        port = event.getWebServer().getPort();
        System.out.println("Spring Boot服务器启动完成，端口: " + port);
        serverStartedLatch.countDown(); // 通知主线程服务器已启动
    }
}