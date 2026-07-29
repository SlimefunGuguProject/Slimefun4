package com.github.relativobr.supreme.setup;

import static com.github.relativobr.supreme.Supreme.getSupremeOptions;
import static com.github.relativobr.supreme.util.ItemUtil.getCapacitorCapacity;
import static com.github.relativobr.supreme.util.ItemUtil.getValueGeneratorsWithLimit;

import com.github.relativobr.supreme.generic.electric.EnergyCapacitor;
import com.github.relativobr.supreme.generic.electric.EnergyGenerator;
import com.github.relativobr.supreme.Supreme;
import com.github.relativobr.supreme.generators.GeneratorMob;
import com.github.relativobr.supreme.generators.SupremeCapacitor;
import com.github.relativobr.supreme.generators.SupremeGenerator;
import com.github.relativobr.supreme.generic.electric.GenerationType;
import com.github.relativobr.supreme.util.ItemGroups;

public class SetupGenerators {

  public static void setup(Supreme sup) {

    if (getSupremeOptions().isEnableTech()) {

      new GeneratorMob(GeneratorMob.GENERATOR_MOB_BASIC, GeneratorMob.RECIPE_GENERATOR_MOB_BASIC).setBuffer(
              getValueGeneratorsWithLimit(GeneratorMob.BASIC_GENERATOR_MOB_BUFFER))
          .setEnergy(getValueGeneratorsWithLimit(GeneratorMob.BASIC_GENERATOR_MOB_ENERGY)).setMobRange(3).register(sup);

      new GeneratorMob(GeneratorMob.GENERATOR_MOB_MEDIUM, GeneratorMob.RECIPE_GENERATOR_MOB_MEDIUM).setBuffer(
              getValueGeneratorsWithLimit(GeneratorMob.MEDIUM_GENERATOR_MOB_BUFFER))
          .setEnergy(getValueGeneratorsWithLimit(GeneratorMob.MEDIUM_GENERATOR_MOB_ENERGY)).setMobRange(6)
          .register(sup);

      new GeneratorMob(GeneratorMob.GENERATOR_MOB_ADVANCED, GeneratorMob.RECIPE_GENERATOR_MOB_ADVANCED).setBuffer(
              getValueGeneratorsWithLimit(GeneratorMob.ADVANCED_GENERATOR_MOB_BUFFER))
          .setEnergy(getValueGeneratorsWithLimit(GeneratorMob.ADVANCED_GENERATOR_MOB_ENERGY)).setMobRange(9)
          .register(sup);

    }

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.BASIC_IGNIS_GENERATOR,
        SupremeGenerator.RECIPE_BASIC_IGNIS_GENERATOR).setBuffer(
            getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorBasicIgnisBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorBasicIgnisEnergy())).setType(GenerationType.FIRE)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.IGNIS_GENERATOR,
        SupremeGenerator.RECIPE_IGNIS_GENERATOR).setBuffer(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorIgnisBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorIgnisEnergy())).setType(GenerationType.FIRE)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.BASIC_VENTUS_GENERATOR,
        SupremeGenerator.RECIPE_BASIC_VENTUS_GENERATOR).setBuffer(
            getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorBasicVentusBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorBasicVentusEnergy())).setType(GenerationType.WIND)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.VENTUS_GENERATOR,
        SupremeGenerator.RECIPE_VENTUS_GENERATOR).setBuffer(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorVentusBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorVentusEnergy())).setType(GenerationType.WIND)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.BASIC_AQUA_GENERATOR,
        SupremeGenerator.RECIPE_BASIC_AQUA_GENERATOR).setBuffer(
            getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorBasicAquaBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorBasicAquaEnergy())).setType(GenerationType.WATER)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.AQUA_GENERATOR,
        SupremeGenerator.RECIPE_AQUA_GENERATOR).setBuffer(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorAquaBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorAquaEnergy())).setType(GenerationType.WATER)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.BASIC_LUX_GENERATOR,
        SupremeGenerator.RECIPE_BASIC_LUX_GENERATOR).setBuffer(
            getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorBasicLuxBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorBasicLuxEnergy())).setType(GenerationType.SKY)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.LUX_GENERATOR,
        SupremeGenerator.RECIPE_LUX_GENERATOR).setBuffer(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorLuxBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorLuxEnergy())).setType(GenerationType.SKY)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.BASIC_LUMIUM_GENERATOR,
        SupremeGenerator.RECIPE_BASIC_LUMIUM_GENERATOR).setBuffer(
            getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorBasicLumiumBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorBasicLumiumEnergy())).setType(GenerationType.DARK)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.LUMIUM_GENERATOR,
        SupremeGenerator.RECIPE_LUMIUM_GENERATOR).setBuffer(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorLumiumBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorLumiumEnergy())).setType(GenerationType.DARK)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.THORNIUM_GENERATOR,
        SupremeGenerator.RECIPE_THORNIUM_GENERATOR).setBuffer(
            getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorThorniumBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorThorniumEnergy())).setType(GenerationType.EVERY)
        .register(sup);

    new EnergyGenerator(ItemGroups.ELECTRIC_CATEGORY, SupremeGenerator.SUPREME_GENERATOR,
        SupremeGenerator.RECIPE_SUPREME_GENERATOR).setBuffer(
            getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorSupremeBuffer()))
        .setEnergy(getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getGeneratorSupremeEnergy())).setType(GenerationType.EVERY)
        .register(sup);

    new EnergyCapacitor(ItemGroups.ELECTRIC_CATEGORY, SupremeCapacitor.AURUM_CAPACITOR,
        SupremeCapacitor.MACHINE_AURUM_CAPACITOR,
        getCapacitorCapacity(Supreme.getSupremePowerSection().getCapacitorAurumCapacity())).register(sup);

    new EnergyCapacitor(ItemGroups.ELECTRIC_CATEGORY, SupremeCapacitor.TITANIUM_CAPACITOR,
        SupremeCapacitor.MACHINE_TITANIUM_CAPACITOR,
        getCapacitorCapacity(Supreme.getSupremePowerSection().getCapacitorTitaniumCapacity())).register(sup);

    new EnergyCapacitor(ItemGroups.ELECTRIC_CATEGORY, SupremeCapacitor.ADAMANTIUM_CAPACITOR,
        SupremeCapacitor.MACHINE_ADAMANTIUM_CAPACITOR,
        getCapacitorCapacity(Supreme.getSupremePowerSection().getCapacitorAdamantiumCapacity())).register(sup);

    new EnergyCapacitor(ItemGroups.ELECTRIC_CATEGORY, SupremeCapacitor.THORNIUM_CAPACITOR,
        SupremeCapacitor.MACHINE_THORNIUM_CAPACITOR,
        getCapacitorCapacity(Supreme.getSupremePowerSection().getCapacitorThorniumCapacity())).register(sup);

    new EnergyCapacitor(ItemGroups.ELECTRIC_CATEGORY, SupremeCapacitor.SUPREME_CAPACITOR,
        SupremeCapacitor.MACHINE_SUPREME_CAPACITOR,
        getCapacitorCapacity(Supreme.getSupremePowerSection().getCapacitorSupremeCapacity())).register(sup);

  }
}
