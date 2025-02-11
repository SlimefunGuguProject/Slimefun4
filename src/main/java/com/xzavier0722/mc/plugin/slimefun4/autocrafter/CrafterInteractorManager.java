package com.xzavier0722.mc.plugin.slimefun4.autocrafter;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.Block;

/**
 *
 * This manager provide accessibility to custom interactors.
 *
 * @author Xzavier0722
 *
 * @see CrafterInteractable
 * @see CrafterInteractorHandler
 *
 */
public class CrafterInteractorManager {

    private static final Map<String, CrafterInteractorHandler> handlers = Collections.synchronizedMap(new HashMap<>());

    /**
     * Register the specific slimefun item as crafter interactor.
     * @param id: the id of the slimefun item that will be registered as interactor.
     * @param handler: way to get the {@link CrafterInteractable} implementation.
     *
     * @see CrafterInteractorHandler
     */
    public static void register(String id, CrafterInteractorHandler handler) {
        handlers.put(id, handler);
    }

    public static CrafterInteractorHandler getHandler(String id) {
        return handlers.get(id);
    }

    public static CrafterInteractable getInteractor(Block b) {
        if (hasInterator(b)) {
            var blockData = StorageCacheUtils.getBlock(b.getLocation());
            CrafterInteractorHandler handler = handlers.get(blockData.getSfId());
            return handler.getInteractor(blockData.getBlockMenu());
        }
        return null;
    }

    public static boolean hasInterator(Block b) {
        var blockData = StorageCacheUtils.getBlock(b.getLocation());
        return blockData != null && handlers.containsKey(blockData.getSfId());
    }
}
