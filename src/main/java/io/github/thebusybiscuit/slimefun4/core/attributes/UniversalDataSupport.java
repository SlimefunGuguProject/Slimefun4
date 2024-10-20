package io.github.thebusybiscuit.slimefun4.core.attributes;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This attribute represents {@link SlimefunItem} support store
 * {@link SlimefunUniversalData}
 *
 * The {@link SlimefunItem} to be universal is encouraged be able to use PDC.
 * Otherwise, it will lose its uuid to identify.
 *
 * Check here to find out what type of block can support this:
 * <a href="https://jd.papermc.io/paper/1.21/org/bukkit/block/TileState.html">Paper Doc</a>
 *
 * @author NoRainCity
 *
 * @see SlimefunUniversalData
 */
public interface UniversalDataSupport {}
