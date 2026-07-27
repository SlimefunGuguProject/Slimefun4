#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM_REPOSITORY="${GUGU_UPSTREAM_REPOSITORY:-https://github.com/SlimefunGuguProject/Slimefun4.git}"
UPSTREAM_REF="${1:-master}"
SYNC_BRANCH="${2:-automation/gugu-upstream-sync}"
ABORT_ON_CONFLICT="${GUGU_SYNC_ABORT_ON_CONFLICT:-0}"
RUN_BUILD="${GUGU_SYNC_BUILD:-0}"
MARKER_FILE="$ROOT_DIR/.gugu-upstream-base"

cd "$ROOT_DIR"

if [[ ! -d .git ]]; then
  echo "This safe sync must run inside a Git checkout, not an extracted source folder." >&2
  echo "Commit the source to GitHub first, then run this script or the Sync Gugu Upstream workflow." >&2
  exit 2
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Refusing to sync with uncommitted changes. Commit or stash them first." >&2
  exit 2
fi

BASE_SHA="$(git rev-parse HEAD)"
BASE_BRANCH="$(git branch --show-current)"
if [[ -z "$BASE_BRANCH" ]]; then
  BASE_BRANCH="detached-${BASE_SHA:0:8}"
fi

INTEGRATED_UPSTREAM_SHA=""
if [[ -f "$MARKER_FILE" ]]; then
  INTEGRATED_UPSTREAM_SHA="$(tr -d '[:space:]' < "$MARKER_FILE")"
  if [[ ! "$INTEGRATED_UPSTREAM_SHA" =~ ^[0-9a-fA-F]{40}$ ]]; then
    echo "Invalid .gugu-upstream-base value: expected one full 40-character commit SHA." >&2
    exit 2
  fi
fi

if git remote get-url upstream >/dev/null 2>&1; then
  git remote set-url upstream "$UPSTREAM_REPOSITORY"
else
  git remote add upstream "$UPSTREAM_REPOSITORY"
fi

git fetch --no-tags upstream "+refs/heads/${UPSTREAM_REF}:refs/remotes/upstream/${UPSTREAM_REF}"
UPSTREAM_SHA="$(git rev-parse "upstream/${UPSTREAM_REF}")"

if git merge-base --is-ancestor "$UPSTREAM_SHA" "$BASE_SHA"; then
  echo "No Gugu updates are pending. ${UPSTREAM_REF} is already contained in ${BASE_SHA}."
  exit 0
fi

RANGE_BASE="$BASE_SHA"
if [[ -n "$INTEGRATED_UPSTREAM_SHA" ]]; then
  if ! git cat-file -e "${INTEGRATED_UPSTREAM_SHA}^{commit}" 2>/dev/null; then
    echo "The commit recorded in .gugu-upstream-base is not available from the selected upstream history." >&2
    echo "Recorded commit: $INTEGRATED_UPSTREAM_SHA" >&2
    exit 2
  fi

  if git merge-base --is-ancestor "$UPSTREAM_SHA" "$INTEGRATED_UPSTREAM_SHA"; then
    echo "No Gugu updates are pending. The recorded integration baseline already includes ${UPSTREAM_SHA}."
    exit 0
  fi

  if ! git merge-base --is-ancestor "$INTEGRATED_UPSTREAM_SHA" "$UPSTREAM_SHA"; then
    echo "The selected upstream branch no longer descends from .gugu-upstream-base." >&2
    echo "Refusing to guess across rewritten or unrelated upstream history." >&2
    echo "Recorded commit: $INTEGRATED_UPSTREAM_SHA" >&2
    echo "Upstream commit: $UPSTREAM_SHA" >&2
    exit 2
  fi

  RANGE_BASE="$INTEGRATED_UPSTREAM_SHA"
fi

REPORT_PATH="${GUGU_SYNC_REPORT:-$ROOT_DIR/gugu-sync-report.txt}"
{
  echo "Gugu upstream sync report"
  echo "Generated: $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  echo "Base branch: $BASE_BRANCH"
  echo "Base commit: $BASE_SHA"
  echo "Recorded upstream baseline: ${INTEGRATED_UPSTREAM_SHA:-none}"
  echo "Upstream ref: $UPSTREAM_REF"
  echo "Upstream commit: $UPSTREAM_SHA"
  echo
  echo "Pending commits after the effective baseline:"
  git log --reverse --date=short --format='- %h %ad %s' "${RANGE_BASE}..${UPSTREAM_SHA}" || true
  echo
  echo "Changed files after the effective baseline:"
  git diff --name-status "${RANGE_BASE}...${UPSTREAM_SHA}" || true
} > "$REPORT_PATH"

# Start from the exact fork revision. If storage or other upstream work was
# integrated manually, first connect the recorded upstream commit using an
# "ours" merge. This records ancestry without replacing any fork source files,
# so the following real merge considers only commits newer than the marker.
git switch -C "$SYNC_BRANCH" "$BASE_SHA"

if [[ -n "$INTEGRATED_UPSTREAM_SHA" ]] && ! git merge-base --is-ancestor "$INTEGRATED_UPSTREAM_SHA" HEAD; then
  git merge --no-ff -s ours -m "chore: record manually integrated Gugu baseline ${INTEGRATED_UPSTREAM_SHA:0:8}" \
    "$INTEGRATED_UPSTREAM_SHA"
fi

set +e
git merge --no-ff --no-edit "upstream/${UPSTREAM_REF}"
MERGE_STATUS=$?
set -e

if [[ $MERGE_STATUS -ne 0 ]]; then
  {
    echo
    echo "Merge conflicts:"
    git diff --name-only --diff-filter=U || true
  } >> "$REPORT_PATH"

  echo "Gugu sync stopped because conflicts require review:" >&2
  git diff --name-only --diff-filter=U >&2 || true

  if [[ "$ABORT_ON_CONFLICT" == "1" ]]; then
    git merge --abort || true
    git reset --hard "$BASE_SHA"
  fi
  exit 3
fi

printf '%s\n' "$UPSTREAM_SHA" > "$MARKER_FILE"
git add "$MARKER_FILE"
if ! git diff --cached --quiet; then
  git commit -m "chore: record Gugu upstream baseline ${UPSTREAM_SHA:0:8}"
fi

python3 scripts/verify_english.py .
python3 scripts/verify_chunk_load_threading.py .
python3 scripts/verify_part2.py .
python3 scripts/verify_part3.py .
python3 scripts/check_api_annotations.py .
python3 scripts/verify_gugu_sync.py .

if [[ "$RUN_BUILD" == "1" ]]; then
  chmod +x gradlew
  ./gradlew spotlessCheck test build --no-daemon
fi

{
  echo
  echo "Result: merge and configured validation completed successfully."
  echo "Recorded upstream baseline: $UPSTREAM_SHA"
  echo "Review the branch and database migration notes before merging."
} >> "$REPORT_PATH"

echo "Prepared review branch: $SYNC_BRANCH"
echo "Report: $REPORT_PATH"
