package com.xzavier0722.mc.plugin.slimefun4.storage.util;

import io.github.bakedlibs.dough.collections.Pair;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Getter
public class InvSnapshot {
    @Nonnull
    private final List<Pair<ItemStack, Integer>> snapshot;

    public InvSnapshot(List<Pair<ItemStack, Integer>> snapshot) {
        this.snapshot = List.copyOf(snapshot);
    }

    public InvSnapshot(Inventory inventory) {
        this.snapshot = InvStorageUtils.getInvSnapshot(inventory.getContents());
    }

    public InvSnapshot(ItemStack[] list) {
        this.snapshot = InvStorageUtils.getInvSnapshot(list);
    }

    public Set<Integer> getChangedSlots(ItemStack[] item) {
        return InvStorageUtils.getChangedSlots(snapshot, item);
    }

    public Set<Integer> getChangedSlots(Inventory inv) {
        return getChangedSlots(inv.getContents());
    }
}
