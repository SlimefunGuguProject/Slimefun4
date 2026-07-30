# Folia Phase 1 Validation Report

## Scope

This report covers the first conservative Folia-support implementation for Slimefun Legacy. It validates source structure and repository policy checks, but it does not replace a full Gradle build or live Folia staging test.

## Passed checks

The following repository-provided checks completed successfully:

```text
python3 scripts/verify_english.py .
python3 scripts/verify_chunk_load_threading.py .
python3 scripts/check_api_annotations.py
python3 scripts/verify_part2.py .
python3 scripts/verify_part3.py .
python3 scripts/verify_part4.py
python3 scripts/verify_folia_phase1.py .
git diff --check
```

Validated areas include:

- player-facing English text policy;
- chunk-load threading rules;
- compatibility-protected API annotations;
- prior Part 2, Part 3, and Part 4 maintenance requirements;
- Folia scheduler routing and ownership checks;
- region-owned machine ticker dispatch;
- concurrent Cargo, energy, and ticker-cycle state;
- no whitespace errors in the patch.

A JUnit 5 regression test was also added for concurrent `BlockTicker#update()` calls. It verifies that `uniqueTick()` executes once per cycle even when the same ticker instance is reached concurrently by multiple Folia regions.

## Full build limitation in this workspace

`./gradlew test --no-daemon` was attempted. The Gradle Wrapper could not download Gradle 9.4.1 because this isolated workspace could not resolve `services.gradle.org`:

```text
java.net.UnknownHostException: services.gradle.org
```

Therefore, this package must still be compiled by GitHub Actions or another environment with dependency access before it is installed on a server. No claim is made that the full Gradle test suite ran here.

## Required staging validation

Before production use:

1. Run the normal GitHub Actions build and test workflows.
2. Start a copied server and world on the targeted Folia build.
3. Test Slimefun Legacy without addons first.
4. Add machine, storage, Cargo, and energy addons one at a time.
5. Exercise machines in distant regions while players open menus and move between regions.
6. Confirm cross-region Cargo and energy nodes defer safely rather than transferring.
7. Watch for Folia ownership exceptions, duplicate cycles, item loss, charge inconsistencies, and shutdown errors.
8. Restart repeatedly and verify block storage, backpacks, machine inventories, and network state.

Folia support remains experimental after Phase 1.
