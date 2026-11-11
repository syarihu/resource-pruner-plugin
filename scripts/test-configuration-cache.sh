#!/bin/bash
set -euo pipefail

echo "=== Test: Configuration Cache should work correctly ==="

# Matches the problem summary Gradle prints when problems are reported as warnings
# (e.g. "1 problem was found storing the configuration cache.")
CC_PROBLEM_PATTERN="problems? (was|were) found (storing|reusing) the configuration cache|Configuration cache problems found"

# Clean first
./gradlew clean

# First run - should store cache
./gradlew :example:pruneResourcesPreviewDebug 2>&1 | tee /tmp/first_run.log

if grep -Eq "$CC_PROBLEM_PATTERN" /tmp/first_run.log; then
  echo "ERROR: Configuration cache problems found in first run"
  exit 1
fi

if grep -q "Configuration cache entry stored" /tmp/first_run.log; then
  echo "SUCCESS: Configuration cache entry stored"
else
  echo "ERROR: Configuration cache entry not stored"
  exit 1
fi

# Second run - should reuse cache
./gradlew :example:pruneResourcesPreviewDebug 2>&1 | tee /tmp/second_run.log

if grep -Eq "$CC_PROBLEM_PATTERN" /tmp/second_run.log; then
  echo "ERROR: Configuration cache problems found in second run"
  exit 1
fi

if grep -q "Configuration cache entry reused" /tmp/second_run.log; then
  echo "SUCCESS: Configuration cache entry reused"
else
  echo "ERROR: Configuration cache entry not reused"
  exit 1
fi
