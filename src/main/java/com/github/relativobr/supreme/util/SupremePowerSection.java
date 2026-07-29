package com.github.relativobr.supreme.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class SupremePowerSection {

  int capacitorAurumCapacity;
  int capacitorTitaniumCapacity;
  int capacitorAdamantiumCapacity;
  int capacitorThorniumCapacity;
  int capacitorSupremeCapacity;
  int generatorBasicIgnisEnergy;
  int generatorBasicIgnisBuffer;
  int generatorBasicVentusEnergy;
  int generatorBasicVentusBuffer;
  int generatorBasicAquaEnergy;
  int generatorBasicAquaBuffer;
  int generatorBasicLuxEnergy;
  int generatorBasicLuxBuffer;
  int generatorBasicLumiumEnergy;
  int generatorBasicLumiumBuffer;
  int generatorIgnisEnergy;
  int generatorIgnisBuffer;
  int generatorVentusEnergy;
  int generatorVentusBuffer;
  int generatorAquaEnergy;
  int generatorAquaBuffer;
  int generatorLuxEnergy;
  int generatorLuxBuffer;
  int generatorLumiumEnergy;
  int generatorLumiumBuffer;
  int generatorThorniumEnergy;
  int generatorThorniumBuffer;
  int generatorSupremeEnergy;
  int generatorSupremeBuffer;

  public static SupremePowerSection defaultValue() {
    return SupremePowerSection.builder()
            .capacitorAurumCapacity(1000000)
            .capacitorTitaniumCapacity(4000000)
            .capacitorAdamantiumCapacity(16000000)
            .capacitorThorniumCapacity(100000000)
            .capacitorSupremeCapacity(1600000000)
            .generatorBasicIgnisEnergy(2500)
            .generatorBasicIgnisBuffer(5000)
            .generatorBasicVentusEnergy(2500)
            .generatorBasicVentusBuffer(5000)
            .generatorBasicAquaEnergy(2500)
            .generatorBasicAquaBuffer(5000)
            .generatorBasicLuxEnergy(2500)
            .generatorBasicLuxBuffer(5000)
            .generatorBasicLumiumEnergy(5000)
            .generatorBasicLumiumBuffer(10000)
            .generatorIgnisEnergy(25000)
            .generatorIgnisBuffer(50000)
            .generatorVentusEnergy(25000)
            .generatorVentusBuffer(50000)
            .generatorAquaEnergy(25000)
            .generatorAquaBuffer(50000)
            .generatorLuxEnergy(25000)
            .generatorLuxBuffer(50000)
            .generatorLumiumEnergy(75000)
            .generatorLumiumBuffer(500000)
            .generatorThorniumEnergy(1000000)
            .generatorThorniumBuffer(6000000)
            .generatorSupremeEnergy(2000000)
            .generatorSupremeBuffer(12000000)
            .build();
  }
}
