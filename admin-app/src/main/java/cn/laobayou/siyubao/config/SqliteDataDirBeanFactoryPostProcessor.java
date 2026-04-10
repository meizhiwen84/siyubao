package cn.laobayou.siyubao.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;

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
    }
}
