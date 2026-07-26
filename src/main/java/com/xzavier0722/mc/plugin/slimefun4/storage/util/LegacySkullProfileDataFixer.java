package com.xzavier0722.mc.plugin.slimefun4.storage.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class LegacySkullProfileDataFixer {
    private static final int TAG_END = 0;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;
    private static final int TAG_INT_ARRAY = 11;

    private LegacySkullProfileDataFixer() {}

    static String fix(String encodedNbt) {
        try {
            var decoded = Base64.getDecoder().decode(encodedNbt);
            var fixed = new ByteArrayOutputStream(decoded.length);
            boolean changed;
            try (var input = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(decoded)));
                    var gzip = new GZIPOutputStream(fixed);
                    var output = new DataOutputStream(gzip)) {
                var rootType = input.readUnsignedByte();
                output.writeByte(rootType);
                output.writeUTF(input.readUTF());
                changed = copyPayload(rootType, input, output, false);
            }
            return changed ? Base64.getEncoder().encodeToString(fixed.toByteArray()) : encodedNbt;
        } catch (IOException | IllegalArgumentException ignored) {
            return encodedNbt;
        }
    }

    static LegacyProfile extractProfile(String encodedNbt) {
        try {
            var collector = new ProfileCollector();
            var decoded = Base64.getDecoder().decode(encodedNbt);
            try (var input = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(decoded)))) {
                var rootType = input.readUnsignedByte();
                input.readUTF();
                readPayload(rootType, input, "", collector, null);
            }
            return collector.toProfile();
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void readPayload(
            int type, DataInputStream input, String path, ProfileCollector collector, PropertyCollector property)
            throws IOException {
        switch (type) {
            case 1 -> input.readByte();
            case 2 -> input.readShort();
            case 3 -> input.readInt();
            case 4 -> input.readLong();
            case 5 -> input.readFloat();
            case 6 -> input.readDouble();
            case 7 -> input.skipNBytes(input.readInt());
            case TAG_STRING -> input.readUTF();
            case TAG_LIST -> readList(input, path, collector);
            case TAG_COMPOUND -> readCompound(input, path, collector, property);
            case TAG_INT_ARRAY -> input.skipNBytes((long) input.readInt() * Integer.BYTES);
            case 12 -> input.skipNBytes((long) input.readInt() * Long.BYTES);
            default -> throw new IOException("Unsupported NBT tag type: " + type);
        }
    }

    private static void readCompound(
            DataInputStream input, String path, ProfileCollector collector, PropertyCollector property)
            throws IOException {
        while (true) {
            var type = input.readUnsignedByte();
            if (type == TAG_END) {
                return;
            }
            var name = input.readUTF();
            if (path.equals("SkullProfile") && name.equals("Id") && type == TAG_INT_ARRAY) {
                var length = input.readInt();
                if (length != 4) {
                    throw new IOException("Invalid legacy SkullProfile UUID length: " + length);
                }
                var mostSignificantBits = ((long) input.readInt() << 32) | (input.readInt() & 0xFFFFFFFFL);
                var leastSignificantBits = ((long) input.readInt() << 32) | (input.readInt() & 0xFFFFFFFFL);
                collector.uniqueId = new UUID(mostSignificantBits, leastSignificantBits);
            } else if (type == TAG_STRING) {
                var value = input.readUTF();
                if (path.equals("SkullProfile") && name.equals("Name")) {
                    collector.name = value;
                } else if (property != null && name.equals("Value")) {
                    property.value = value;
                } else if (property != null && name.equals("Signature")) {
                    property.signature = value;
                }
            } else {
                var childPath = path.isEmpty() ? name : path + '.' + name;
                readPayload(type, input, childPath, collector, property);
            }
        }
    }

    private static void readList(DataInputStream input, String path, ProfileCollector collector) throws IOException {
        var elementType = input.readUnsignedByte();
        var length = input.readInt();
        var propertyName = path.startsWith("SkullProfile.Properties.")
                ? path.substring("SkullProfile.Properties.".length())
                : null;
        for (var i = 0; i < length; i++) {
            var property = propertyName == null ? null : new PropertyCollector(propertyName);
            readPayload(elementType, input, path, collector, property);
            if (property != null && property.value != null) {
                collector.properties.add(new LegacyProperty(property.name, property.value, property.signature));
            }
        }
    }

    private static boolean copyPayload(int type, DataInputStream input, DataOutputStream output, boolean skullProfile)
            throws IOException {
        return switch (type) {
            case 1 -> {
                output.writeByte(input.readByte());
                yield false;
            }
            case 2 -> {
                output.writeShort(input.readShort());
                yield false;
            }
            case 3 -> {
                output.writeInt(input.readInt());
                yield false;
            }
            case 4 -> {
                output.writeLong(input.readLong());
                yield false;
            }
            case 5 -> {
                output.writeFloat(input.readFloat());
                yield false;
            }
            case 6 -> {
                output.writeDouble(input.readDouble());
                yield false;
            }
            case 7 -> copyByteArray(input, output);
            case TAG_STRING -> {
                output.writeUTF(input.readUTF());
                yield false;
            }
            case TAG_LIST -> copyList(input, output);
            case TAG_COMPOUND -> copyCompound(input, output, skullProfile);
            case TAG_INT_ARRAY -> copyIntArray(input, output);
            case 12 -> copyLongArray(input, output);
            default -> throw new IOException("Unsupported NBT tag type: " + type);
        };
    }

    private static boolean copyCompound(DataInputStream input, DataOutputStream output, boolean skullProfile)
            throws IOException {
        var changed = false;
        while (true) {
            var type = input.readUnsignedByte();
            if (type == TAG_END) {
                output.writeByte(TAG_END);
                return changed;
            }

            var name = input.readUTF();
            if (skullProfile && type == TAG_INT_ARRAY && name.equals("Id")) {
                var length = input.readInt();
                if (length != 4) {
                    throw new IOException("Invalid legacy SkullProfile UUID length: " + length);
                }
                var mostSignificantBits = ((long) input.readInt() << 32) | (input.readInt() & 0xFFFFFFFFL);
                var leastSignificantBits = ((long) input.readInt() << 32) | (input.readInt() & 0xFFFFFFFFL);
                output.writeByte(TAG_STRING);
                output.writeUTF(name);
                output.writeUTF(new UUID(mostSignificantBits, leastSignificantBits).toString());
                changed = true;
                continue;
            }

            output.writeByte(type);
            output.writeUTF(name);
            changed |= copyPayload(type, input, output, type == TAG_COMPOUND && name.equals("SkullProfile"));
        }
    }

    private static boolean copyList(DataInputStream input, DataOutputStream output) throws IOException {
        var elementType = input.readUnsignedByte();
        var length = input.readInt();
        output.writeByte(elementType);
        output.writeInt(length);
        var changed = false;
        for (var i = 0; i < length; i++) {
            changed |= copyPayload(elementType, input, output, false);
        }
        return changed;
    }

    private static boolean copyByteArray(DataInputStream input, DataOutputStream output) throws IOException {
        var length = input.readInt();
        output.writeInt(length);
        output.write(input.readNBytes(length));
        return false;
    }

    private static boolean copyIntArray(DataInputStream input, DataOutputStream output) throws IOException {
        var length = input.readInt();
        output.writeInt(length);
        for (var i = 0; i < length; i++) {
            output.writeInt(input.readInt());
        }
        return false;
    }

    private static boolean copyLongArray(DataInputStream input, DataOutputStream output) throws IOException {
        var length = input.readInt();
        output.writeInt(length);
        for (var i = 0; i < length; i++) {
            output.writeLong(input.readLong());
        }
        return false;
    }

    record LegacyProfile(UUID uniqueId, String name, List<LegacyProperty> properties) {}

    record LegacyProperty(String name, String value, String signature) {}

    private static final class ProfileCollector {
        private UUID uniqueId;
        private String name;
        private final List<LegacyProperty> properties = new ArrayList<>();

        private LegacyProfile toProfile() {
            return uniqueId == null && name == null && properties.isEmpty()
                    ? null
                    : new LegacyProfile(uniqueId, name, List.copyOf(properties));
        }
    }

    private static final class PropertyCollector {
        private final String name;
        private String value;
        private String signature;

        private PropertyCollector(String name) {
            this.name = name;
        }
    }
}
