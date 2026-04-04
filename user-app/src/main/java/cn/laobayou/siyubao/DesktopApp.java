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

@SpringBootApplication
public class DesktopApp {

    private static int port = 6942;
    private static FileLock lock;

    public static void main(String[] args) {
        if (!checkSingleInstance()) {
            return;
        }

        new Thread(() -> SpringApplication.run(DesktopApp.class, args)).start();

        try {
            Thread.sleep(2500);
            CefAppBuilder builder = new CefAppBuilder();
            builder.setInstallDir(new File("./jcef"));
            CefApp cefApp = builder.build();
            CefClient client = cefApp.createClient();

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
                        lock.release();
                        channel.close();
                        raf.close();
                    } catch (Exception ignored) {
                    }
                }
            }));

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @EventListener
    public void onStart(ServletWebServerInitializedEvent event) {
        port = event.getWebServer().getPort();
    }
}