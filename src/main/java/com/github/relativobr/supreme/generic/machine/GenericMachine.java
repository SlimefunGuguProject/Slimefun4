package com.github.relativobr.supreme.generic.machine;

import static java.util.Objects.nonNull;

import com.github.relativobr.supreme.Supreme;
import com.github.relativobr.supreme.generic.recipe.AbstractItemRecipe;
import com.github.relativobr.supreme.generic.recipe.InventoryRecipe;
import com.github.relativobr.supreme.util.SupremeInventoryUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotHopperable;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Shared implementation for Supreme's container machines.
 *
 * <p>The state maps are deliberately keyed by placed block so machines never share progress. Inputs may remain visible across several cargo deliveries, are consumed atomically once complete,
 * and are restored if the block is broken before the completed output is delivered.</p>
 */
public class GenericMachine extends AContainer implements NotHopperable, RecipeDisplayItem {

  private final Map<Block, MachineRecipe> processing = new HashMap<>();
  private final Map<Block, Integer> progressTime = new HashMap<>();
  private final Map<Block, Map<ItemStack, Integer>> consumedItemsMap = new HashMap<>();
  private final Map<Block, Integer> attemptCount = new HashMap<>();
  public final List<AbstractItemRecipe> machineRecipes = new ArrayList<>();
  private Integer timeProcess;
  private String machineIdentifier = "MediumContainerMachine";

  @ParametersAreNonnullByDefault
  public GenericMachine(ItemGroup category, SlimefunItemStack item, RecipeType recipeType,
      ItemStack[] recipe) {
    super(category, item, recipeType, recipe);

    addItemHandler(onBlockBreak());

    new BlockMenuPreset(getId(), getItemName()) {

      @Override
      public void init() {
        constructMenu(this);
      }

      @Override
      public boolean canOpen(Block b, Player p) {
        return p.hasPermission("slimefun.inventory.bypass") || Slimefun.getProtectionManager()
            .hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
      }

      @Override
      public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
        return flow == ItemTransportFlow.WITHDRAW ? getOutputSlots() : getInputSlots();
      }

      @Override
      public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow,
          ItemStack item) {
        if (flow == ItemTransportFlow.WITHDRAW) {
          return getOutputSlots();
        }

        // Include empty slots even when the same ingredient is already present. The old behavior
        // exposed only matching occupied slots, which prevented cargo from filling recipes that
        // require the same item in more than one input slot.
        List<Integer> matching = new LinkedList<>();
        List<Integer> empty = new LinkedList<>();
        for (int slot : getInputSlots()) {
          ItemStack stack = menu.getItemInSlot(slot);
          if (stack == null || stack.getType().isAir()) {
            empty.add(slot);
          } else if (SlimefunUtils.isItemSimilar(stack, item, false, true)
              && stack.getAmount() < stack.getMaxStackSize()) {
            matching.add(slot);
          }
        }

        matching.sort(Comparator.comparingInt(slot -> menu.getItemInSlot(slot).getAmount()));
        matching.addAll(empty);
        return matching.stream().mapToInt(Integer::intValue).toArray();
      }
    };
  }

  @Nonnull
  @Override
  protected BlockBreakHandler onBlockBreak() {
    return new SimpleBlockBreakHandler() {
      @Override
      public void onBlockBreak(Block b) {
        BlockMenu inv = BlockStorage.getInventory(b);
        if (inv != null) {
          // Restore reserved inputs before the normal menu drop path, returning each item once.
          revertConsumedItem(b, inv);
          inv.dropItems(b.getLocation(), getInputSlots());
          inv.dropItems(b.getLocation(), getOutputSlots());
        } else {
          dropConsumedItems(b);
        }
        onMachineBreak(b);
        removeMapBlock(b);
      }
    };
  }

  /** Allows special machines to clear their own per-block state. */
  protected void onMachineBreak(Block block) {
    // Default machines have no additional state.
  }

  /**
   * Gives specialized machines a chance to validate external state immediately before inputs are
   * consumed. Returning {@code false} safely cancels the pending cycle.
   */
  protected boolean canStartProcess(Block block, BlockMenu menu, MachineRecipe recipe) {
    return true;
  }

  /** Called once after the complete recipe input and energy charge have been secured. */
  protected void onProcessStarted(Block block, BlockMenu menu, MachineRecipe recipe) {
    // Default machines do not have external state to commit.
  }

  protected void updateStatusReset(BlockMenu menu) {
    menu.replaceExistingItem(getStatusSlot(), getDisplayOrInfo(null, " "));
  }

  protected void updateStatusInvalidInput(BlockMenu menu) {
    menu.replaceExistingItem(getStatusSlot(),
        getDisplayOrWarn(null, "&cInput a valid material to start"));
  }

  protected void updateStatusOutputFull(BlockMenu menu) {
    menu.replaceExistingItem(getStatusSlot(), getDisplayOrWarn(null, "&cOutput is full"));
  }

  protected void updateStatusConnectEnergy(BlockMenu menu, ItemStack itemStack) {
    menu.replaceExistingItem(getStatusSlot(),
        getDisplayOrWarn(itemStack, "&cConnect energy to continue"));
  }

  protected void updateStatusLoadMaterial(BlockMenu menu, ItemStack itemStack, int attempts,
      int progressCount, int totalProgress) {
    CustomItemStack infoDetail = new CustomItemStack(itemStack,
        "&cLoad more material to start", "",
        "&7Attempts: &e" + attempts + " &7/ &e"
            + Supreme.getSupremeOptions().getMachineMaxAttemptConsumed(),
        "&7Progress: &e" + progressCount + " &7/ &e" + totalProgress, "");
    menu.replaceExistingItem(getStatusSlot(), infoDetail);
  }

  @Nonnull
  public GenericMachine setMachineRecipes(@Nonnull List<AbstractItemRecipe> recipes) {
    machineRecipes.clear();
    machineRecipes.addAll(recipes);
    return this;
  }

  public GenericMachine setTimeProcess(int timeProcess) {
    this.timeProcess = timeProcess;
    return this;
  }

  public int getTimeProcess() {
    if (timeProcess == null) {
      timeProcess = 15;
    }
    return timeProcess;
  }

  @Override
  protected void constructMenu(BlockMenuPreset preset) {
    for (int i : getBorderSlots()) {
      preset.addItem(i, new CustomItemStack(Material.GRAY_STAINED_GLASS_PANE, " "),
          ChestMenuUtils.getEmptyClickHandler());
    }
    for (int i : getInputBorderSlots()) {
      preset.addItem(i, new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE, " "),
          ChestMenuUtils.getEmptyClickHandler());
    }
    for (int i : getOutputBorderSlots()) {
      preset.addItem(i, new CustomItemStack(Material.ORANGE_STAINED_GLASS_PANE, " "),
          ChestMenuUtils.getEmptyClickHandler());
    }

    preset.addItem(getStatusSlot(), new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "),
        ChestMenuUtils.getEmptyClickHandler());

    for (int i : getOutputSlots()) {
      preset.addMenuClickHandler(i, new ChestMenu.AdvancedMenuClickHandler() {
        @Override
        public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
          return false;
        }

        @Override
        public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor,
            ClickAction action) {
          return cursor == null || cursor.getType() == Material.AIR;
        }
      });
    }
  }

  @Override
  public ItemStack getProgressBar() {
    return new ItemStack(Material.PISTON);
  }

  @Override
  public int[] getInputSlots() {
    return InventoryRecipe.MEDIUM_INPUT;
  }

  @Override
  public int[] getOutputSlots() {
    return InventoryRecipe.MEDIUM_OUTPUT;
  }

  public int getStatusSlot() {
    return InventoryRecipe.MEDIUM_STATUS_SLOT;
  }

  public int[] getBorderSlots() {
    return InventoryRecipe.MEDIUM_BORDER;
  }

  public int[] getInputBorderSlots() {
    return InventoryRecipe.MEDIUM_INPUT_BORDER;
  }

  public int[] getOutputBorderSlots() {
    return InventoryRecipe.MEDIUM_OUTPUT_BORDER;
  }

  @Nonnull
  @Override
  public String getMachineIdentifier() {
    return nonNull(machineIdentifier) ? machineIdentifier : "MachineIdentifier";
  }

  @Nonnull
  public GenericMachine setMachineIdentifier(@Nonnull String identifier) {
    machineIdentifier = identifier;
    return this;
  }

  @Override
  protected void tick(Block b) {
    BlockMenu inv = BlockStorage.getInventory(b);
    if (inv == null) {
      return;
    }

    if (isProcessing(b)) {
      doProcessing(b, inv);
    } else {
      nextProcessing(b, inv);
    }
  }

  @Nonnull
  @Override
  public List<ItemStack> getDisplayRecipes() {
    List<ItemStack> displayRecipes = new ArrayList<>();
    for (AbstractItemRecipe recipe : machineRecipes) {
      displayRecipes.add(new CustomItemStack(Material.GRAY_STAINED_GLASS_PANE, " "));
      displayRecipes.add(recipe.getFirstItemOutput());
    }
    return displayRecipes;
  }

  @Override
  protected MachineRecipe findNextRecipe(BlockMenu inv) {
    for (AbstractItemRecipe recipe : machineRecipes) {
      ItemStack[] input = recipe.getInputNotNull();
      if (matchingRecipe(input, inv)) {
        return new MachineRecipe(getTimeProcess(), input, recipe.getOutputNotNull());
      }
    }
    return null;
  }

  protected int getProgressTime(Block b) {
    return progressTime.getOrDefault(b, getTimeProcess());
  }

  protected MachineRecipe getProcessing(Block b) {
    return processing.get(b);
  }

  protected Map<ItemStack, Integer> getConsumedItems(Block b) {
    return consumedItemsMap.computeIfAbsent(b, ignored -> new LinkedHashMap<>());
  }

  protected boolean isProcessing(Block b) {
    return getProcessing(b) != null;
  }

  protected boolean notHasSpaceOutput(BlockMenu inv, ItemStack[] result) {
    return !SupremeInventoryUtils.canFit(inv, getOutputSlots(), result);
  }

  private void nextProcessing(Block b, BlockMenu inv) {
    MachineRecipe next = findNextRecipe(inv);
    if (next != null) {
      processing.put(b, next);
      progressTime.put(b, next.getTicks());
      consumedItemsMap.put(b, new LinkedHashMap<>());
      attemptCount.put(b, 0);
    } else if (getInputSlots().length <= 5) {
      updateStatusReset(inv);
    } else {
      updateStatusInvalidInput(inv);
    }
  }

  protected final void removeMapBlock(Block b) {
    progressTime.remove(b);
    processing.remove(b);
    attemptCount.remove(b);
    consumedItemsMap.remove(b);
  }

  private void doProcessing(Block b, BlockMenu inv) {
    MachineRecipe recipe = getProcessing(b);
    if (recipe == null) {
      removeMapBlock(b);
      return;
    }

    ItemStack[] result = recipe.getOutput();
    if (result == null || result.length == 0) {
      removeMapBlock(b);
      updateStatusReset(inv);
      return;
    }

    int ticks = recipe.getTicks();
    int ticksRemaining = getProgressTime(b);

    if (ticksRemaining == ticks) {
      startProcessTicks(b, inv, ticksRemaining);
    } else if (ticksRemaining <= 0) {
      endProcessTicks(b, inv, result);
    } else if (takeMachineCharge(b, inv)) {
      doProcessTicks(b, inv, ticks, ticksRemaining, result[0]);
    }
  }

  private boolean takeMachineCharge(Block b, BlockMenu inv) {
    if (getCharge(b.getLocation()) < getEnergyConsumption()) {
      updateStatusConnectEnergy(inv, null);
      return false;
    }
    removeCharge(b.getLocation(), getEnergyConsumption());
    return true;
  }

  private void startProcessTicks(Block b, BlockMenu inv, int ticksRemaining) {
    MachineRecipe recipe = getProcessing(b);
    if (recipe == null) {
      removeMapBlock(b);
      return;
    }

    // Do not reserve ingredients while a complete result has nowhere to go.
    if (notHasSpaceOutput(inv, recipe.getOutput())) {
      updateStatusOutputFull(inv);
      return;
    }

    if (!canStartProcess(b, inv, recipe)) {
      revertConsumedItem(b, inv);
      removeMapBlock(b);
      updateStatusInvalidInput(inv);
      return;
    }

    int attempts = attemptCount.getOrDefault(b, 0) + 1;
    if (!hasCompleteRecipeInputs(inv, recipe.getInput())) {
      if (attempts >= Supreme.getSupremeOptions().getMachineMaxAttemptConsumed()) {
        removeMapBlock(b);
        updateStatusInvalidInput(inv);
        return;
      }

      attemptCount.put(b, attempts);
      int progressCount = countAvailableRecipeInputs(inv, recipe.getInput());
      int totalProgress = Arrays.stream(recipe.getInput())
          .filter(java.util.Objects::nonNull)
          .mapToInt(ItemStack::getAmount)
          .sum();
      updateStatusLoadMaterial(inv, recipe.getOutput()[0], attempts, progressCount,
          totalProgress);
      return;
    }

    if (getCharge(b.getLocation()) < getEnergyConsumption()) {
      updateStatusConnectEnergy(inv, recipe.getOutput()[0]);
      return;
    }

    // Availability was checked immediately above on the synchronized ticker, so this commit is
    // atomic from the server's perspective. Inputs are never hidden while waiting for power.
    if (!consumptionRecipe(b, inv)) {
      removeMapBlock(b);
      updateStatusInvalidInput(inv);
      return;
    }

    removeCharge(b.getLocation(), getEnergyConsumption());
    onProcessStarted(b, inv, recipe);
    progressTime.put(b, Math.max(ticksRemaining - getSpeed(), 0));
    attemptCount.put(b, 0);
  }

  private void revertConsumedItem(Block b, BlockMenu inv) {
    Map<ItemStack, Integer> consumedItems = consumedItemsMap.get(b);
    if (consumedItems == null || consumedItems.isEmpty()) {
      return;
    }

    for (Map.Entry<ItemStack, Integer> consumedEntry : consumedItems.entrySet()) {
      ItemStack consumedItem = consumedEntry.getKey();
      int amount = consumedEntry.getValue();
      if (consumedItem == null || consumedItem.getType().isAir()) {
        continue;
      }

      int maxStackSize = consumedItem.getMaxStackSize();
      while (amount > 0) {
        int stackSize = Math.min(maxStackSize, amount);
        ItemStack returnItem = consumedItem.clone();
        returnItem.setAmount(stackSize);
        ItemStack leftover = inv.pushItem(returnItem, getInputSlots());
        if (leftover != null && b.getWorld() != null) {
          b.getWorld().dropItemNaturally(b.getLocation(), leftover);
        }
        amount -= stackSize;
      }
    }
    consumedItems.clear();
  }

  private void dropConsumedItems(Block block) {
    Map<ItemStack, Integer> consumedItems = consumedItemsMap.get(block);
    if (consumedItems == null || consumedItems.isEmpty() || block.getWorld() == null) {
      return;
    }

    for (Map.Entry<ItemStack, Integer> entry : consumedItems.entrySet()) {
      ItemStack item = entry.getKey();
      int amount = entry.getValue();
      if (item == null || item.getType().isAir()) {
        continue;
      }
      while (amount > 0) {
        int stackSize = Math.min(item.getMaxStackSize(), amount);
        ItemStack dropped = item.clone();
        dropped.setAmount(stackSize);
        block.getWorld().dropItemNaturally(block.getLocation(), dropped);
        amount -= stackSize;
      }
    }
    consumedItems.clear();
  }

  private void endProcessTicks(Block b, BlockMenu inv, ItemStack[] result) {
    if (notHasSpaceOutput(inv, result)) {
      updateStatusOutputFull(inv);
      return;
    }
    SupremeInventoryUtils.pushAll(inv, getOutputSlots(), result);
    removeMapBlock(b);
    updateStatusReset(inv);
  }

  private void doProcessTicks(Block b, BlockMenu inv, int ticks, int ticksRemaining,
      ItemStack result) {
    progressTime.put(b, Math.max(ticksRemaining - getSpeed(), 0));
    ChestMenuUtils.updateProgressbar(inv, getStatusSlot(), ticksRemaining, ticks, result);
  }

  private boolean consumptionRecipe(Block b, BlockMenu inv) {
    Map<ItemStack, Integer> requiredItems = groupSimilarItems(getProcessing(b).getInput());
    Map<ItemStack, Integer> consumedItems = getConsumedItems(b);

    for (Map.Entry<ItemStack, Integer> entry : requiredItems.entrySet()) {
      ItemStack requiredItem = entry.getKey();
      int remaining = entry.getValue();

      for (int slot : getInputSlots()) {
        ItemStack slotItem = inv.getItemInSlot(slot);
        if (slotItem == null || slotItem.getType().isAir()
            || !SlimefunUtils.isItemSimilar(slotItem, requiredItem, false, false)) {
          continue;
        }

        int amountToConsume = Math.min(slotItem.getAmount(), remaining);
        if (amountToConsume > 0) {
          ItemStack consumed = slotItem.clone();
          inv.consumeItem(slot, amountToConsume);
          mergeSimilar(consumedItems, consumed, amountToConsume);
          remaining -= amountToConsume;
        }
        if (remaining == 0) {
          break;
        }
      }

      if (remaining > 0) {
        // This should be unreachable on the synchronized ticker because availability was checked
        // immediately before committing, but keep the guard to avoid producing for free.
        revertConsumedItem(b, inv);
        return false;
      }
    }
    return true;
  }

  private boolean hasCompleteRecipeInputs(BlockMenu inv, ItemStack[] recipe) {
    Map<ItemStack, Integer> requiredItems = groupSimilarItems(recipe);
    for (Map.Entry<ItemStack, Integer> entry : requiredItems.entrySet()) {
      if (countAvailable(inv, entry.getKey()) < entry.getValue()) {
        return false;
      }
    }
    return true;
  }

  private int countAvailableRecipeInputs(BlockMenu inv, ItemStack[] recipe) {
    int available = 0;
    for (Map.Entry<ItemStack, Integer> entry : groupSimilarItems(recipe).entrySet()) {
      available += Math.min(entry.getValue(), countAvailable(inv, entry.getKey()));
    }
    return available;
  }

  private int countAvailable(BlockMenu inv, ItemStack requiredItem) {
    int amount = 0;
    for (int slot : getInputSlots()) {
      ItemStack slotItem = inv.getItemInSlot(slot);
      if (slotItem != null && !slotItem.getType().isAir()
          && SlimefunUtils.isItemSimilar(slotItem, requiredItem, false, false)) {
        amount += slotItem.getAmount();
      }
    }
    return amount;
  }

  private boolean matchingRecipe(ItemStack[] recipe, BlockMenu inv) {
    // Starting requires one visible item of each distinct ingredient. Full quantities may arrive
    // over several cargo ticks and remain in the input slots until the complete recipe can commit.
    for (ItemStack required : groupSimilarItems(recipe).keySet()) {
      boolean present = false;
      for (int slot : getInputSlots()) {
        ItemStack itemInSlot = inv.getItemInSlot(slot);
        if (itemInSlot != null
            && SlimefunUtils.isItemSimilar(itemInSlot, required, false, false)) {
          present = true;
          break;
        }
      }
      if (!present) {
        return false;
      }
    }
    return true;
  }

  private Map<ItemStack, Integer> groupSimilarItems(ItemStack[] items) {
    Map<ItemStack, Integer> grouped = new LinkedHashMap<>();
    if (items == null) {
      return grouped;
    }
    for (ItemStack item : items) {
      if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
        mergeSimilar(grouped, item, item.getAmount());
      }
    }
    return grouped;
  }

  private void mergeSimilar(Map<ItemStack, Integer> items, ItemStack item, int amount) {
    for (Map.Entry<ItemStack, Integer> entry : items.entrySet()) {
      if (SlimefunUtils.isItemSimilar(entry.getKey(), item, false, false)) {
        entry.setValue(entry.getValue() + amount);
        return;
      }
    }
    ItemStack key = item.clone();
    key.setAmount(1);
    items.put(key, amount);
  }

  private ItemStack getDisplayOrInfo(ItemStack itemStack, String name) {
    return new CustomItemStack(
        itemStack != null ? itemStack : new ItemStack(Material.BLACK_STAINED_GLASS_PANE), name);
  }

  private ItemStack getDisplayOrWarn(ItemStack itemStack, String name) {
    return new CustomItemStack(
        itemStack != null ? itemStack : new ItemStack(Material.RED_STAINED_GLASS_PANE), name);
  }
}
