package com.github.relativobr.supreme.machine.tech;

import com.github.relativobr.supreme.util.UtilEnergy;
import com.github.relativobr.supreme.compat.SupremeBlockTicker;
import com.github.relativobr.supreme.generic.machine.SimpleItemWithLargeContainerMachine;
import com.github.relativobr.supreme.Supreme;
import com.github.relativobr.supreme.tools.MobCollectorTools;
import com.github.relativobr.supreme.machine.recipe.MobTechCollectorMachineRecipe;
import com.github.relativobr.supreme.resource.SupremeComponents;
import com.github.relativobr.supreme.resource.magical.SupremeCetrus;
import com.github.relativobr.supreme.resource.mobtech.BeeTech;
import com.github.relativobr.supreme.resource.mobtech.IronGolemTech;
import com.github.relativobr.supreme.resource.mobtech.ZombieTech;
import com.github.relativobr.supreme.util.ItemUtil;
import com.github.relativobr.supreme.util.SupremeItemStack;
import com.github.relativobr.supreme.util.SupremeOptions;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineTier;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.utils.LoreBuilder;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

public class MobTechCollector extends SimpleItemWithLargeContainerMachine {

  public static final SlimefunItemStack MOB_TECH_COLLECTOR_MACHINE_I = new SupremeItemStack(
      "MOB_TECH_COLLECTOR_MACHINE", Material.NETHER_GOLD_ORE, "&bMobTech Collector I", "",
      "&fThis machine allows you to collect ", "&fMobTech head nearby mobs. (3 block)", "",
      LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), LoreBuilder.speed(1),
      LoreBuilder.powerBuffer(1000), UtilEnergy.energyPowerPerTick(20), "", "&3Supreme Machine");
  public static final ItemStack[] RECIPE_MOB_TECH_COLLECTOR_MACHINE_I = new ItemStack[]{
      SupremeComponents.RUSTLESS_MACHINE, MobCollectorTools.MOB_COLLECTOR_I, SupremeComponents.RUSTLESS_MACHINE,
      SupremeComponents.INDUCTIVE_MACHINE, MobCollectorTools.MOB_COLLECTOR_I, SupremeComponents.INDUCTIVE_MACHINE,
      SupremeComponents.AURUM_PLATE, SlimefunItems.PROGRAMMABLE_ANDROID_3_BUTCHER, SupremeComponents.AURUM_PLATE};

  public static final SlimefunItemStack MOB_TECH_COLLECTOR_MACHINE_II = new SupremeItemStack(
      "SUPREME_MOB_TECH_COLLECTOR_MACHINE_II", Material.NETHER_GOLD_ORE, "&bMobTech Collector II", "",
      "&fThis machine allows you to collect ", "&fMobTech head nearby mobs. (6 block)", "",
      LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(1),
      LoreBuilder.powerBuffer(1000), UtilEnergy.energyPowerPerTick(20), "", "&3Supreme Machine");
  public static final ItemStack[] RECIPE_MOB_TECH_COLLECTOR_MACHINE_II = new ItemStack[]{
      SupremeComponents.CONVEYANCE_MACHINE, SupremeCetrus.CETRUS_LUMIUM, SupremeComponents.CONVEYANCE_MACHINE,
      SupremeComponents.INDUCTOR_MACHINE, MobTechCollector.MOB_TECH_COLLECTOR_MACHINE_I,
      SupremeComponents.INDUCTOR_MACHINE, SupremeComponents.THORNERITE, SupremeCetrus.CETRUS_IGNIS,
      SupremeComponents.THORNERITE};

  public static final SlimefunItemStack MOB_TECH_COLLECTOR_MACHINE_III = new SupremeItemStack(
      "SUPREME_MOB_TECH_COLLECTOR_MACHINE_III", Material.NETHER_GOLD_ORE, "&bMobTech Collector III", "",
      "&fThis machine allows you to collect ", "&fMobTech head nearby mobs. (9 block)", "",
      LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(1),
      LoreBuilder.powerBuffer(1000), UtilEnergy.energyPowerPerTick(20), "", "&3Supreme Machine");
  public static final ItemStack[] RECIPE_MOB_TECH_COLLECTOR_MACHINE_III = new ItemStack[]{SupremeComponents.THORNERITE,
      SupremeCetrus.CETRUS_LUX, SupremeComponents.THORNERITE, SupremeComponents.SUPREME,
      MobTechCollector.MOB_TECH_COLLECTOR_MACHINE_II, SupremeComponents.SUPREME, SupremeComponents.CRYSTALLIZER_MACHINE,
      SupremeCetrus.CETRUS_LUMIUM, SupremeComponents.CRYSTALLIZER_MACHINE};


  private final Set<MobTechCollectorMachineRecipe> mobTechCollectorMachineRecipes = new HashSet<>();
  private final Map<Block, LivingEntity> pendingEntities = new HashMap<>();
  private int mobRange = 4;

  @ParametersAreNonnullByDefault
  public MobTechCollector(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
    super(category, item, recipeType, recipe);
  }


  @Override
  protected void registerDefaultRecipes() {
    this.recipes.clear();
    SupremeOptions supremeOptions = Supreme.getSupremeOptions();
    if (supremeOptions.isMobTechEnableBee()) {
      this.addProduce(new MobTechCollectorMachineRecipe(SupremeComponents.EMPTY_MOBTECH,
          ItemUtil.buildItemFromMobTechDTO(BeeTech.SIMPLE_BEE, 0), (n) -> n instanceof Bee));
    }
    if (supremeOptions.isMobTechEnableIronGolem()) {
      this.addProduce(new MobTechCollectorMachineRecipe(SupremeComponents.EMPTY_MOBTECH,
          ItemUtil.buildItemFromMobTechDTO(IronGolemTech.SIMPLE_GOLEM, 0), (n) -> n instanceof IronGolem));
    }
    if (supremeOptions.isMobTechEnableZombie()) {
      this.addProduce(new MobTechCollectorMachineRecipe(SupremeComponents.EMPTY_MOBTECH,
          ItemUtil.buildItemFromMobTechDTO(ZombieTech.SIMPLE_ZOMBIE, 0), (n) -> n instanceof Zombie));
    }
  }


  public void addProduce(@Nonnull MobTechCollectorMachineRecipe produce) {
    Validate.notNull(produce, "A produce cannot be null");
    this.mobTechCollectorMachineRecipes.add(produce);
  }

  @Override
  public void preRegister() {
    addItemHandler(new SupremeBlockTicker(true, this::tick));
  }

  @Nonnull
  @Override
  public List<ItemStack> getDisplayRecipes() {
    return MobTechCollectorMachineRecipe.getAllRecipe();
  }

  @Nonnull
  @Override
  public String getRecipeSectionLabel(@Nonnull Player p) {
    return "&7Collects:";
  }


  @Override
  protected MachineRecipe findNextRecipe(@Nonnull BlockMenu inv) {
    for (int slot : getInputSlots()) {
      ItemStack itemInSlot = inv.getItemInSlot(slot);
      if (itemInSlot == null || !SlimefunUtils.isItemSimilar(itemInSlot,
          SupremeComponents.EMPTY_MOBTECH, false, false)) {
        continue;
      }

      for (MobTechCollectorMachineRecipe produce : mobTechCollectorMachineRecipes) {
        if (!InvUtils.fits(inv.toInventory(), produce.getOutput()[0], getOutputSlots())) {
          continue;
        }

        LivingEntity entity = findAnimalNearby(inv.getBlock(), produce::test);
        if (entity != null) {
          // Do not consume here. GenericMachine will reserve the recipe input exactly once.
          pendingEntities.put(inv.getBlock(), entity);
          return produce;
        }
      }
    }
    return null;
  }

  @ParametersAreNonnullByDefault
  private LivingEntity findAnimalNearby(Block block, Predicate<LivingEntity> predicate) {
    for (Entity entity : block.getWorld().getNearbyEntities(
        block.getLocation(), mobRange, mobRange, mobRange)) {
      if (entity instanceof LivingEntity living && predicate.test(living)) {
        return living;
      }
    }
    return null;
  }

  @Override
  protected boolean canStartProcess(Block block, BlockMenu menu, MachineRecipe recipe) {
    LivingEntity entity = pendingEntities.get(block);
    if (entity == null || !entity.isValid() || entity.isDead()
        || entity.getWorld() != block.getWorld()) {
      pendingEntities.remove(block);
      return false;
    }

    double dx = Math.abs(entity.getLocation().getX() - block.getX());
    double dy = Math.abs(entity.getLocation().getY() - block.getY());
    double dz = Math.abs(entity.getLocation().getZ() - block.getZ());
    if (dx > mobRange || dy > mobRange || dz > mobRange) {
      pendingEntities.remove(block);
      return false;
    }

    boolean valid = recipe instanceof MobTechCollectorMachineRecipe collectorRecipe
        && collectorRecipe.test(entity);
    if (!valid) {
      pendingEntities.remove(block);
    }
    return valid;
  }

  @Override
  protected void onProcessStarted(Block block, BlockMenu menu, MachineRecipe recipe) {
    LivingEntity entity = pendingEntities.remove(block);
    if (entity != null && entity.isValid() && !entity.isDead()) {
      entity.remove();
    }
  }

  @Override
  protected void onMachineBreak(Block block) {
    pendingEntities.remove(block);
  }

  @Nonnull
  @Override
  public String getMachineIdentifier() {
    return "MOB_TECH_COLLECTOR";
  }

  @Override
  public ItemStack getProgressBar() {
    return new ItemStack(Material.IRON_SWORD);
  }

  public final MobTechCollector setMobRange(int value) {
    this.mobRange = value;
    return this;
  }

}
