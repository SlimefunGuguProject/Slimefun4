package io.github.thebusybiscuit.slimefun4.api;

import city.norain.slimefun4.SlimefunExtended;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import javax.annotation.Nonnull;
import org.apache.commons.lang.Validate;
import org.bukkit.Server;

/**
 * This enum holds all versions of Minecraft that we currently support.
 *
 * @author TheBusyBiscuit
 * @author Walshy
 * @see Slimefun
 */
public enum MinecraftVersion {

    /**
     * This constant represents Minecraft (Java Edition) Version 1.16
     * (The "Nether Update")
     */
    MINECRAFT_1_16(1, 16, "1.16.x"),

    /**
     * This constant represents Minecraft (Java Edition) Version 1.17
     * (The "Caves and Cliffs: Part I" Update)
     */
    MINECRAFT_1_17(1, 17, "1.17.x"),

    /**
     * This constant represents Minecraft (Java Edition) Version 1.18
     * (The "Caves and Cliffs: Part II" Update)
     */
    MINECRAFT_1_18(1, 18, "1.18.x"),

    /**
     * This constant represents Minecraft (Java Edition) Version 1.19
     * ("The Wild Update")
     */
    MINECRAFT_1_19(1, 19, "1.19.x"),

    /**
     * This constant represents Minecraft (Java Edition) Version 1.20
     * ("The Trails &amp; Tales Update")
     */
    MINECRAFT_1_20(1, 20, "1.20.x"),

    /**
     * This constant represents Minecraft (Java Edition) Version 1.20.5
     * ("The Armored Paws Update")
     */
    MINECRAFT_1_20_5(1, 20, 5, "1.20.5+"),

    /**
     * This constant represents Minecraft (Java Edition) Version 1.21
     * ("The Tricky Trials Update")
     */
    MINECRAFT_1_21(1, 21, "1.21.x"),

    /**
     * This constant represents Minecraft (Java Edition) Version 26.1
     * (Tiny Takeover Update)
     */
    MINECRAFT_26_1(26, 1, "26.1.x"),

    /**
     * This constant represents an exceptional state in which we were unable
     * to identify the Minecraft Version we are using
     */
    UNKNOWN("Unknown", true),

    /**
     * This is a very special state that represents the environment being a Unit
     * Test and not an actual running Minecraft Server.
     */
    UNIT_TEST("Unit Test Environment", true);

    private final String name;
    private final boolean virtual;
    private final int majorVersion;
    private final int minorVersion;
    private final int patchVersion;

    /**
     * This constructs a new {@link MinecraftVersion} with the given name.
     * This constructor forces the {@link MinecraftVersion} to be real.
     * It must be a real version of Minecraft.
     *
     * @param majorVersion The major version of minecraft as an {@link Integer}
     * @param name         The display name of this {@link MinecraftVersion}
     */
    MinecraftVersion(int majorVersion, int minorVersion, @Nonnull String name) {
        this.name = name;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = -1;
        this.virtual = false;
    }

    /**
     * This constructs a new {@link MinecraftVersion} with the given name.
     * This constructor forces the {@link MinecraftVersion} to be real.
     * It must be a real version of Minecraft.
     *
     * @param majorVersion The major (minor in semver, major in MC land) version of minecraft as an {@link Integer}
     * @param minor        The minor (patch in semver, minor in MC land) version of minecraft as an {@link Integer}
     * @param name         The display name of this {@link MinecraftVersion}
     */
    MinecraftVersion(int majorVersion, int minor, int patch, @Nonnull String name) {
        this.name = name;
        this.majorVersion = majorVersion;
        this.minorVersion = minor;
        this.patchVersion = patch;
        this.virtual = false;
    }

    /**
     * This constructs a new {@link MinecraftVersion} with the given name.
     * A virtual {@link MinecraftVersion} (unknown or unit test) is not an actual
     * version of Minecraft but rather a state of the {@link Server} software.
     *
     * @param name    The display name of this {@link MinecraftVersion}
     * @param virtual Whether this {@link MinecraftVersion} is virtual
     */
    MinecraftVersion(@Nonnull String name, boolean virtual) {
        this.name = name;
        this.majorVersion = 0;
        this.minorVersion = -1;
        this.patchVersion = -1;
        this.virtual = virtual;
    }

    /**
     * This returns the name of this {@link MinecraftVersion} in a readable format.
     *
     * @return The name of this {@link MinecraftVersion}
     */
    public @Nonnull String getName() {
        return name;
    }

    /**
     * This returns whether this {@link MinecraftVersion} is virtual or not.
     * A virtual {@link MinecraftVersion} does not actually exist but is rather
     * a state of the {@link Server} software used.
     * Virtual {@link MinecraftVersion MinecraftVersions} include "UNKNOWN" and
     * "UNIT TEST".
     *
     * @return Whether this {@link MinecraftVersion} is virtual or not
     */
    public boolean isVirtual() {
        return virtual;
    }

    /**
     * This tests if the given minecraft version number matches with this
     * {@link MinecraftVersion}.
     * <p>
     * You can compare against live server versions by using {@link SlimefunExtended#isAtLeast(int, int)}.
     * It is equivalent to the "major" version
     * <p>
     * Example: {@literal "1.13"} returns {@literal 13}
     *
     * @param minecraftVersion The {@link Integer} version to match
     * @return Whether this {@link MinecraftVersion} matches the specified version id
     */
    @Deprecated(since = "2026.1")
    public boolean isMinecraftVersion(int minecraftVersion) {
        return this.isMinecraftVersion(minecraftVersion, -1);
    }

    /**
     * This tests if the given minecraft version matches with this
     * {@link MinecraftVersion}.
     * <p>
     * You can compare against live server versions by using
     * {@link SlimefunExtended#isAtLeast(int, int)} or {@link SlimefunExtended#isAtLeast(int, int, int)}.
     * <p>
     * Example: {@literal "1.13"} returns {@literal 13}<br />
     * Exampe: {@literal "1.13.2"} returns {@literal 13_2}
     *
     * @param minecraftVersion The {@link Integer} version to match
     * @return Whether this {@link MinecraftVersion} matches the specified version id
     */
    public boolean isMinecraftVersion(int minecraftVersion, int patchVersion) {
        if (isVirtual()) {
            return false;
        }

        if (this.majorVersion != minecraftVersion) {
            return false;
        }
        // the virtual ones are at the last of array, so it will not cause indexOutOfRange
        MinecraftVersion nextVersion = values()[this.ordinal() + 1];
        // checking patchVersion, if next Version is not a virtual version and it is in the same majorVersion as this,
        // then we should ensure patchVersion is lower than nextVersion
        return patchVersion >= this.minorVersion
                && (nextVersion.isVirtual()
                        || nextVersion.majorVersion != this.majorVersion
                        || nextVersion.minorVersion > patchVersion);

        //        if (this.majorVersion == 20) {
        //            return this.minorVersion == -1 ? patchVersion < 5 : patchVersion >= this.minorVersion;
        //        } else {
        //            return this.minorVersion == -1 || patchVersion >= this.minorVersion;
        //        }
    }

    /**
     * This tests if the given minecraft version matches with this
     * {@link MinecraftVersion}.
     * <p>
     * This method accepts full semantic version numbers.
     * <p>
     * Example: {@literal 26.1.1} is equivalent to {@code major = 26, minor = 1, patch = 1}
     *
     * @param major The major version to match
     * @param minor The minor version to match
     * @param patch The patch version to match
     * @return Whether this {@link MinecraftVersion} matches the specified version
     */
    public boolean isMinecraftVersion(int major, int minor, int patch) {
        if (isVirtual()) {
            return false;
        }

        if (this.majorVersion != major || this.minorVersion != minor) {
            return false;
        }

        // the virtual ones are at the last of array, so it will not cause indexOutOfRange
        MinecraftVersion nextVersion = values()[this.ordinal() + 1];
        // checking patch, if next Version is not a virtual version and it is in the same major/minor as this,
        // then we should ensure patch is lower than nextVersion
        return patch >= this.patchVersion
                && (nextVersion.isVirtual()
                        || nextVersion.majorVersion != this.majorVersion
                        || nextVersion.minorVersion != this.minorVersion
                        || nextVersion.patchVersion > patch);
    }

    /**
     * This method checks whether this {@link MinecraftVersion} is newer or equal to
     * the given {@link MinecraftVersion},
     *
     * An unknown version will default to {@literal false}.
     *
     * @param version The {@link MinecraftVersion} to compare
     * @return Whether this {@link MinecraftVersion} is newer or equal to the given {@link MinecraftVersion}
     */
    public boolean isAtLeast(@Nonnull MinecraftVersion version) {
        Validate.notNull(version, "A Minecraft version cannot be null!");

        if (this == UNKNOWN) {
            return false;
        }

        /**
         * Unit-Test only code.
         * Running #isAtLeast(...) should always be meaningful.
         * If the provided version equals the lowest supported version, then
         * this will essentially always return true and result in a tautology.
         * This is most definitely an oversight from us and should be fixed, therefore
         * we will trigger an exception.
         *
         * In order to not disrupt server operations, this exception is only thrown during
         * unit tests since the oversight itself will be harmless.
         */
        if (this == UNIT_TEST && version.ordinal() == 0) {
            throw new IllegalArgumentException("Version " + version + " is the lowest supported version already!");
        }

        return this.ordinal() >= version.ordinal();
    }

    public boolean isAtLeast(int majorVersion, int minorVersion) {
        if (this == UNKNOWN) {
            return false;
        }

        return SlimefunExtended.isAtLeast(majorVersion, minorVersion);
    }

    /**
     * This checks whether this {@link MinecraftVersion} is older than the specified {@link MinecraftVersion}.
     *
     * An unknown version will default to {@literal true}.
     *
     * @param version The {@link MinecraftVersion} to compare
     * @return Whether this {@link MinecraftVersion} is older than the given one
     */
    public boolean isBefore(@Nonnull MinecraftVersion version) {
        return !isAtLeast(version);
        //        Validate.notNull(version, "A Minecraft version cannot be null!");
        //
        //        if (this == UNKNOWN) {
        //            return true;
        //        }
        //
        //        return version.ordinal() > this.ordinal();
    }

    public boolean isBefore(int majorVersion, int minorVersion) {
        return !isAtLeast(majorVersion, minorVersion);
    }
}
