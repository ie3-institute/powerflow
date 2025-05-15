#!/usr/bin/env bash
set -euo pipefail

if [ -z "${BRANCH_NAME:-}" ]; then
  echo "Error: BRANCH_NAME variable is not set."
  exit 1
fi


pattern_dev='^(developer|develop|dev)$'
pattern_release='.*rel/.*'
pattern_dependabot='^dependabot/.*'
pattern_hotfix='.*hotfix/.*'
pattern_main='.*main'
pattern_feature='^[a-z]{2}/#[0-9]+(-.+)?$'

BRANCH_TYPE="unknown"

if [[ "$BRANCH_NAME" =~ $pattern_dev ]]; then
  BRANCH_TYPE="dev"
elif [[ "$BRANCH_NAME" =~ $pattern_release ]]; then
  BRANCH_TYPE="release"
elif [[ "$BRANCH_NAME" =~ $pattern_dependabot ]]; then
  BRANCH_TYPE="dependabot"
elif [[ "$BRANCH_NAME" =~ $pattern_hotfix ]]; then
  BRANCH_TYPE="hotfix"
elif [[ "$BRANCH_NAME" =~ $pattern_main ]]; then
  BRANCH_TYPE="main"
elif [[ "$BRANCH_NAME" =~ $pattern_feature ]]; then
  BRANCH_TYPE="feature"
else
  echo "Error: Check branch name format (e.g., ps/#1337-FeatureName). Current branch name is '$BRANCH_NAME'."
  exit 1
fi

echo "========================="
echo "Branch type: $BRANCH_TYPE"
echo "BRANCH_TYPE=$BRANCH_TYPE" >> "$GITHUB_ENV"
echo "========================="
