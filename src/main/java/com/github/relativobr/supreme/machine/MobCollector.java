package com.github.relativobr.supreme.machine;

import com.github.relativobr.supreme.util.UtilEnergy;
import static com.github.relativobr.supreme.Supreme.getSupremeOptions;

import com.github.relativobr.supreme.compat.SupremeBlockTicker;
import com.github.relativobr.supreme.generic.machine.SimpleItemWithLargeContainerMachine;
import com.github.relativobr.supreme.machine.recipe.MobCollectorMachineRecipe;
import com.github.relativobr.supreme.resource.SupremeComponents;
import com.github.relativobr.supreme.resource.magical.SupremeAttribute;
import com.github.relativobr.supreme.resource.magical.SupremeCetrus;
import com.github.relativobr.supreme.util.SupremeInventoryUtils;
import com.github.relativobr.supreme.util.SupremeItemStack;
import com.github.relativobr.supreme.util.SupremeOptions;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineTier;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.LoreBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class MobCollector extends SimpleItemWithLargeContainerMachine {

  public static final SlimefunItemStack MOB_COLLECTOR_MACHINE = new SupremeItemStack("SUPREME_MOB_COLLECTOR_MACHINE_I",
      Material.RESPAWN_ANCHOR, "&bMob Collector", "", "&fThis machine allows you to collect ",
      "&fitems from nearby mobs. (4 block)", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE),
      LoreBuilder.speed(1), LoreBuilder.powerBuffer(1000), UtilEnergy.energyPowerPerTick(20), "", "&3Supreme Machine");
  public static final ItemStack[] RECIPE_MOB_COLLECTOR_MACHINE = new ItemStack[]{SupremeComponents.RUSTLESS_MACHINE,
      new ItemStack(Material.RESPAWN_ANCHOR), SupremeComponents.RUSTLESS_MACHINE, SupremeComponents.INDUCTIVE_MACHINE,
      SupremeComponents.SYNTHETIC_RUBY, SupremeComponents.INDUCTIVE_MACHINE, SupremeComponents.ADAMANTIUM_PLATE,
      SlimefunItems.PROGRAMMABLE_ANDROID_3_BUTCHER, SupremeComponents.ADAMANTIUM_PLATE};

  public static final SlimefunItemStack MOB_COLLECTOR_MACHINE_II = new SupremeItemStack(
      "SUPREME_MOB_COLLECTOR_MACHINE_II", Material.RESPAWN_ANCHOR, "&bMob Collector II", "",
      "&fThis machine allows you to collect", "&f items from nearby mobs. (8 block)", "",
      LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(5),
      LoreBuilder.powerBuffer(5000), UtilEnergy.energyPowerPerTick(100), "", "&3Supreme Machine");
  public static final ItemStack[] RECIPE_MOB_COLLECTOR_MACHINE_II = new ItemStack[]{
      SupremeComponents.CONVEYANCE_MACHINE, SupremeCetrus.CETRUS_LUMIUM, SupremeComponents.CONVEYANCE_MACHINE,
      SupremeComponents.INDUCTOR_MACHINE, MobCollector.MOB_COLLECTOR_MACHINE, SupremeComponents.INDUCTOR_MACHINE,
      SupremeComponents.THORNERITE, SupremeCetrus.CETRUS_IGNIS, SupremeComponents.THORNERITE};

  public static final SlimefunItemStack MOB_COLLECTOR_MACHINE_III = new SupremeItemStack(
      "SUPREME_MOB_COLLECTOR_MACHINE_III", Material.RESPAWN_ANCHOR, "&bMob Collector III", "",
      "&fThis machine allows you to collect", "&f items from nearby mobs. (16 block)", "",
      LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(15),
      LoreBuilder.powerBuffer(15000), UtilEnergy.energyPowerPerTick(300), "", "&3Supreme Machine");
  public static final ItemStack[] RECIPE_MOB_COLLECTOR_MACHINE_III = new ItemStack[]{SupremeComponents.THORNERITE,
      SupremeAttribute.getBomb(), SupremeComponents.THORNERITE, SupremeComponents.SUPREME,
      MobCollector.MOB_COLLECTOR_MACHINE_II, SupremeComponents.SUPREME, SupremeComponents.CRYSTALLIZER_MACHINE,
      SupremeCetrus.CETRUS_LUMIUM, SupremeComponents.CRYSTALLIZER_MACHINE};

  private final Map<Block, MachineRecipe> processing = new HashMap<>();
  private final Map<Block, Integer> progress = new HashMap<>();
  private final Map<Block, Integer> selectedInputSlots = new HashMap<>();
  private final Set<MobCollectorMachineRecipe> mobCollectorMachineRecipes = new HashSet();
  private int mobRange = 4;

  @ParametersAreNonnullByDefault
  public MobCollector(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
    super(category, item, recipeType, recipe);
  }


  @Override
  protected void registerDefaultRecipes() {
    SupremeOptions supremeOptions = getSupremeOptions();
    boolean customBc = supremeOptions.isCustomBc();
    this.recipes.clear();
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.GLASS_BOTTLE, this.getSpeed()),
            new ItemStack(Material.HONEY_BOTTLE, this.getSpeed()),
            (n) -> n.getType() == EntityType.BEE));
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.GLASS_BOTTLE, this.getSpeed()),
            new ItemStack(Material.INK_SAC, this.getSpeed()),
            (n) -> n.getType() == EntityType.SQUID));
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.GLASS_BOTTLE, this.getSpeed()),
            new ItemStack(Material.GLOW_INK_SAC, this.getSpeed()),
            (n) -> n.getType() == EntityType.GLOW_SQUID));
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.GLASS_BOTTLE, this.getSpeed()),
            new SlimefunItemStack(SlimefunItems.FILLED_FLASK_OF_KNOWLEDGE, this.getSpeed()),
            (n) -> n.getType() == EntityType.WITHER));
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.GLASS_BOTTLE, this.getSpeed()),
            new ItemStack(Material.DRAGON_BREATH, this.getSpeed()),
            (n) -> n.getType() == EntityType.ENDER_DRAGON));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.HONEYCOMB, this.getSpeed()), (n) -> n.getType() == EntityType.BEE));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.WHITE_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.WHITE));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.ORANGE_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.ORANGE));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.MAGENTA_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.MAGENTA));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.LIGHT_BLUE_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.LIGHT_BLUE));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.YELLOW_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.YELLOW));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.LIME_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.LIME));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.PINK_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.PINK));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.GRAY_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.GRAY));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.LIGHT_GRAY_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.LIGHT_GRAY));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.CYAN_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.CYAN));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.PURPLE_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.PURPLE));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.BLUE_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.BLUE));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.BROWN_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.BROWN));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.GREEN_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.GREEN));
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
            new ItemStack(Material.RED_WOOL, this.getSpeed()),
            (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.RED));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.BLACK_WOOL, this.getSpeed()),
        (n) -> n.getType() == EntityType.SHEEP && ((Sheep) n).getColor() == DyeColor.BLACK));
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
            new ItemStack(Material.LEATHER, this.getSpeed()),
            (n) -> n.getType() == EntityType.COW));
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
            new ItemStack(Material.FEATHER, this.getSpeed()),
            (n) -> n.getType() == EntityType.CHICKEN));
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
            new ItemStack(Material.SPONGE, this.getSpeed()),
            (n) -> n.getType() == EntityType.GUARDIAN));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new ItemStack(Material.SPIDER_EYE, this.getSpeed()),
        (n) -> n.getType() == EntityType.SPIDER));
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
            new ItemStack(Material.COAL, this.getSpeed()),
            (n) -> n.getType() == EntityType.WITHER_SKELETON));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
        new SlimefunItemStack(SlimefunItems.COMPRESSED_CARBON, this.getSpeed()),
        (n) -> n.getType() == EntityType.WITHER));
    if (!customBc) {
      this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.SHEARS),
          new SlimefunItemStack(SlimefunItems.BASIC_CIRCUIT_BOARD, this.getSpeed()),
          (n) -> n.getType() == EntityType.IRON_GOLEM));
    }
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.PHANTOM_MEMBRANE, this.getSpeed()),
        (n) -> n.getType() == EntityType.PHANTOM));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.ROTTEN_FLESH, this.getSpeed()),
        (n) -> n.getType() == EntityType.ZOMBIE));
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
            new ItemStack(Material.BONE, this.getSpeed()),
            (n) -> n.getType() == EntityType.SKELETON));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.GUNPOWDER, this.getSpeed()),
        (n) -> n.getType() == EntityType.CREEPER));
    if (!customBc) {
      this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
          new ItemStack(Material.SLIME_BALL, this.getSpeed()),
          (n) -> n.getType() == EntityType.SLIME));
    }
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.STRING, this.getSpeed()), (n) -> n.getType() == EntityType.SPIDER));
    if (!customBc) {
      this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
          new ItemStack(Material.WITHER_SKELETON_SKULL, this.getSpeed()),
          (n) -> n.getType() == EntityType.WITHER_SKELETON));
    }
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.ENDER_PEARL, this.getSpeed()),
        (n) -> n.getType() == EntityType.ENDERMAN));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.BLAZE_ROD, this.getSpeed()),
        (n) -> n.getType() == EntityType.BLAZE));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.MAGMA_CREAM, this.getSpeed()),
        (n) -> n.getType() == EntityType.MAGMA_CUBE));
    if (!customBc) {
      this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
          new ItemStack(Material.NETHER_STAR, this.getSpeed()),
          (n) -> n.getType() == EntityType.WITHER));
      this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
          new ItemStack(Material.GHAST_TEAR, this.getSpeed()),
          (n) -> n.getType() == EntityType.GHAST));
      this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
          new ItemStack(Material.TOTEM_OF_UNDYING, this.getSpeed()),
          (n) -> n.getType() == EntityType.RAVAGER));
    }
    this.addProduce(
        new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
            new ItemStack(Material.BEEF, this.getSpeed()),
            (n) -> n.getType() == EntityType.COW));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.PORKCHOP, this.getSpeed()), (n) -> n.getType() == EntityType.PIG));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.CHICKEN, this.getSpeed()),
        (n) -> n.getType() == EntityType.CHICKEN));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.MUTTON, this.getSpeed()), (n) -> n.getType() == EntityType.SHEEP));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.SNOWBALL, this.getSpeed()),
        (n) -> {
          String entityType = n.getType().name();
          return "SNOW_GOLEM".equals(entityType) || "SNOWMAN".equals(entityType);
        }));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.IRON_INGOT, this.getSpeed()),
        (n) -> n.getType() == EntityType.IRON_GOLEM));
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.NAUTILUS_SHELL, this.getSpeed()),
        (n) -> n.getType() == EntityType.DROWNED));
    if (!customBc) {
      this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
          new ItemStack(Material.PRISMARINE_SHARD, this.getSpeed()),
          (n) -> n.getType() == EntityType.GUARDIAN));
      this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
          new ItemStack(Material.PRISMARINE_CRYSTALS, this.getSpeed()),
          (n) -> n.getType() == EntityType.ELDER_GUARDIAN));
    }
    this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.GLASS_BOTTLE, this.getSpeed()),
        (n) -> n.getType() == EntityType.WITCH));
    if (!customBc) {
      this.addProduce(new MobCollectorMachineRecipe(new ItemStack(Material.GOLD_INGOT),
              new SlimefunItemStack(SlimefunItems.STRANGE_NETHER_GOO, this.getSpeed()),
              (n) -> n.getType() == EntityType.PIGLIN));
    }
  }


  public void addProduce(@Nonnull MobCollectorMachineRecipe produce) {
    Validate.notNull(produce, "A produce cannot be null");
    this.mobCollectorMachineRecipes.add(produce);
  }

  @Override
  public void preRegister() {
    addItemHandler(new SupremeBlockTicker(true, this::tick));
  }

  @Nonnull
  @Override
  public List<ItemStack> getDisplayRecipes() {
    return MobCollectorMachineRecipe.getAllRecipe();
  }

  @Nonnull
  @Override
  public String getRecipeSectionLabel(@Nonnull Player p) {
    return "&7Collects:";
  }


  @Override
  protected MachineRecipe findNextRecipe(@Nonnull BlockMenu inv) {
    Block block = inv.getBlock();
    selectedInputSlots.remove(block);

    for (int slot : getInputSlots()) {
      ItemStack itemInSlot = inv.getItemInSlot(slot);
      if (itemInSlot == null || itemInSlot.getType().isAir()) {
        continue;
      }

      for (MobCollectorMachineRecipe produce : mobCollectorMachineRecipes) {
        ItemStack itemInInput = produce.getInput()[0];
        if (itemInInput == null || itemInSlot.getType() != itemInInput.getType()) {
          continue;
        }
        if (itemInSlot.getType() == Material.GLASS_BOTTLE
            && itemInSlot.getAmount() < Math.max(1, itemInInput.getAmount())) {
          continue;
        }
        if (!SupremeInventoryUtils.canFit(inv, getOutputSlots(), produce.getOutput())) {
          continue;
        }
        if (isAnimalNearby(block, produce::test)) {
          // Reserve only the slot reference here. The tool/bottle cost is committed atomically
          // with the output after processing completes, so no input is lost while power is absent.
          selectedInputSlots.put(block, slot);
          return produce;
        }
      }
    }

    return null;
  }

  @ParametersAreNonnullByDefault
  private boolean isAnimalNearby(Block b, Predicate<LivingEntity> predicate) {
    return !b.getWorld().getNearbyEntities(b.getLocation(), mobRange, mobRange, mobRange, (n) -> {
      return this.isValidAnimal(n, predicate);
    }).isEmpty();
  }

  @ParametersAreNonnullByDefault
  private boolean isValidAnimal(Entity n, Predicate<LivingEntity> predicate) {
    return n instanceof LivingEntity && predicate.test((LivingEntity) n);
  }

  public final MobCollector setMobRange(int value) {
    this.mobRange = value;
    return this;
  }

  @Override
  protected void tick(Block b) {
    BlockMenu inv = BlockStorage.getInventory(b);
    if (inv == null) {
      return;
    }

    MachineRecipe active = processing.get(b);
    if (active == null) {
      MachineRecipe next = findNextRecipe(inv);
      if (next != null) {
        processing.put(b, next);
        progress.put(b, next.getTicks());
      } else {
        updateStatusReset(inv);
      }
      return;
    }

    ItemStack[] recipeOutput = active.getOutput();
    if (notHasSpaceOutput(inv, recipeOutput)) {
      updateStatusOutputFull(inv);
      return;
    }

    int timeLeft = progress.getOrDefault(b, active.getTicks());
    if (timeLeft <= 0) {
      if (!commitCollectorInput(b, inv, active)) {
        clearCollectorState(b);
        updateStatusInvalidInput(inv);
        return;
      }
      SupremeInventoryUtils.pushAll(inv, getOutputSlots(), recipeOutput);
      clearCollectorState(b);
      updateStatusReset(inv);
      return;
    }

    if (getCharge(b.getLocation()) < getEnergyConsumption()) {
      updateStatusConnectEnergy(inv, recipeOutput.length > 0 ? recipeOutput[0] : null);
      return;
    }

    if (takeCharge(b.getLocation())) {
      ChestMenuUtils.updateProgressbar(inv, getStatusSlot(), timeLeft, active.getTicks(),
          getProgressBar());
      progress.put(b, Math.max(timeLeft - getSpeed(), 0));
    }
  }

  private boolean commitCollectorInput(Block block, BlockMenu menu, MachineRecipe recipe) {
    Integer slot = selectedInputSlots.get(block);
    ItemStack[] inputs = recipe.getInput();
    if (slot == null || inputs == null || inputs.length == 0 || inputs[0] == null) {
      return false;
    }

    ItemStack required = inputs[0];
    ItemStack current = menu.getItemInSlot(slot);
    if (current == null || current.getType() != required.getType()) {
      return false;
    }

    if (current.getType() == Material.GLASS_BOTTLE) {
      int amount = Math.max(1, required.getAmount());
      if (current.getAmount() < amount) {
        return false;
      }
      menu.consumeItem(slot, amount);
      return true;
    }

    ItemMeta itemMeta = current.getItemMeta();
    if (itemMeta instanceof Damageable durability && !itemMeta.isUnbreakable()) {
      int currentDamage = durability.getDamage();
      if (currentDamage + 2 >= current.getType().getMaxDurability()) {
        menu.consumeItem(slot);
      } else {
        durability.setDamage(currentDamage + 2);
        current.setItemMeta(itemMeta);
        menu.replaceExistingItem(slot, current);
      }
    }
    return true;
  }

  private void clearCollectorState(Block block) {
    processing.remove(block);
    progress.remove(block);
    selectedInputSlots.remove(block);
  }

  @Override
  protected void onMachineBreak(Block block) {
    clearCollectorState(block);
  }

  @Nonnull
  @Override
  public String getMachineIdentifier() {
    return "MOB_COLLECTOR";
  }

  @Override
  public ItemStack getProgressBar() {
    return new ItemStack(Material.IRON_SWORD);
  }

  public MachineRecipe getProcessing(Block b) {
    return processing.get(b);
  }

  public boolean isProcessing(Block b) {
    return getProcessing(b) != null;
  }

}
