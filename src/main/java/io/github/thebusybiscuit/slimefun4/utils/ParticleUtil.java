package io.github.thebusybiscuit.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * @author Final_ROOT
 * @author balugaq
 */
@SuppressWarnings("DuplicatedCode")
public class ParticleUtil {
    private static final double[] BLOCK_CUBE_OFFSET_X = new double[] {0, 1, 0, 0, 1, 1, 0, 1};
    private static final double[] BLOCK_CUBE_OFFSET_Y = new double[] {0, 0, 1, 0, 1, 0, 1, 1};
    private static final double[] BLOCK_CUBE_OFFSET_Z = new double[] {0, 0, 0, 1, 0, 1, 1, 1};

    public static void drawLineByTotalAmount(
            @NotNull Particle particle, int totalAmount, @NotNull Location @NotNull ... locations) {
        for (int i = 0; i < locations.length; i++) {
            if ((i + 1) < locations.length) {
                Location location1 = locations[i];
                Location location2 = locations[i + 1];

                if (totalAmount < 1 || location1.getWorld() == null || location1.getWorld() != location2.getWorld()) {
                    return;
                }
                World world = location1.getWorld();
                double[] x = disperse(totalAmount, location1.getX(), location2.getX());
                double[] y = disperse(totalAmount, location1.getY(), location2.getY());
                double[] z = disperse(totalAmount, location1.getZ(), location2.getZ());
                for (int j = 0; j < totalAmount; j++) {
                    world.spawnParticle(particle, x[j], y[j], z[j], 1, 0, 0, 0, 0);
                }
            }
        }
    }

    public static void drawCubeByBlock(
            @NotNull final Plugin plugin,
            @NotNull final Particle particle,
            final long interval,
            @NotNull final Block @NotNull ... blocks) {
        int time = 0;
        for (final Block block : blocks) {
            final Location location = block.getLocation();
            final World world = location.getWorld();
            if (world == null) {
                continue;
            }
            final int x = location.getBlockX();
            final int y = location.getBlockY();
            final int z = location.getBlockZ();
            if (time < 50) {
                for (int i = 0; i < BLOCK_CUBE_OFFSET_X.length; i++) {
                    world.spawnParticle(
                            particle,
                            x + BLOCK_CUBE_OFFSET_X[i],
                            y + BLOCK_CUBE_OFFSET_Y[i],
                            z + BLOCK_CUBE_OFFSET_Z[i],
                            1,
                            0,
                            0,
                            0,
                            0);
                }
            } else {
                plugin.getServer()
                        .getScheduler()
                        .runTaskLaterAsynchronously(
                                plugin,
                                () -> {
                                    for (int i = 0; i < BLOCK_CUBE_OFFSET_X.length; i++) {
                                        world.spawnParticle(
                                                particle,
                                                x + BLOCK_CUBE_OFFSET_X[i],
                                                y + BLOCK_CUBE_OFFSET_Y[i],
                                                z + BLOCK_CUBE_OFFSET_Z[i],
                                                1,
                                                0,
                                                0,
                                                0,
                                                0);
                                    }
                                },
                                time / 50);
            }
            time += (int) interval;
        }
    }

    public static void drawLineFrom(final @NotNull Location location1, final @NotNull Location location2) {
        drawLineByTotalAmount(Particle.WAX_OFF, (int) location1.distance(location2) * 4, location1, location2);
    }

    public static void highlightBlock(final @NotNull Location location) {
        highlightBlock(location.getBlock());
    }

    public static void highlightBlock(final Block block) {
        drawCubeByBlock(Slimefun.instance(), Particle.WAX_ON, 1, block);
    }

    public static void highlightBlock(@NotNull Player player, @NotNull Location location, int shrinkTimes) {
        for (int i = 0; i < shrinkTimes; i++) {
            Bukkit.getScheduler()
                    .runTaskLater(
                            Slimefun.instance(),
                            () -> {
                                drawLineFrom(player.getEyeLocation().clone().add(0D, -0.5D, 0D), location);
                                highlightBlock(location);
                            },
                            20L * i);
        }
    }

    public static double[] disperse(int size, Number @NotNull ... value) {
        if (size == 1 && value.length > 0) {
            return new double[] {value[0].doubleValue()};
        } else if (size == 0 || value.length == 0) {
            return new double[0];
        }
        double[] result = new double[size--];
        for (int i = 0; i <= size; i++) {
            double p = ((double) i) / size * (value.length - 1);
            double value1 = value[(int) Math.floor(p)].doubleValue() * (1 - p + Math.floor(p));
            double value2 = value[(int) Math.ceil(p)].doubleValue() * (p - Math.floor(p));
            result[i] = value1 + value2;
        }
        return result;
    }
}
