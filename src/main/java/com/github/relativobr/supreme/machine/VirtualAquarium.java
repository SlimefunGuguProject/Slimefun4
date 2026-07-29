package com.github.relativobr.supreme.machine;

import com.github.relativobr.supreme.util.UtilEnergy;
import com.github.relativobr.supreme.compat.SupremeBlockTicker;
import com.github.relativobr.supreme.generic.machine.SimpleItemWithLargeContainerMachine;
import com.github.relativobr.supreme.machine.recipe.VirtualAquariumMachineRecipe;
import com.github.relativobr.supreme.resource.SupremeComponents;
import com.github.relativobr.supreme.resource.magical.SupremeAttribute;
import com.github.relativobr.supreme.resource.magical.SupremeCetrus;
import com.github.relativobr.supreme.util.SupremeInventoryUtils;
import com.github.relativobr.supreme.util.SupremeItemStack;
import com.github.relativobr.supreme.util.UtilMachine;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineTier;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.LoreBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class VirtualAquarium extends SimpleItemWithLargeContainerMachine {

  public static final SlimefunItemStack VIRTUAL_AQUARIUM_MACHINE = new SupremeItemStack("SUPREME_VIRTUAL_AQUARIUM_I",
      Material.DARK_PRISMARINE, "&bVirtual Aquarium", "", "&fThis machine allows you to collect ",
      "&f items that are collected at sea.", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE),
      LoreBuilder.speed(1), LoreBuilder.powerBuffer(1000), UtilEnergy.energyPowerPerTick(20), "", "&3Supreme Machine");
  public static final ItemStack[] RECIPE_VIRTUAL_AQUARIUM_MACHINE = new ItemStack[]{SupremeComponents.SYNTHETIC_RUBY,
      new ItemStack(Material.FISHING_ROD), SupremeComponents.SYNTHETIC_RUBY, SupremeComponents.INDUCTIVE_MACHINE,
      SupremeComponents.PETRIFIER_MACHINE, SupremeComponents.INDUCTIVE_MACHINE, SupremeComponents.ADAMANTIUM_PLATE,
      SlimefunItems.PROGRAMMABLE_ANDROID_2_FISHERMAN, SupremeComponents.ADAMANTIUM_PLATE};

  public static final SlimefunItemStack VIRTUAL_AQUARIUM_MACHINE_II = new SupremeItemStack(
      "SUPREME_VIRTUAL_AQUARIUM_II", Material.DARK_PRISMARINE, "&bVirtual Aquarium II", "",
      "&fThis machine allows you to collect", "&f items that are collected at sea.", "",
      LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(5),
      LoreBuilder.powerBuffer(5000), UtilEnergy.energyPowerPerTick(100), "", "&3Supreme Machine");
  public static final ItemStack[] RECIPE_VIRTUAL_AQUARIUM_MACHINE_II = new ItemStack[]{
      SupremeComponents.CONVEYANCE_MACHINE, SupremeCetrus.CETRUS_LUMIUM, SupremeComponents.CONVEYANCE_MACHINE,
      SupremeComponents.INDUCTOR_MACHINE, VirtualAquarium.VIRTUAL_AQUARIUM_MACHINE, SupremeComponents.INDUCTOR_MACHINE,
      SupremeComponents.THORNERITE, SupremeCetrus.CETRUS_IGNIS, SupremeComponents.THORNERITE};

  public static final SlimefunItemStack VIRTUAL_AQUARIUM_MACHINE_III = new SupremeItemStack(
      "SUPREME_VIRTUAL_AQUARIUM_III", Material.DARK_PRISMARINE, "&bVirtual Aquarium III", "",
      "&fThis machine allows you to collect", "&f items that are collected at sea.", "",
      LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(15),
      LoreBuilder.powerBuffer(15000), UtilEnergy.energyPowerPerTick(300), "", "&3Supreme Machine");
  public static final ItemStack[] RECIPE_VIRTUAL_AQUARIUM_MACHINE_III = new ItemStack[]{SupremeComponents.THORNERITE,
      SupremeAttribute.getMagic(), SupremeComponents.THORNERITE, SupremeComponents.SUPREME,
      VirtualAquarium.VIRTUAL_AQUARIUM_MACHINE_II, SupremeComponents.SUPREME, SupremeComponents.CRYSTALLIZER_MACHINE,
      SupremeCetrus.CETRUS_LUMIUM, SupremeComponents.CRYSTALLIZER_MACHINE};

  private final Map<Block, MachineRecipe> processing = new HashMap<>();
  private final Map<Block, Integer> progress = new HashMap<>();
  private final Map<Block, ItemStack> selectedOutput = new HashMap<>();
  private final Set<VirtualAquariumMachineRecipe> virtualAquariumMachineRecipe = new HashSet();

  @ParametersAreNonnullByDefault
  public VirtualAquarium(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
    super(category, item, recipeType, recipe);
  }

  @Override
  protected void registerDefaultRecipes() {
    this.recipes.clear();
    this.addProduce(new VirtualAquariumMachineRecipe(new ItemStack(Material.FISHING_ROD),
        new ItemStack[]{new ItemStack(Material.SPONGE, 2), new ItemStack(Material.SEA_LANTERN, 2),
            new ItemStack(Material.COD, 20), new ItemStack(Material.SALMON, 20),
            new ItemStack(Material.TROPICAL_FISH, 20), new ItemStack(Material.INK_SAC, 20),
            new ItemStack(Material.NAUTILUS_SHELL, 6), new ItemStack(Material.STICK, 5),
            new ItemStack(Material.STRING, 5)}));
    this.addProduce(new VirtualAquariumMachineRecipe(new ItemStack(Material.TRIDENT),
        new ItemStack[]{new ItemStack(Material.SPONGE, 10), new ItemStack(Material.SEA_LANTERN, 10),
            new ItemStack(Material.COD, 5), new ItemStack(Material.SALMON, 5), new ItemStack(Material.TROPICAL_FISH, 5),
            new ItemStack(Material.INK_SAC, 5), new ItemStack(Material.NAUTILUS_SHELL, 20),
            new ItemStack(Material.STICK, 20), new ItemStack(Material.STRING, 20)}));
    this.addProduce(new VirtualAquariumMachineRecipe(new ItemStack(Material.GOLDEN_HOE),
        new ItemStack[]{new ItemStack(Material.SPONGE, 1), new ItemStack(Material.SEA_LANTERN, 1),
            new ItemStack(Material.COD, 3), new ItemStack(Material.SALMON, 3), new ItemStack(Material.TROPICAL_FISH, 2),
            new ItemStack(Material.INK_SAC, 2), new ItemStack(Material.NAUTILUS_SHELL, 5),
            new ItemStack(Material.STICK, 50), new ItemStack(Material.STRING, 33)}));

  }


  public void addProduce(@Nonnull VirtualAquariumMachineRecipe produce) {
    Validate.notNull(produce, "A produce cannot be null");
    this.virtualAquariumMachineRecipe.add(produce);
  }

  @Override
  public void preRegister() {
    addItemHandler(new SupremeBlockTicker(true, this::tick));
  }

  @Nonnull
  @Override
  public List<ItemStack> getDisplayRecipes() {
    return VirtualAquariumMachineRecipe.getAllRecipe();
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
      if (itemInSlot == null || itemInSlot.getType().isAir()) {
        continue;
      }

      for (VirtualAquariumMachineRecipe produce : virtualAquariumMachineRecipe) {
        ItemStack itemInInput = produce.getInput()[0];
        if (itemInInput == null || itemInSlot.getType() != itemInInput.getType()) {
          continue;
        }

        ItemStack material = UtilMachine.getMaterial(produce.getOutput(), UtilMachine.getRandomInt());
        if (material == null) {
          continue;
        }
        ItemStack chosen = material.clone();
        chosen.setAmount(1);
        if (!SupremeInventoryUtils.canFit(inv, getOutputSlots(), new ItemStack[] {chosen})) {
          continue;
        }

        ItemMeta itemMeta = itemInSlot.getItemMeta();
        if (itemMeta instanceof Damageable durability && !itemMeta.isUnbreakable()) {
          int current = durability.getDamage();
          if (current + 2 >= itemInSlot.getType().getMaxDurability()) {
            inv.consumeItem(slot);
          } else {
            durability.setDamage(current + 2);
            itemInSlot.setItemMeta(itemMeta);
            inv.replaceExistingItem(slot, itemInSlot);
          }
        }
        selectedOutput.put(inv.getBlock(), chosen);
        return produce;
      }
    }
    return null;
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
        selectedOutput.remove(b);
        updateStatusReset(inv);
      }
      return;
    }

    ItemStack output = selectedOutput.get(b);
    if (output == null) {
      processing.remove(b);
      progress.remove(b);
      updateStatusInvalidInput(inv);
      return;
    }
    if (!SupremeInventoryUtils.canFit(inv, getOutputSlots(), new ItemStack[] {output})) {
      updateStatusOutputFull(inv);
      return;
    }

    int timeLeft = progress.getOrDefault(b, active.getTicks());
    if (timeLeft <= 0) {
      SupremeInventoryUtils.pushAll(inv, getOutputSlots(), new ItemStack[] {output});
      processing.remove(b);
      progress.remove(b);
      selectedOutput.remove(b);
      updateStatusReset(inv);
      return;
    }

    if (getCharge(b.getLocation()) < getEnergyConsumption()) {
      updateStatusConnectEnergy(inv, output);
      return;
    }

    if (takeCharge(b.getLocation())) {
      ChestMenuUtils.updateProgressbar(inv, getStatusSlot(), timeLeft, active.getTicks(),
          getProgressBar());
      progress.put(b, Math.max(timeLeft - getSpeed(), 0));
    }
  }

  @Override
  protected void onMachineBreak(Block block) {
    processing.remove(block);
    progress.remove(block);
    selectedOutput.remove(block);
  }

  @Nonnull
  @Override
  public String getMachineIdentifier() {
    return "VIRTUAL_AQUARIUM";
  }

  @Override
  public ItemStack getProgressBar() {
    return new ItemStack(Material.FISHING_ROD);
  }


  public MachineRecipe getProcessing(Block b) {
    return processing.get(b);
  }

  public boolean isProcessing(Block b) {
    return getProcessing(b) != null;
  }


}
