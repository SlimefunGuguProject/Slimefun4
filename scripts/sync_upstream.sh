#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM_REPOSITORY="${GUGU_UPSTREAM_REPOSITORY:-https://github.com/SlimefunGuguProject/Slimefun4.git}"
UPSTREAM_REF="${1:-master}"
SYNC_BRANCH="${2:-automation/gugu-upstream-sync}"
ABORT_ON_CONFLICT="${GUGU_SYNC_ABORT_ON_CONFLICT:-0}"
RUN_BUILD="${GUGU_SYNC_BUILD:-0}"

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

REPORT_PATH="${GUGU_SYNC_REPORT:-$ROOT_DIR/gugu-sync-report.txt}"
{
  echo "Gugu upstream sync report"
  echo "Generated: $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  echo "Base branch: $BASE_BRANCH"
  echo "Base commit: $BASE_SHA"
  echo "Upstream ref: $UPSTREAM_REF"
  echo "Upstream commit: $UPSTREAM_SHA"
  echo
  echo "Pending commits:"
  git log --reverse --date=short --format='- %h %ad %s' "${BASE_SHA}..${UPSTREAM_SHA}" || true
  echo
  echo "Changed files:"
  git diff --name-status "${BASE_SHA}...${UPSTREAM_SHA}" || true
} > "$REPORT_PATH"

# Start from the exact fork revision. A real Git merge preserves local commits and
# allows Git to identify conflicts; unlike rsync, it cannot silently delete fork work.
git switch -C "$SYNC_BRANCH" "$BASE_SHA"

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
  fi
  exit 3
fi

python3 scripts/verify_english.py .
python3 scripts/verify_part2.py .
python3 scripts/check_api_annotations.py .

if [[ "$RUN_BUILD" == "1" ]]; then
  chmod +x gradlew
  ./gradlew spotlessCheck test build --no-daemon
fi

{
  echo
  echo "Result: merge and configured validation completed successfully."
  echo "Review the branch and database migration notes before merging."
} >> "$REPORT_PATH"

echo "Prepared review branch: $SYNC_BRANCH"
echo "Report: $REPORT_PATH"
