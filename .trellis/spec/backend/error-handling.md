# Error Handling

> How errors are handled in this project.

---

## Overview

<!--
Document your project's error handling conventions here.

Questions to answer:
- What error types do you define?
- How are errors propagated?
- How are errors logged?
- How are errors returned to clients?
-->

(To be filled by the team)

---

## Error Types

<!-- Custom error classes/types -->

(To be filled by the team)

---

## Error Handling Patterns

### Pattern: Async guard must be atomic and released on ALL completion paths

**Problem**: Listener/Service code that guards a player/action with a state set (e.g. `Set<UUID> openingPlayers`) and then runs async work (`CompletableFuture` + main-thread executor) can leak the guard or accept duplicate requests:

1. `set.add(uuid)` without checking the return value → duplicate requests start parallel async loads (root cause of the backpack item-duplication exploit, see task `07-17-backpack-open-race`).
2. Releasing the guard only in `thenAccept` → on exceptional completion the callback never runs and the player is permanently locked out.

**Solution**:

```java
// Wrong
openingPlayers.add(uuid);                       // duplicate requests pass through
future.thenAcceptAsync(result -> {
    openingPlayers.remove(uuid);                // never runs on exceptional completion
    ...
}, mainThreadExecutor);

// Correct
if (!openingPlayers.add(uuid)) {
    return;                                     // atomic reject: a load is already pending
}
future.whenCompleteAsync((result, ex) -> {
    openingPlayers.remove(uuid);                // FIRST statement: success, null and
    if (ex != null) {                           // exceptional paths all release the guard
        Slimefun.logger().log(Level.SEVERE, "...", ex);
        return;
    }
    ...
}, mainThreadExecutor);
```

**Why**: `HashSet.add` returns `false` when the element already exists — that is the cheapest atomic "is a request already pending" check on the main thread. `whenComplete` fires for normal, null-result and exceptional completion, so cleanup in its first statement cannot be skipped.

**Related**: When the same player already has an open view of the resource, reject the new request instead of force-closing and reopening — reopening can swap to a stale duplicate instance and duplicate items (see `BackpackListener.openBackpackInternal`).

---

## API Error Responses

<!-- Standard error response format -->

(To be filled by the team)

---

## Common Mistakes

<!-- Error handling mistakes your team has made -->

(To be filled by the team)
