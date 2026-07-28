# Part 4 validation

Run before publishing:

```bash
python3 scripts/verify_english.py .
python3 scripts/verify_chunk_load_threading.py .
python3 scripts/check_api_annotations.py
python3 scripts/verify_part2.py .
python3 scripts/verify_part3.py .
python3 scripts/verify_part4.py
./gradlew spotlessApply --no-daemon
./gradlew spotlessCheck clean build --no-daemon
```

Runtime validation on a copied Paper server:

- Die with Soulbound items with `keepInventory` both enabled and disabled.
- Glide into a wall and confirm Elytra Cap protection survives the one-tick stop transition.
- Fire Slimefun bows across chunk and region boundaries.
- Test Vanilla Auto-Crafters with limited crafting enabled and disabled.
- Brew normal, extended, strong, splash, and lingering potions.
- Run `/sf versions` and `/sf timings` before and after an empty profiling interval.

Folia preview validation:

- Use a separate copied server and database.
- Confirm every addon declares and implements Folia support.
- Exercise Cargo, energy, machine tickers, backpacks, guide menus, teleporters, Soulbound items, bows, and Item Doctor across distant loaded regions.
- Treat cross-region-access warnings as release blockers.
