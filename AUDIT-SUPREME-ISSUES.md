# Supreme Legacy issue and systems audit

Reviewed against the SlimefunGuguProject/Supreme issue tracker and the Supreme Legacy v5 source.

## Patched in this pass

- Issue #36: capacitor storage was incorrectly routed through the 16,000,000 J generator limiter.
  Capacitor capacities now use their configured values, including 100,000,000 J Thornium and
  1,600,000,000 J Supreme capacities.
- Issue #30: machine lore that consumes power every server tick now reports J/tick rather than J/s.
- Cargo/Networks compatibility: the legacy item-transport overload now exposes machine input slots
  for insertion instead of returning no slots. The item-aware overload still provides preferred
  matching/empty-slot ordering.
- Mob generator cache validation now handles removed, invalid, teleported, and cross-world entities
  without duplicate Bukkit lookups or cross-world distance exceptions.

## Previously patched and rechecked

- Issue #37: machines stop when the complete output cannot fit and do not consume a new recipe.
- Issue #32: recipe inputs are committed atomically and reserved inputs are restored on block break.
- Issue #18: MobTech Collector consumes one Empty MobTech per successful operation.
- Issue #17: Mob Collector Tool III stores 50,000 J.
- Issue #16: mutation levels are retained by the tech generator path.

## Still recommended for server testing

- Cargo Nodes and Networks pushing duplicate ingredients into multi-slot recipes.
- Rapid cargo insert/withdraw around the final processing tick.
- Chunk unload/reload during an active machine cycle.
- Server stop or crash during an active machine cycle.
- Multiple identical generators and quarries in adjacent chunks.
- Capacitor charging above 16 MJ on Legacy, Gugu, and United.
