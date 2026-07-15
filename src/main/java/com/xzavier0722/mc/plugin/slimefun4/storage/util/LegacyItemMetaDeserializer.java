package com.xzavier0722.mc.plugin.slimefun4.storage.util;

import com.google.gson.JsonParser;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public final class LegacyItemMetaDeserializer implements ConfigurationSerializable {
    static final String ITEM_META_ALIAS = "ItemMeta";
    private static final String PUBLIC_BUKKIT_VALUES = "PublicBukkitValues";
    private static final Pattern UNQUOTED_SNBT_STRING = Pattern.compile("[A-Za-z0-9._+-]+");
    private static Class<? extends ConfigurationSerializable> delegate;

    private LegacyItemMetaDeserializer() {}

    static void setDelegate(Class<? extends ConfigurationSerializable> delegateClass) {
        delegate = delegateClass;
    }

    @SuppressWarnings("unchecked")
    public static ConfigurationSerializable deserialize(Map<String, Object> serializedMeta) {
        serializedMeta = new LinkedHashMap<>(serializedMeta);
        var replacements = new LinkedHashMap<String, String>();
        LegacySkullProfileDataFixer.LegacyProfile legacyProfile = null;
        var publicBukkitValues = serializedMeta.get(PUBLIC_BUKKIT_VALUES);
        if (publicBukkitValues instanceof Map<?, ?> rawValues) {
            sanitizeMap((Map<String, Object>) rawValues, replacements);
        }
        var internal = serializedMeta.get("internal");
        if (internal instanceof String encodedNbt) {
            legacyProfile = LegacySkullProfileDataFixer.extractProfile(encodedNbt);
            serializedMeta.put("internal", LegacySkullProfileDataFixer.fix(encodedNbt));
        }

        var itemMeta = ConfigurationSerialization.deserializeObject(serializedMeta, delegate);
        if (itemMeta instanceof ItemMeta bukkitMeta) {
            restoreLegacySkullProfile(bukkitMeta, legacyProfile);
            restoreLegacySkullOwner(bukkitMeta, serializedMeta.get("skull-owner"));
            restoreTopLevelStrings(bukkitMeta, replacements);
        }
        if (replacements.isEmpty()) {
            return itemMeta;
        }

        var normalizedMeta = new LinkedHashMap<>(itemMeta.serialize());
        var normalizedValues = normalizedMeta.get(PUBLIC_BUKKIT_VALUES);
        if (!(normalizedValues instanceof String snbt)) {
            throw new IllegalStateException("Paper did not serialize PublicBukkitValues as SNBT");
        }
        for (var replacement : replacements.entrySet()) {
            snbt = snbt.replace(quoteSnbtString(replacement.getKey()), quoteSnbtString(replacement.getValue()));
        }
        normalizedMeta.put(PUBLIC_BUKKIT_VALUES, snbt);
        return ConfigurationSerialization.deserializeObject(normalizedMeta, delegate);
    }

    @SuppressWarnings("deprecation")
    private static void restoreLegacySkullProfile(
            ItemMeta itemMeta, LegacySkullProfileDataFixer.LegacyProfile legacyProfile) {
        if (!(itemMeta instanceof SkullMeta skullMeta) || legacyProfile == null) {
            return;
        }

        var profile = Bukkit.createProfile(legacyProfile.uniqueId(), legacyProfile.name());
        for (var property : legacyProfile.properties()) {
            if (property.name().equals("textures")) {
                restoreTextures(profile, property.value());
            }
        }
        skullMeta.setPlayerProfile((com.destroystokyo.paper.profile.PlayerProfile) profile);
    }

    private static void restoreTextures(org.bukkit.profile.PlayerProfile profile, String encodedTextures) {
        try {
            var json = new String(Base64.getDecoder().decode(encodedTextures), StandardCharsets.UTF_8);
            var texturesJson = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures");
            var textures = profile.getTextures();
            if (texturesJson.has("SKIN")) {
                var skinUrl = texturesJson.getAsJsonObject("SKIN").get("url").getAsString();
                textures.setSkin(URI.create(skinUrl).toURL());
            }
            if (texturesJson.has("CAPE")) {
                var capeUrl = texturesJson.getAsJsonObject("CAPE").get("url").getAsString();
                textures.setCape(URI.create(capeUrl).toURL());
            }
            profile.setTextures(textures);
        } catch (IllegalArgumentException | IllegalStateException | java.io.IOException ignored) {
            // Keep the UUID/name even if a malformed legacy texture property cannot be restored.
        }
    }

    @SuppressWarnings("deprecation")
    private static void restoreLegacySkullOwner(ItemMeta itemMeta, Object serializedOwner) {
        if (itemMeta instanceof SkullMeta skullMeta
                && serializedOwner instanceof String owner
                && !owner.isBlank()
                && !skullMeta.hasOwner()) {
            skullMeta.setOwner(owner);
        }
    }

    private static void restoreTopLevelStrings(ItemMeta itemMeta, Map<String, String> replacements) {
        var persistentData = itemMeta.getPersistentDataContainer();
        for (var key : persistentData.getKeys()) {
            if (!persistentData.has(key, PersistentDataType.STRING)) {
                continue;
            }
            var token = persistentData.get(key, PersistentDataType.STRING);
            var original = replacements.remove(token);
            if (original != null) {
                persistentData.set(key, PersistentDataType.STRING, original);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Object sanitizePersistentValue(Object value, Map<String, String> replacements) {
        if (value instanceof Map<?, ?> map) {
            sanitizeMap((Map<String, Object>) map, replacements);
        } else if (value instanceof List<?> list) {
            var values = (List<Object>) list;
            values.replaceAll(element -> sanitizePersistentValue(element, replacements));
        } else if (value instanceof String stringValue && needsQuoting(stringValue)) {
            var token = "sflegacystring" + UUID.randomUUID().toString().replace("-", "");
            replacements.put(token, stringValue);
            return token;
        }
        return value;
    }

    private static void sanitizeMap(Map<String, Object> values, Map<String, String> replacements) {
        values.replaceAll((key, value) -> sanitizePersistentValue(value, replacements));
    }

    private static String quoteSnbtString(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static boolean needsQuoting(String value) {
        return value.isEmpty()
                || isUuid(value)
                || (!UNQUOTED_SNBT_STRING.matcher(value).matches() && !isStructuredOrQuotedSnbt(value));
    }

    private static boolean isStructuredOrQuotedSnbt(String value) {
        if (value.length() < 2) {
            return false;
        }
        var first = value.charAt(0);
        var last = value.charAt(value.length() - 1);
        return (first == '{' && last == '}')
                || (first == '[' && last == ']')
                || (first == '"' && last == '"')
                || (first == '\'' && last == '\'');
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        throw new UnsupportedOperationException("LegacyItemMetaDeserializer is only used for deserialization");
    }
}
