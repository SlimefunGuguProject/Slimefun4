package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataType;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.FieldKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordSet;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.DataUtils;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

public class ProfileDataController extends ADataController {
    private final BackpackCache backpackCache;
    private final Map<String, PlayerProfile> profileCache;
    private final Map<String, Runnable> invalidingBackpackTasks;

    ProfileDataController() {
        super(DataType.PLAYER_PROFILE);
        backpackCache = new BackpackCache();
        profileCache = new ConcurrentHashMap<>();
        invalidingBackpackTasks = new ConcurrentHashMap<>();
    }

    @Nullable public PlayerProfile getProfile(OfflinePlayer p) {
        checkDestroy();
        var uuid = p.getUniqueId().toString();
        var re = profileCache.get(uuid);
        if (re != null) {
            return re;
        }

        var key = new RecordKey(DataScope.PLAYER_PROFILE);
        key.addField(FieldKey.PLAYER_BACKPACK_NUM);
        key.addField(FieldKey.PLAYER_NAME);
        key.addCondition(FieldKey.PLAYER_UUID, uuid);

        var result = getData(key);
        if (result.isEmpty()) {
            return null;
        }

        // check player name changed or not
        var currentPlayerName = p.getName();
        if (currentPlayerName != null && !currentPlayerName.equals(result.get(0).get(FieldKey.PLAYER_NAME))) {
            updateUsername(uuid, currentPlayerName);
        }

        var bNum = result.get(0).getInt(FieldKey.BACKPACK_NUMBER);

        var researches = new HashSet<Research>();
        getUnlockedResearchKeys(uuid).forEach(rKey -> Research.getResearch(rKey).ifPresent(researches::add));

        re = new PlayerProfile(p, bNum, researches);
        profileCache.put(uuid, re);
        return re;
    }

    public void getProfileAsync(OfflinePlayer p, IAsyncReadCallback<PlayerProfile> callback) {
        scheduleReadTask(() -> invokeCallback(callback, getProfile(p)));
    }

    @Nullable public PlayerBackpack getBackpack(OfflinePlayer owner, int num) {
        checkDestroy();
        var uuid = owner.getUniqueId().toString();
        var re = backpackCache.get(uuid, num);
        if (re != null) {
            return re;
        }

        var key = new RecordKey(DataScope.BACKPACK_PROFILE);
        key.addField(FieldKey.BACKPACK_ID);
        key.addField(FieldKey.BACKPACK_SIZE);
        key.addField(FieldKey.BACKPACK_NAME);
        key.addCondition(FieldKey.PLAYER_UUID, uuid);
        key.addCondition(FieldKey.BACKPACK_NUMBER, num + "");

        var bResult = getData(key);
        if (bResult.isEmpty()) {
            return null;
        }

        var result = bResult.get(0);
        var size = Integer.parseInt(bResult.get(0).get(FieldKey.BACKPACK_SIZE));
        var idStr = result.get(FieldKey.BACKPACK_ID);

        re = new PlayerBackpack(
                owner,
                UUID.fromString(idStr),
                DataUtils.profileDataDebase64(result.getOrDef(FieldKey.BACKPACK_NAME, "")),
                num,
                size,
                getBackpackInv(idStr, size));
        backpackCache.put(re);
        return re;
    }

    @Nullable public PlayerBackpack getBackpack(String uuid) {
        var re = backpackCache.get(uuid);
        if (re != null) {
            return re;
        }

        var key = new RecordKey(DataScope.BACKPACK_PROFILE);
        key.addField(FieldKey.BACKPACK_ID);
        key.addField(FieldKey.BACKPACK_SIZE);
        key.addField(FieldKey.BACKPACK_NAME);
        key.addField(FieldKey.BACKPACK_NUMBER);
        key.addField(FieldKey.PLAYER_UUID);
        key.addCondition(FieldKey.BACKPACK_ID, uuid);

        var resultSet = getData(key);
        if (resultSet.isEmpty()) {
            return null;
        }

        var result = resultSet.get(0);
        var idStr = result.get(FieldKey.BACKPACK_ID);
        var size = result.getInt(FieldKey.BACKPACK_SIZE);

        re = new PlayerBackpack(
                Bukkit.getOfflinePlayer(UUID.fromString(result.get(FieldKey.PLAYER_UUID))),
                UUID.fromString(idStr),
                DataUtils.profileDataDebase64(result.getOrDef(FieldKey.BACKPACK_NAME, "")),
                result.getInt(FieldKey.BACKPACK_NUMBER),
                size,
                getBackpackInv(idStr, size));
        backpackCache.put(re);
        return re;
    }

    @Nonnull
    private ItemStack[] getBackpackInv(String uuid, int size) {
        var key = new RecordKey(DataScope.BACKPACK_INVENTORY);
        key.addField(FieldKey.INVENTORY_SLOT);
        key.addField(FieldKey.INVENTORY_ITEM);
        key.addCondition(FieldKey.BACKPACK_ID, uuid);

        var invResult = getData(key);
        var re = new ItemStack[size];
        invResult.forEach(
                each -> re[each.getInt(FieldKey.INVENTORY_SLOT)] = each.getItemStack(FieldKey.INVENTORY_ITEM));

        return re;
    }

    @Nonnull
    private Set<NamespacedKey> getUnlockedResearchKeys(String uuid) {
        var key = new RecordKey(DataScope.PLAYER_RESEARCH);
        key.addField(FieldKey.RESEARCH_ID);
        key.addCondition(FieldKey.PLAYER_UUID, uuid);

        var result = getData(key);
        if (result.isEmpty()) {
            return Collections.emptySet();
        }

        return result.stream()
                .map(record -> NamespacedKey.fromString(record.get(FieldKey.RESEARCH_ID)))
                .collect(Collectors.toSet());
    }

    public void getBackpackAsync(OfflinePlayer owner, int num, IAsyncReadCallback<PlayerBackpack> callback) {
        scheduleReadTask(() -> invokeCallback(callback, getBackpack(owner, num)));
    }

    public void getBackpackAsync(String uuid, IAsyncReadCallback<PlayerBackpack> callback) {
        scheduleReadTask(() -> invokeCallback(callback, getBackpack(uuid)));
    }

    @Nonnull
    public Set<PlayerBackpack> getBackpacks(String pUuid) {
        checkDestroy();
        var key = new RecordKey(DataScope.BACKPACK_PROFILE);
        key.addField(FieldKey.BACKPACK_ID);
        key.addCondition(FieldKey.PLAYER_UUID, pUuid);

        var result = getData(key);
        if (result.isEmpty()) {
            return Collections.emptySet();
        }

        var re = new HashSet<PlayerBackpack>();
        result.forEach(bUuid -> re.add(getBackpack(bUuid.get(FieldKey.BACKPACK_ID))));
        return re;
    }

    public void getBackpacksAsync(String pUuid, IAsyncReadCallback<Set<PlayerBackpack>> callback) {
        scheduleReadTask(() -> {
            var re = getBackpacks(pUuid);
            invokeCallback(callback, re.isEmpty() ? null : re);
        });
    }

    @Nonnull
    public PlayerProfile createProfile(OfflinePlayer p) {
        checkDestroy();
        var uuid = p.getUniqueId().toString();
        var cache = profileCache.get(uuid);
        if (cache != null) {
            return cache;
        }

        var re = new PlayerProfile(p, 0);
        profileCache.put(uuid, re);

        var key = new RecordKey(DataScope.PLAYER_PROFILE);
        key.addCondition(FieldKey.PLAYER_UUID, uuid);
        scheduleWriteTask(new UUIDKey(DataScope.NONE, p.getUniqueId()), key, getRecordSet(re), true);
        return re;
    }

    public void setResearch(String uuid, NamespacedKey researchKey, boolean unlocked) {
        var key = new RecordKey(DataScope.PLAYER_RESEARCH);
        key.addCondition(FieldKey.PLAYER_UUID, uuid);
        key.addCondition(FieldKey.RESEARCH_ID, researchKey.toString());
        if (unlocked) {
            var data = new RecordSet();
            data.put(FieldKey.PLAYER_UUID, uuid);
            data.put(FieldKey.RESEARCH_ID, researchKey.toString());
            scheduleWriteTask(new UUIDKey(DataScope.NONE, uuid), key, data, false);
        } else {
            scheduleDeleteTask(new UUIDKey(DataScope.NONE, uuid), key, false);
        }
    }

    @Nonnull
    public PlayerBackpack createBackpack(OfflinePlayer p, String name, int num, int size) {
        var re = new PlayerBackpack(p, UUID.randomUUID(), name, num, size, null);
        var key = new RecordKey(DataScope.BACKPACK_PROFILE);
        key.addCondition(FieldKey.BACKPACK_ID, re.getUniqueId().toString());
        scheduleWriteTask(new UUIDKey(DataScope.NONE, p.getUniqueId()), key, getRecordSet(re), true);
        return re;
    }

    public void saveBackpackInfo(PlayerBackpack bp) {
        var key = new RecordKey(DataScope.BACKPACK_PROFILE);
        key.addCondition(FieldKey.BACKPACK_ID, bp.getUniqueId().toString());
        key.addField(FieldKey.BACKPACK_SIZE);
        key.addField(FieldKey.BACKPACK_NAME);
        scheduleWriteTask(new UUIDKey(DataScope.NONE, bp.getOwner().getUniqueId()), key, getRecordSet(bp), false);
    }

    public void saveProfileBackpackCount(PlayerProfile profile) {
        var key = new RecordKey(DataScope.PLAYER_PROFILE);
        key.addField(FieldKey.PLAYER_BACKPACK_NUM);
        var uuid = profile.getUUID();
        key.addCondition(FieldKey.PLAYER_UUID, uuid.toString());
        scheduleWriteTask(new UUIDKey(DataScope.NONE, uuid), key, getRecordSet(profile), false);
    }

    public void saveBackpackInventory(PlayerBackpack bp, Set<Integer> slots) {
        var id = bp.getUniqueId().toString();
        var inv = bp.getInventory();
        slots.forEach(slot -> {
            var key = new RecordKey(DataScope.BACKPACK_INVENTORY);
            key.addCondition(FieldKey.BACKPACK_ID, id);
            key.addCondition(FieldKey.INVENTORY_SLOT, slot + "");
            key.addField(FieldKey.INVENTORY_ITEM);
            var is = inv.getItem(slot);
            if (is == null) {
                scheduleDeleteTask(new UUIDKey(DataScope.NONE, bp.getOwner().getUniqueId()), key, false);
            } else {
                try {
                    var data = new RecordSet();
                    data.put(FieldKey.BACKPACK_ID, id);
                    data.put(FieldKey.INVENTORY_SLOT, slot + "");
                    data.put(FieldKey.INVENTORY_ITEM, is);
                    scheduleWriteTask(new UUIDKey(DataScope.NONE, bp.getOwner().getUniqueId()), key, data, false);
                } catch (IllegalArgumentException e) {
                    Slimefun.logger().log(Level.WARNING, e.getMessage());
                }
            }
        });
    }

    public void saveBackpackInventory(PlayerBackpack bp, Integer... slots) {
        saveBackpackInventory(bp, Set.of(slots));
    }

    public UUID getPlayerUuid(String pName) {
        checkDestroy();
        var key = new RecordKey(DataScope.PLAYER_PROFILE);
        key.addField(FieldKey.PLAYER_UUID);
        key.addCondition(FieldKey.PLAYER_NAME, pName);

        var result = getData(key);
        if (result.isEmpty()) {
            return null;
        }

        return UUID.fromString(result.get(0).get(FieldKey.PLAYER_UUID));
    }

    public void getPlayerUuidAsync(String pName, IAsyncReadCallback<UUID> callback) {
        scheduleReadTask(() -> invokeCallback(callback, getPlayerUuid(pName)));
    }

    public void updateUsername(String uuid, String newName) {
        var key = new RecordKey(DataScope.PLAYER_PROFILE);
        key.addField(FieldKey.PLAYER_NAME);
        key.addCondition(FieldKey.PLAYER_UUID, uuid);

        var data = new RecordSet();
        data.put(FieldKey.PLAYER_NAME, newName);
        data.put(FieldKey.PLAYER_UUID, uuid);

        scheduleWriteTask(new UUIDKey(DataScope.NONE, uuid), key, data, false);
    }

    private static RecordSet getRecordSet(PlayerBackpack bp) {
        var re = new RecordSet();
        re.put(FieldKey.PLAYER_UUID, bp.getOwner().getUniqueId().toString());
        re.put(FieldKey.BACKPACK_ID, bp.getUniqueId().toString());
        re.put(FieldKey.BACKPACK_NUMBER, bp.getId() + "");
        re.put(FieldKey.BACKPACK_SIZE, bp.getSize() + "");
        re.put(FieldKey.BACKPACK_NAME, DataUtils.profileDataBase64(bp.getName()));
        return re;
    }

    private static RecordSet getRecordSet(PlayerProfile profile) {
        var re = new RecordSet();
        re.put(FieldKey.PLAYER_UUID, profile.getUUID().toString());
        re.put(FieldKey.PLAYER_NAME, profile.getOwner().getName());
        re.put(FieldKey.PLAYER_BACKPACK_NUM, profile.getBackpackCount() + "");
        return re;
    }

    public void invalidateCache(String pUuid) {
        var removed = profileCache.remove(pUuid);
        if (removed != null) {
            removed.markInvalid();
        }

        var task = new Runnable() {
            @Override
            public void run() {
                if (invalidingBackpackTasks.remove(pUuid) != this) {
                    return;
                }

                if (Bukkit.getOfflinePlayer(UUID.fromString(pUuid)).isConnected()) {
                    return;
                }

                backpackCache.invalidate(pUuid);
            }
        };
        invalidingBackpackTasks.put(pUuid, task);
        scheduleWriteTask(task);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        backpackCache.clean();
        profileCache.clear();
    }
}
