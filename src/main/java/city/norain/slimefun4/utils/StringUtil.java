package city.norain.slimefun4.utils;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalDataTrait;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.inventory.ItemStack;

public class StringUtil {
    public static String itemStackToString(ItemStack item) {
        if (item == null) {
            return "null";
        }

        var sfItem = SlimefunItem.getByItem(item);

        if (sfItem != null) {
            return String.format(
                    "ItemStack [sfId=%s (Addon=%s)] (type=%s, amount=%d)",
                    sfItem.getId(), sfItem.getAddon().getName(), item.getType(), item.getAmount());
        } else {
            return String.format("ItemStack (type=%s, amount=%d)", item.getType(), item.getAmount());
        }
    }

    public static Set<UniversalDataTrait> getTraitsFromStr(String str) {
        if (str == null || str.isEmpty()) {
            return new HashSet<>();
        }

        var traits = new HashSet<UniversalDataTrait>();

        for (String t : str.split(",")) {
            try {
                var trait = UniversalDataTrait.valueOf(t.toUpperCase());
                traits.add(trait);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return traits;
    }

    public static String getTraitsStr(Set<UniversalDataTrait> traits) {
        if (traits == null || traits.isEmpty()) {
            return "";
        }

        return String.join(",", traits.stream().map(Enum::name).toList());
    }

    /**
     * 格式化详细线程信息
     */
    public static String formatDetailedThreadInfo(ThreadInfo threadInfo) {
        StringBuilder sb = new StringBuilder();

        // 线程名和基本信息
        sb.append(threadInfo.getThreadName());
        sb.append(" #").append(threadInfo.getThreadId());
        sb.append(" ").append(threadInfo.getThreadState());

        // 锁信息
        if (threadInfo.getLockName() != null) {
            sb.append(" on ").append(threadInfo.getLockName());
            if (threadInfo.getLockOwnerName() != null) {
                sb.append(" owned by \"").append(threadInfo.getLockOwnerName()).append("\"");
                sb.append(" #").append(threadInfo.getLockOwnerId());
            }
        }

        sb.append("\n");

        // 线程状态详情
        sb.append("   Thread.State: ").append(threadInfo.getThreadState());
        if (threadInfo.getBlockedTime() > 0) {
            sb.append(" (blocked for ").append(threadInfo.getBlockedTime()).append("ms)");
        }
        if (threadInfo.getWaitedTime() > 0) {
            sb.append(" (waited for ").append(threadInfo.getWaitedTime()).append("ms)");
        }
        sb.append("\n");

        // 堆栈跟踪
        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            sb.append("\tat ").append(element.toString()).append("\n");

            // 第一个堆栈元素的锁信息
            if (i == 0 && threadInfo.getLockName() != null) {
                sb.append("\t- ").append(getThreadStateDescription(threadInfo)).append("\n");
            }
        }

        // 持有的锁
        if (threadInfo.getLockedMonitors().length > 0) {
            sb.append("   Locked monitors:\n");
            Arrays.stream(threadInfo.getLockedMonitors())
                    .forEach(monitor ->
                            sb.append("\t- locked ").append(monitor.toString()).append("\n"));
        }

        // 持有的同步器
        if (threadInfo.getLockedSynchronizers().length > 0) {
            sb.append("   Locked synchronizers:\n");
            Arrays.stream(threadInfo.getLockedSynchronizers())
                    .forEach(sync -> sb.append("\t- ").append(sync.toString()).append("\n"));
        }

        return sb.toString();
    }

    private static String getThreadStateDescription(ThreadInfo threadInfo) {
        switch (threadInfo.getThreadState()) {
            case BLOCKED:
                return "blocked on " + threadInfo.getLockName();
            case WAITING, TIMED_WAITING:
                return "waiting on " + threadInfo.getLockName();
            default:
                return threadInfo.getThreadState().toString().toLowerCase();
        }
    }
}
