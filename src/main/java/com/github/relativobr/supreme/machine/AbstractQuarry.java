package com.github.relativobr.supreme.machine;

import static com.github.relativobr.supreme.Supreme.getSupremeOptions;

import com.github.relativobr.supreme.compat.SupremeBlockTicker;
import com.github.relativobr.supreme.generic.recipe.InventoryRecipe;
import com.github.relativobr.supreme.util.ItemUtil;
import com.github.relativobr.supreme.util.SupremeQuarryOutput;
import com.github.relativobr.supreme.util.UtilEnergy;
import com.github.relativobr.supreme.util.UtilMachine;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemState;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.LoreBuilder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class AbstractQuarry extends SlimefunItem implements EnergyNetComponent {

  private static final DecimalFormat FORMAT = new DecimalFormat("###,###,##0.00",
      DecimalFormatSymbols.getInstance(Locale.ENGLISH));
  private static final String OWNER_KEY = "owner";
  private static final String ENABLED_KEY = "supreme_enabled";
  private static final Particle HAPPY_PARTICLE = resolveParticle("HAPPY_VILLAGER",
      "VILLAGER_HAPPY");

  private final Map<BlockPosition, Integer> tickDelays = new ConcurrentHashMap<>();
  private int energyConsumed = -1;
  private int energyCapacity = -1;
  private boolean effect = true;
  private int delaySpeed = 1;
  private SupremeQuarryOutput output;

  @ParametersAreNonnullByDefault
  public AbstractQuarry(ItemGroup category, SlimefunItemStack machine, ItemStack[] recipe) {
    super(category, machine, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);
    addItemHandler(onPlace(), onRightClick(), onBreak());
  }

  @Nonnull
  public static String format(double number) {
    return FORMAT.format(number);
  }

  @Override
  public void preRegister() {
    addItemHandler(new SupremeBlockTicker(true, this::tick));
  }

  private void tick(Block b) {
    Block targetBlock = b.getRelative(BlockFace.DOWN);
    if (!isEnabled(b) || isInvalidInventory(targetBlock)
        || getCharge(b.getLocation()) < getEnergyConsumption()) {
      return;
    }

    BlockPosition position = new BlockPosition(b.getLocation());
    int threshold = Math.max(1, getSupremeOptions().getCustomTickerDelay() * delaySpeed);
    int delay = tickDelays.getOrDefault(position, 0) + 1;
    if (delay < threshold) {
      tickDelays.put(position, delay);
      return;
    }
    tickDelays.put(position, 0);

    BlockState state = targetBlock.getState();
    if (!(state instanceof InventoryHolder holder)) {
      return;
    }

    ItemStack material = ItemUtil.getItemQuarry(getOutput(), UtilMachine.getRandomInt());
    if (material == null || material.getType().isAir()) {
      return;
    }

    ItemStack outputItem = material.clone();
    outputItem.setAmount(1);
    Inventory inventory = holder.getInventory();
    if (!inventory.addItem(outputItem).isEmpty()) {
      return;
    }

    if (effect && HAPPY_PARTICLE != null) {
      Location location = b.getLocation().add(0.5, 0.8, 0.5);
      b.getWorld().spawnParticle(HAPPY_PARTICLE, location, 6);
    }
    removeCharge(b.getLocation(), getEnergyConsumption());
  }

  @Nonnull
  private BlockPlaceHandler onPlace() {
    return new BlockPlaceHandler(false) {
      @Override
      public void onPlayerPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        BlockStorage.addBlockInfo(block, OWNER_KEY, event.getPlayer().getUniqueId().toString());
        setEnabled(block, true);
      }
    };
  }

  @Nonnull
  private BlockBreakHandler onBreak() {
    return new SimpleBlockBreakHandler() {
      @Override
      public void onBlockBreak(Block block) {
        tickDelays.remove(new BlockPosition(block.getLocation()));
      }
    };
  }

  @Nonnull
  public BlockUseHandler onRightClick() {
    return event -> {
      event.cancel();
      Player player = event.getPlayer();
      Block block = event.getClickedBlock().orElse(null);
      if (block == null) {
        return;
      }

      if (isInvalidInventory(block.getRelative(BlockFace.DOWN))) {
        Slimefun.getLocalization().sendMessage(player, "machines.CARGO_NODES.must-be-placed");
        return;
      }

      String owner = BlockStorage.getLocationInfo(block.getLocation(), OWNER_KEY);
      boolean ownsBlock = owner == null || owner.equals(player.getUniqueId().toString());
      if (ownsBlock && Slimefun.getProtectionManager()
          .hasPermission(player, block, Interaction.INTERACT_BLOCK)) {
        showMachine(player, block);
      } else {
        Slimefun.getLocalization().sendMessage(player, "inventory.no-access");
      }
    };
  }

  @ParametersAreNonnullByDefault
  protected void showMachine(Player player, Block block) {
    Validate.notNull(player, "The Player should not be null");
    Validate.notNull(block, "The Block should not be null");

    ChestMenu menu = new ChestMenu(getItemName());
    menu.setPlayerInventoryClickable(false);
    menu.setEmptySlotsClickable(false);
    ChestMenuUtils.drawBackground(menu, InventoryRecipe.QUARRY_BORDER);
    ChestMenuUtils.drawBackground(menu, InventoryRecipe.QUARRY_OUTPUT);
    ChestMenuUtils.drawBackground(menu, InventoryRecipe.QUARRY_OUTPUT_BORDER);

    for (int slot : InventoryRecipe.QUARRY_INPUT_BORDER) {
      menu.addItem(slot, ChestMenuUtils.getInputSlotTexture(),
          ChestMenuUtils.getEmptyClickHandler());
    }

    int energyCharge = getCharge(block.getLocation());
    boolean enabled = isEnabled(block);
    String powerPerTick = UtilEnergy.energyPowerPerTick(getEnergyConsumption());
    String powerCharged = LoreBuilder.powerCharged(energyCharge, getCapacity());
    String infoSpeed = UtilEnergy.timePerItem(
        (getSupremeOptions().getCustomTickerDelay() * delaySpeed) / 2);

    if (energyCharge < getEnergyConsumption() || !enabled) {
      menu.addItem(InventoryRecipe.QUARRY_STATUS,
          new CustomItemStack(Material.OBSIDIAN, ChatColor.RED + "NOT-ACTIVE", powerPerTick,
              powerCharged, infoSpeed));
    } else {
      menu.addItem(InventoryRecipe.QUARRY_STATUS,
          new CustomItemStack(Material.GLOWSTONE, ChatColor.GREEN + "ACTIVE", powerPerTick,
              powerCharged, infoSpeed));
    }
    menu.addMenuClickHandler(InventoryRecipe.QUARRY_STATUS,
        ChestMenuUtils.getEmptyClickHandler());

    if (enabled) {
      menu.addItem(InventoryRecipe.QUARRY_BUTTON, new CustomItemStack(Material.EMERALD_BLOCK,
          Slimefun.getLocalization().getMessages(player,
              "messages.auto-crafting.tooltips.enabled")));
      menu.addMenuClickHandler(InventoryRecipe.QUARRY_BUTTON, (pl, item, slot, action) -> {
        setEnabled(block, false);
        showMachine(player, block);
        return false;
      });
    } else {
      menu.addItem(InventoryRecipe.QUARRY_BUTTON, new CustomItemStack(Material.REDSTONE_BLOCK,
          Slimefun.getLocalization().getMessages(player,
              "messages.auto-crafting.tooltips.disabled")));
      menu.addMenuClickHandler(InventoryRecipe.QUARRY_BUTTON, (pl, item, slot, action) -> {
        setEnabled(block, true);
        showMachine(player, block);
        return false;
      });
    }

    player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1, 1);
    menu.open(player);
  }

  protected boolean isInvalidInventory(@Nonnull Block block) {
    return !(block.getState() instanceof InventoryHolder);
  }

  private boolean isEnabled(Block block) {
    return !"false".equalsIgnoreCase(
        BlockStorage.getLocationInfo(block.getLocation(), ENABLED_KEY));
  }

  private void setEnabled(Block block, boolean enabled) {
    BlockStorage.addBlockInfo(block, ENABLED_KEY, Boolean.toString(enabled));
  }

  private static Particle resolveParticle(String... names) {
    for (String name : names) {
      try {
        return Particle.valueOf(name);
      } catch (IllegalArgumentException ignored) {
        // Try the name used by the next supported Minecraft generation.
      }
    }
    return null;
  }

  @Override
  public int getCapacity() {
    return energyCapacity;
  }

  @Nonnull
  public final AbstractQuarry setCapacity(int capacity) {
    Validate.isTrue(capacity > 0, "The capacity must be greater than zero!");
    if (getState() == ItemState.UNREGISTERED) {
      energyCapacity = capacity;
      return this;
    }
    throw new IllegalStateException("You cannot modify the capacity after the Item was registered.");
  }

  public int getEnergyConsumption() {
    return energyConsumed;
  }

  @Nonnull
  public final AbstractQuarry setEnergyConsumption(int energyConsumption) {
    Validate.isTrue(energyConsumption > 0,
        "The energy consumption must be greater than zero!");
    Validate.isTrue(energyCapacity > 0,
        "You must specify the capacity before you can set the consumption amount.");
    Validate.isTrue(energyConsumption <= energyCapacity,
        "The energy consumption cannot be higher than the capacity (" + energyCapacity + ')');
    energyConsumed = energyConsumption;
    return this;
  }

  @Nonnull
  @Override
  public final EnergyNetComponentType getEnergyComponentType() {
    return EnergyNetComponentType.CONSUMER;
  }

  public SupremeQuarryOutput getOutput() {
    return output;
  }

  public AbstractQuarry setOutput(SupremeQuarryOutput output) {
    this.output = output;
    return this;
  }

  public AbstractQuarry setEffect(boolean effect) {
    this.effect = effect;
    return this;
  }

  public AbstractQuarry setDelaySpeed(int delaySpeed) {
    this.delaySpeed = delaySpeed;
    return this;
  }
}
