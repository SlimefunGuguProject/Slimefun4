#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"

if grep -R "LoreBuilder\.powerPerSecond" -n "$ROOT/src/main/java"; then
  echo "Machine consumption lore must use J/tick." >&2
  exit 1
fi

if grep -R "getValueGeneratorsWithLimit(Supreme.getSupremePowerSection().getCapacitor" -n "$ROOT/src/main/java"; then
  echo "Capacitors must not use the generator production limiter." >&2
  exit 1
fi

grep -q 'capacitorThorniumCapacity(100000000)' "$ROOT/src/main/java/com/github/relativobr/supreme/util/SupremePowerSection.java"
grep -q 'capacitorSupremeCapacity(1600000000)' "$ROOT/src/main/java/com/github/relativobr/supreme/util/SupremePowerSection.java"
grep -q 'flow == ItemTransportFlow.WITHDRAW ? getOutputSlots() : getInputSlots()' "$ROOT/src/main/java/com/github/relativobr/supreme/generic/machine/GenericMachine.java"

echo "Supreme invariants verified."
