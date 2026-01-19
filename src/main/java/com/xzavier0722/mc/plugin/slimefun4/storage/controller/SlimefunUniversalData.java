package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import city.norain.slimefun4.api.menu.UniversalMenu;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalDataTrait;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.inventory.ItemStack;

@Slf4j
@Getter
public class SlimefunUniversalData extends ASlimefunDataContainer {
    @Setter
    private volatile UniversalMenu menu;

    private final Set<UniversalDataTrait> traits = new HashSet<>();

    @ParametersAreNonnullByDefault
    SlimefunUniversalData(UUID uuid, String sfId) {
        super(uuid.toString(), sfId);
    }

    @ParametersAreNonnullByDefault
    SlimefunUniversalData(UUID uuid, String sfId, Set<UniversalDataTrait> traits) {
        super(uuid.toString(), sfId);
        this.traits.addAll(traits);
    }

    @ParametersAreNonnullByDefault
    @SneakyThrows
    public void setData(String key, String val) {
        checkData();

        if (isPendingRemove()) {
            throw new IllegalStateException("不能修改即将删除的方块数据");
        }

        if (UniversalDataTrait.isReservedKey(key)) {
            throw new IllegalAccessException("不能修改当前受保护的方块数据键值对");
        }

        setCacheInternal(key, val, true);
        Slimefun.getDatabaseManager().getBlockDataController().scheduleDelayedUniversalDataUpdate(this, key);
    }

    @ParametersAreNonnullByDefault
    protected void setTraitData(UniversalDataTrait trait, String val) {
        checkData();

        if (!trait.getReservedKey().isEmpty()) {
            setCacheInternal(trait.getReservedKey(), val, true);

            Slimefun.getDatabaseManager()
                    .getBlockDataController()
                    .scheduleDelayedUniversalDataUpdate(this, trait.getReservedKey());
        }
    }

    @ParametersAreNonnullByDefault
    public void removeData(String key) {
        if (removeCacheInternal(key) != null || !isDataLoaded()) {
            Slimefun.getDatabaseManager().getBlockDataController().scheduleDelayedUniversalDataUpdate(this, key);
        }
    }

    @Nullable public ItemStack[] getMenuContents() {
        if (menu == null) {
            return null;
        }
        var re = new ItemStack[54];
        var presetSlots = menu.getPreset().getPresetSlots();
        var inv = menu.toInventory().getContents();
        for (var i = 0; i < inv.length; i++) {
            if (presetSlots.contains(i)) {
                continue;
            }
            re[i] = inv[i];
        }

        return re;
    }

    public UUID getUUID() {
        return UUID.fromString(getKey());
    }

    public void addTrait(UniversalDataTrait... trait) {
        traits.addAll(List.of(trait));
    }

    public boolean hasTrait(UniversalDataTrait trait) {
        return traits.contains(trait);
    }

    @Override
    public String toString() {
        return "SlimefunUniversalData [uuid= " + getUUID() + ", sfId=" + getSfId() + ", isPendingRemove="
                + isPendingRemove() + "]";
    }
}
