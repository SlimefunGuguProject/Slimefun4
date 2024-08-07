package city.norain.slimefun4.utils;

import city.norain.slimefun4.utils.pdctype.UUIDType;
import com.google.common.base.Preconditions;
import io.papermc.lib.PaperLib;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PDCUtil {
    public static final PersistentDataType<byte[], UUID> UUID_TYPE = new UUIDType();

    private PDCUtil() throws IllegalAccessException {
        throw new IllegalAccessException("Utility Class");
    }

    public static <T, Z> void setValue(
            @Nonnull Block block,
            @Nonnull PersistentDataType<T, Z> type,
            @Nonnull NamespacedKey key,
            @Nonnull Z value) {
        var state = (TileState) PaperLib.getBlockState(block, false);
        var container = state.getPersistentDataContainer();

        setValue(container, type, key, value);
    }

    public static <T, Z> void setValue(
            @Nonnull PersistentDataContainer container,
            @Nonnull PersistentDataType<T, Z> type,
            @Nonnull NamespacedKey key,
            @Nonnull Z value) {
        Preconditions.checkArgument(container != null, "Container cannot be null!");
        Preconditions.checkArgument(type != null, "PDC type cannot be null!");
        Preconditions.checkArgument(key != null, "PDC key cannot be null!");

        container.set(key, type, value);
    }

    @Nullable public static <T, Z> Z getValue(
            @Nonnull Block block, @Nonnull PersistentDataType<T, Z> type, @Nonnull NamespacedKey key) {
        var state = (TileState) PaperLib.getBlockState(block, false);
        var container = state.getPersistentDataContainer();

        return getValue(container, type, key);
    }

    @Nullable public static <T, Z> Z getValue(
            @Nonnull PersistentDataContainer container,
            @Nonnull PersistentDataType<T, Z> type,
            @Nonnull NamespacedKey key) {
        Preconditions.checkArgument(container != null, "Container cannot be null!");
        Preconditions.checkArgument(type != null, "PDC type cannot be null!");
        Preconditions.checkArgument(key != null, "PDC key cannot be null!");

        if (container.has(key, type)) {
            return container.get(key, type);
        } else {
            return null;
        }
    }
}
