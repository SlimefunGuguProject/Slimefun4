# Slimefun Legacy — Optional AdvancedEnchantments Compatibility

## Included machines

This patch updates the shared Slimefun enchantment machine implementation used by:

- Auto Enchanter I
- Auto Enchanter II
- Auto Disenchanter I
- Auto Disenchanter II
- InfinityExpansion2 Advanced Enchanter and Advanced Disenchanter
- InfinityExpansion2 Infinity Enchanter and Infinity Disenchanter

The InfinityExpansion2 tiers inherit Slimefun's core `AutoEnchanter` and `AutoDisenchanter` processing, so no InfinityExpansion2 source modification is needed for the current addon implementation.

## Optional integration

AdvancedEnchantments is not required:

- No direct AdvancedEnchantments imports are compiled into Slimefun.
- No `depend` or `softdepend` entry is added to `plugin.yml`.
- Slimefun checks for an enabled `AdvancedEnchantments` plugin after server startup.
- AEAPI is accessed through a guarded runtime bridge.
- Without AdvancedEnchantments, the original vanilla enchantment behavior remains active.
- API failures leave machine inputs untouched and log only the first compatibility warning.

## Machine behavior

### Enchanters

- Continue accepting vanilla enchanted books.
- Also accept standard AdvancedEnchantments books using the current `ae_book` item-data format.
- Apply custom enchantments through AEAPI and use the `ItemStack` returned by AEAPI.
- Preserve existing Slimefun enchant-count, enchant-level, ignored-lore, and override-level settings.
- Support books containing both vanilla stored enchantments and AE book metadata.

AE book success and destroy percentages are preserved as metadata when read, but automated application is deterministic. The machine applies the enchant through AEAPI rather than rolling the manual drag-and-drop book chances.

### Disenchanters

- Detect current AdvancedEnchantments item data (`ae_enchantment-*`).
- Remove custom enchantments through AEAPI when available.
- Verify removal before consuming either input.
- Produce an AE-compatible enchanted book with 100% success and 0% destroy chance.
- Preserve all remaining custom and vanilla enchantments on the output item.

Only one AE custom enchantment is extracted per operation. AE books are individually identified and non-stackable, while the machine has two output slots. Run the cleaned item through again with another plain book to extract the next custom enchantment. Once no AE enchantments remain, the normal Slimefun operation extracts the vanilla enchantments into one vanilla enchanted book.

## Paper event-thread fix

The patch also includes the required event-thread repair for:

- `AutoEnchantEvent`
- `AutoDisenchantEvent`
- `AsyncAutoEnchanterProcessEvent`

These events now mark themselves synchronous or asynchronous according to the thread that constructs them. This prevents Paper from rejecting the events when a viewed machine inventory causes its ticker to execute on the primary server thread.

## Files changed

1. `src/main/java/io/github/thebusybiscuit/slimefun4/api/events/AsyncAutoEnchanterProcessEvent.java`
2. `src/main/java/io/github/thebusybiscuit/slimefun4/api/events/AutoDisenchantEvent.java`
3. `src/main/java/io/github/thebusybiscuit/slimefun4/api/events/AutoEnchantEvent.java`
4. `src/main/java/io/github/thebusybiscuit/slimefun4/api/events/EventThreading.java` (new)
5. `src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/electric/machines/enchanting/AutoDisenchanter.java`
6. `src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/electric/machines/enchanting/AutoEnchanter.java`
7. `src/main/java/io/github/thebusybiscuit/slimefun4/integrations/AdvancedEnchantmentsIntegration.java` (new)
8. `src/main/java/io/github/thebusybiscuit/slimefun4/integrations/IntegrationsManager.java`

## Validation performed

- Java 21 AdvancedEnchantments bridge harness: passed.
- Java 21 compilation of the actual modified Auto Enchanter and Auto Disenchanter classes against API-compatible test stubs: passed.
- Dynamic event-thread harness for primary-thread, worker-thread, and server-unavailable construction: passed.
- 120-column, LF-ending, and trailing-whitespace checks on all changed files: passed.
- Optional-dependency check for direct AE imports and `plugin.yml` dependency entries: passed.

A full Gradle build could not be executed in the packaging environment because the checked-in wrapper requires Gradle 9.4.1 and the environment could not reach `services.gradle.org`. Run `./gradlew clean build` in GitHub Actions or another network-enabled development environment.

## Installation

### Changed-files archive

Extract it over the root of the same Slimefun Legacy source revision and allow the eight source files to be added/replaced.

### Full patched source

Build normally:

```bash
./gradlew clean build
```

The plugin JAR will be produced under `build/libs/`.
