package com.github.relativobr.supreme;

import static com.github.relativobr.supreme.util.CompatibilySupremeLegacy.loadComponents;
import static com.github.relativobr.supreme.util.CompatibilySupremeLegacy.loadCoreResource;
import static com.github.relativobr.supreme.util.CompatibilySupremeLegacy.loadGear;
import static com.github.relativobr.supreme.util.CompatibilySupremeLegacy.loadGenerators;
import static com.github.relativobr.supreme.util.CompatibilySupremeLegacy.loadMachines;

import com.github.relativobr.supreme.setup.MainSetup;
import com.github.relativobr.supreme.util.CompatibilySupremeLegacyItem;
import com.github.relativobr.supreme.util.SupremeOptions;
import com.github.relativobr.supreme.util.SupremePowerSection;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class Supreme extends JavaPlugin implements SlimefunAddon {

  private static Supreme instance;
  private static SupremeOptions supremeOptions = null;
  private static SupremePowerSection supremePowerSection = null;
  private static SupremeLocalization localization = null;
  private static List<CompatibilySupremeLegacyItem> legacyItem = null;

  public static Supreme inst() {
    return instance;
  }

  public static SupremeOptions getSupremeOptions() {
    if (supremeOptions == null) {
      ConfigurationSection typeSection = inst().getConfig().getConfigurationSection("options");
      if (typeSection == null) {
        supremeOptions = SupremeOptions.defaultValue();
      } else {
        supremeOptions = SupremeOptions.builder()
                .autoUpdate(typeSection.getBoolean("auto-update", false))
                .useLegacySupremeexpansionItemId(
                    typeSection.getBoolean("use-legacy-supremeexpansion-item-id", false))
                .lang(typeSection.getString("lang", "en-US"))
                .customTickerDelay(typeSection.getInt("custom-ticker-delay", 2))
                .enableGenerators(typeSection.getBoolean("enable-generators", true))
                .limitProductionGenerators(typeSection.getBoolean("limit-production-generators", false))
                .delayTimeValidGenerators(typeSection.getInt("delay-time-valid-generators", 600))
                .enableQuarry(typeSection.getBoolean("enable-quarry", true))
                .limitProductionQuarry(typeSection.getBoolean("limit-production-quarry", false))
                .baseTimeVirtualGarden(typeSection.getInt("base-time-virtual-garden", 15))
                .baseTimeVirtualAquarium(typeSection.getInt("base-time-virtual-aquarium", 15))
                .baseTimeMobCollector(typeSection.getInt("base-time-mob-collector", 15))
                .baseTimeTechGenerator(typeSection.getInt("base-time-tech-generator", 1800))
                .maxAmountTechGenerator(typeSection.getInt("tech-generator-max-amount", 64))
                .mobTechEnableBee(typeSection.getBoolean("mob-tech-enable-bee", true))
                .mobTechEnableIronGolem(typeSection.getBoolean("mob-tech-enable-iron-golem", true))
                .mobTechEnableZombie(typeSection.getBoolean("mob-tech-enable-zombie", true))
                .enableWeapons(typeSection.getBoolean("enable-weapons", true))
                .enableTools(typeSection.getBoolean("enable-tools", true))
                .enableArmor(typeSection.getBoolean("enable-armor", true))
                .enableTech(typeSection.getBoolean("enable-tech", true))
                .customBc(typeSection.getBoolean("custom-bc", false))
                .machineMaxAttemptConsumed(typeSection.getInt("machine-max-attempt-consumed", 30))
                .build();
      }
    }
    return supremeOptions;
  }

  public static SupremePowerSection getSupremePowerSection() {
    if (supremePowerSection == null) {
      ConfigurationSection typeSection = inst().getConfig().getConfigurationSection("power-section");
      if (typeSection == null) {
        supremePowerSection = SupremePowerSection.defaultValue();
      } else {
        supremePowerSection = SupremePowerSection.builder()
                .capacitorAurumCapacity(typeSection.getInt("capacitor-aurum-capacity", 1000000))
                .capacitorTitaniumCapacity(typeSection.getInt("capacitor-titanium-capacity", 4000000))
                .capacitorAdamantiumCapacity(typeSection.getInt("capacitor-adamantium-capacity", 16000000))
                .capacitorThorniumCapacity(typeSection.getInt("capacitor-thornium-capacity", 100000000))
                .capacitorSupremeCapacity(typeSection.getInt("capacitor-supreme-capacity", 1600000000))
                .generatorBasicIgnisEnergy(typeSection.getInt("generator-basic-ignis-energy", 2500))
                .generatorBasicIgnisBuffer(typeSection.getInt("generator-basic-ignis-buffer", 5000))
                .generatorBasicVentusEnergy(typeSection.getInt("generator-basic-ventus-energy", 2500))
                .generatorBasicVentusBuffer(typeSection.getInt("generator-basic-ventus-buffer", 5000))
                .generatorBasicAquaEnergy(typeSection.getInt("generator-basic-aqua-energy", 2500))
                .generatorBasicAquaBuffer(typeSection.getInt("generator-basic-aqua-buffer", 5000))
                .generatorBasicLuxEnergy(typeSection.getInt("generator-basic-lux-energy", 2500))
                .generatorBasicLuxBuffer(typeSection.getInt("generator-basic-lux-buffer", 5000))
                .generatorBasicLumiumEnergy(typeSection.getInt("generator-basic-lumium-energy", 5000))
                .generatorBasicLumiumBuffer(typeSection.getInt("generator-basic-lumium-buffer", 10000))
                .generatorIgnisEnergy(typeSection.getInt("generator-ignis-energy", 25000))
                .generatorIgnisBuffer(typeSection.getInt("generator-ignis-buffer", 50000))
                .generatorVentusEnergy(typeSection.getInt("generator-ventus-energy", 25000))
                .generatorVentusBuffer(typeSection.getInt("generator-ventus-buffer", 50000))
                .generatorAquaEnergy(typeSection.getInt("generator-aqua-energy", 25000))
                .generatorAquaBuffer(typeSection.getInt("generator-aqua-buffer", 50000))
                .generatorLuxEnergy(typeSection.getInt("generator-lux-energy", 25000))
                .generatorLuxBuffer(typeSection.getInt("generator-lux-buffer", 50000))
                .generatorLumiumEnergy(typeSection.getInt("generator-lumium-energy", 75000))
                .generatorLumiumBuffer(typeSection.getInt("generator-lumium-buffer", 500000))
                .generatorThorniumEnergy(typeSection.getInt("generator-thornium-energy", 1000000))
                .generatorThorniumBuffer(typeSection.getInt("generator-thornium-buffer", 6000000))
                .generatorSupremeEnergy(typeSection.getInt("generator-supreme-energy", 2000000))
                .generatorSupremeBuffer(typeSection.getInt("generator-supreme-buffer", 12000000))
                .build();
      }
    }
    return supremePowerSection;
  }

  public static SupremeLocalization getLocalization() {
    if (localization == null) {
      localization = new SupremeLocalization(inst());
      localization.addLanguage(getSupremeOptions().getLang());
    }
    return localization;
  }

  public static List<CompatibilySupremeLegacyItem> getLegacyItem() {
    if (legacyItem == null || legacyItem.isEmpty()) {
      if (legacyItem == null) {
        legacyItem = new ArrayList<>();
      }
      loadComponents(legacyItem);
      loadGear(legacyItem);
      loadGenerators(legacyItem);
      loadMachines(legacyItem);
      loadCoreResource(legacyItem);
    }
    return legacyItem;
  }

  @Override
  public void onEnable() {

    instance = this;

    Supreme.inst().log(Level.INFO, "########################################");
    Supreme.inst().log(Level.INFO, "      Supreme - By RelativoBR      ");
    Supreme.inst().log(Level.INFO, "########################################");

    Config cfg = new Config(this);
    if (getSupremeOptions() == null) {
      log(Level.SEVERE, "Config section \"options\" missing, Check your config and report this!");
      inst().onDisable();
      return;
    }

    if (getSupremeOptions().isAutoUpdate()) {
      log(Level.WARNING,
          "The original Supreme auto-updater is disabled in Supreme Legacy. Use your fork's GitHub Actions/releases for updates.");
    }

    // localization
    Supreme.inst().log(Level.INFO, "Loaded language Supreme: " + getSupremeOptions().getLang());
    getLocalization();

    // check Compatibily Legacy (SupremeExpansion)
    if (getSupremeOptions().isUseLegacySupremeexpansionItemId()) {
      Supreme.inst().log(Level.INFO, "Legacy SupremeExpansion IDs: enable");
      getLegacyItem();
    } else {
      Supreme.inst().log(Level.INFO, "Legacy SupremeExpansion IDs: disable");
    }

    MainSetup.setup(this);

  }

  @Override
  public void onDisable() {
    instance = null;
  }

  @Override
  public String getBugTrackerURL() {
    return "";
  }

  @Override
  public JavaPlugin getJavaPlugin() {
    return this;
  }

  public final void log(Level level, String messages) {
    this.getLogger().log(level, messages);
  }

}
