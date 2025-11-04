package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalDataTrait;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.LocationUtils;
import io.github.bakedlibs.dough.blocks.BlockPosition;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class SlimefunUniversalBlockData extends SlimefunUniversalData {
    private volatile BlockPosition lastPresent;

    public SlimefunUniversalBlockData(UUID uuid, String sfId) {
        super(uuid, sfId);
    }

    public SlimefunUniversalBlockData(UUID uuid, String sfId, BlockPosition present) {
        super(uuid, sfId);

        this.lastPresent = present;
    }

    public SlimefunUniversalBlockData(UUID uuid, String sfId, Location present) {
        this(uuid, sfId, new BlockPosition(present));
    }

    public void initTraits() {
        addTrait(UniversalDataTrait.BLOCK, UniversalDataTrait.INVENTORY);
    }

    public void initLastPresent() {
        setTraitData(UniversalDataTrait.BLOCK, LocationUtils.getLocKey(lastPresent.toLocation()));
    }

    public void setLastPresent(BlockPosition bp) {
        this.lastPresent = bp;
        setTraitData(UniversalDataTrait.BLOCK, LocationUtils.getLocKey(bp.toLocation()));
    }

    public void setLastPresent(Location l) {
        setLastPresent(new BlockPosition(l));
    }

    public BlockPosition getLastPresent() {
        if (!isDataLoaded()) {
            return null;
        }

        var data = getData(UniversalDataTrait.BLOCK.getReservedKey());

        // 自动修复丢失的位置数据
        if (lastPresent != null) {
            if (data == null) {
                setTraitData(UniversalDataTrait.BLOCK, LocationUtils.getLocKey(lastPresent.toLocation()));
            }

            return lastPresent;
        }

        if (data == null) {
            return null;
        }

        try {
            lastPresent = new BlockPosition(LocationUtils.toLocation(data));
        } catch (RuntimeException e) {
            if (data.isEmpty()) {
                return null;
            }

            // 修复因使用不一致的文本转换导致的位置无法解析
            oldLocationFix(data);
        }

        return lastPresent;
    }

    private void oldLocationFix(String data) {
        try {
            var lArr = data.split(",");
            var bp = new BlockPosition(
                    Bukkit.getWorld(lArr[0].replace("[world=", "")),
                    (int) Double.parseDouble(lArr[1].replace("x=", "")),
                    (int) Double.parseDouble(lArr[2].replace("y=", "")),
                    (int) Double.parseDouble(lArr[3].replace("z=", "").replace("]", "")));

            setTraitData(UniversalDataTrait.BLOCK, LocationUtils.getLocKey(bp.toLocation()));

            lastPresent = bp;
        } catch (Exception x) {
            throw new RuntimeException("Unable to fix location " + data + ", it might be broken", x);
        }
    }

    @Override
    public String toString() {
        return "SlimefunUniversalBlockData [uuid= " + getUUID() + ", sfId=" + getSfId() + ", isPendingRemove="
                + isPendingRemove() + ", lastPresent=" + lastPresent + ", hasMenu=" + (getMenu() != null) + ", traits="
                + getTraits() + "]";
    }
}
