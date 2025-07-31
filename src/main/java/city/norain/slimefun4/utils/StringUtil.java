package city.norain.slimefun4.utils;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalDataTrait;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
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
}
