package io.github.thebusybiscuit.slimefun4.implementation.items.weapons;

import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The {@link IcyBow} is a special kind of bow which slows down any
 * {@link LivingEntity} it hits.
 *
 * @author TheBusyBiscuit
 */
public class IcyBow extends SlimefunBow {

    @ParametersAreNonnullByDefault
    public IcyBow(Category category, SlimefunItemStack item, ItemStack[] recipe) {
        super(category, item, recipe);
    }

    @Override
    public BowShootHandler onShoot() {
        return (e, n) -> {
            if (n instanceof Player) {
                Player p = (Player) n;
                if (p.isBlocking() && e.getFinalDamage() == 0) {
                    return;
                }

                if (SlimefunPlugin.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_17)) {
                    p.setFreezeTicks(60);
                }
            }

            n.getWorld().playEffect(n.getLocation(), Effect.STEP_SOUND, Material.ICE);
            n.getWorld().playEffect(n.getEyeLocation(), Effect.STEP_SOUND, Material.ICE);
            n.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 2, 10));
            n.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 2, -10));
        };
    }

}
