package city.norain.slimefun4.utils.pdctype;

import java.nio.ByteBuffer;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

public class UUIDType implements PersistentDataType<byte[], UUID> {

    @Nonnull
    @Override
    public Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Nonnull
    @Override
    public Class<UUID> getComplexType() {
        return UUID.class;
    }

    @Nonnull
    @Override
    public byte[] toPrimitive(final UUID complex, @Nonnull final PersistentDataAdapterContext context) {
        var bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(complex.getMostSignificantBits());
        bb.putLong(complex.getLeastSignificantBits());
        return bb.array();
    }

    @Nonnull
    @Override
    public UUID fromPrimitive(@Nonnull final byte[] primitive, @Nonnull final PersistentDataAdapterContext context) {
        var bb = ByteBuffer.wrap(primitive);
        var mostBits = bb.getLong();
        var leastBits = bb.getLong();
        return new UUID(mostBits, leastBits);
    }
}
