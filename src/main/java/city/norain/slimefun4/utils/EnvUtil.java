package city.norain.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EnvUtil {
    public static Properties gitInfo = null;

    public void init() {
        try (var resource = Slimefun.class.getResourceAsStream("/git.properties")) {
            if (resource == null) {
                Slimefun.logger().warning("无法加载构建信息: 未找到 /git.properties");
                return;
            }

            var prop = new Properties();
            prop.load(resource);

            gitInfo = prop;
        } catch (IOException e) {
            Slimefun.logger().log(Level.WARNING, "无法加载构建信息", e);
        }
    }

    private String getProperty(String key) {
        if (gitInfo == null) {
            return "unknown";
        }

        String value = gitInfo.getProperty(key);
        return value == null || value.isBlank() ? "unknown" : value;
    }

    public String getBuildTime() {
        return getProperty("git.build.time");
    }

    public String getBuildCommitID() {
        return getProperty("git.commit.id.abbrev");
    }

    public String getBranch() {
        return getProperty("git.branch");
    }
}
