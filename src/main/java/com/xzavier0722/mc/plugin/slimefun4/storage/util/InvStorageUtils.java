package com.xzavier0722.mc.plugin.slimefun4.storage.util;

import io.github.bakedlibs.dough.collections.Pair;
import io.github.thebusybiscuit.slimefun4.core.debug.Debug;
import io.github.thebusybiscuit.slimefun4.core.debug.TestCase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import org.bukkit.inventory.ItemStack;

public class InvStorageUtils {
    private static final Pair<ItemStack, Integer> emptyPair = new Pair<>(null, 0);

    public static Set<Integer> getChangedSlots(InvSnapshot snapshot, ItemStack[] currContent) {
        return getChangedSlots(snapshot == null ? null : snapshot.getSnapshot(), currContent);
    }

    public static Set<Integer> getChangedSlots(List<Pair<ItemStack, Integer>> snapshot, ItemStack[] currContent) {
        var isEmptySnapshot = (snapshot == null || snapshot.isEmpty());
        if (isEmptySnapshot && currContent == null) {
            return Collections.emptySet();
        }

        var re = new HashSet<Integer>();
        if (isEmptySnapshot) {
            for (var i = 0; i < currContent.length; i++) {
                re.add(i);
            }
            return re;
        }

        if (currContent == null) {
            for (var i = 0; i < snapshot.size(); i++) {
                re.add(i);
            }
            return re;
        }

        var size = currContent.length;
        var snapshotSize = snapshot.size();
        if (snapshotSize > size) {
            for (var i = size; i < snapshotSize; i++) {
                re.add(i);
            }
        }

        for (var i = 0; i < size; i++) {
            var each = i < snapshotSize ? snapshot.get(i) : emptyPair;
            var curr = currContent[i];
            if (curr == null) {
                if (each.getFirstValue() != null) {
                    re.add(i);
                }
                continue;
            }
            // fix: #1099 more strict difference check
            if (curr.getAmount() != each.getSecondValue() || !Objects.equals(curr, each.getFirstValue())) {
                re.add(i);
            }
        }

        Debug.log(TestCase.BACKPACK, "changedSlots: " + re);

        return re;
    }

    @Nonnull
    public static List<Pair<ItemStack, Integer>> getInvSnapshot(ItemStack[] invContents) {
        var re = new ArrayList<Pair<ItemStack, Integer>>(invContents.length);
        for (var each : invContents) {
            // fix: in case some addons directly manipulate origin ItemStack
            // fix: # 1099 bundles may change their meta internally without a new itemstack instance
            re.add(each == null ? emptyPair : new Pair<>(each.clone(), each.getAmount()));
        }

        return re;
    }
}
