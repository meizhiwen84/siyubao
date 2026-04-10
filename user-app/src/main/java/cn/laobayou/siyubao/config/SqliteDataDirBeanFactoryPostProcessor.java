package cn.laobayou.siyubao.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class SqliteDataDirBeanFactoryPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (environment == null) return;
        String url = environment.getProperty("spring.datasource.url", "");
        String u = url == null ? "" : url.trim();
        if (!u.startsWith("jdbc:sqlite:")) return;
        String path = u.substring("jdbc:sqlite:".length());
        if (path.isEmpty() || ":memory:".equalsIgnoreCase(path)) return;

        File dbFile = new File(path);
        File parent = dbFile.getAbsoluteFile().getParentFile();
        if (parent != null) parent.mkdirs();

        try {
            File target = dbFile.getAbsoluteFile();
            File legacy = new File("./data/siyubao.db").getAbsoluteFile();
            if (!target.exists() && legacy.exists() && !sameFile(target, legacy)) {
                Path p = target.toPath();
                Files.createDirectories(p.getParent());
                Files.copy(legacy.toPath(), p);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean sameFile(File a, File b) {
        try {
            return a.getCanonicalFile().equals(b.getCanonicalFile());
        } catch (Exception e) {
            return a.getAbsolutePath().equalsIgnoreCase(b.getAbsolutePath());
        }
    }
}
