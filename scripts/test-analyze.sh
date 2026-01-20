#!/bin/bash
set -e

echo "=== Test: analyzeResources should detect unused resources correctly ==="

# Run analyzeResources on example module
OUTPUT=$(./gradlew :example:analyzeResourcesDebug --no-configuration-cache 2>&1)

echo "$OUTPUT"

# Verify that unused resources are detected
if echo "$OUTPUT" | grep -q "Resources to prune:"; then
  echo "SUCCESS: analyzeResources detected unused resources"
else
  echo "FAILURE: analyzeResources did not detect unused resources"
  exit 1
fi

# Verify specific unused resources are detected
if echo "$OUTPUT" | grep -q "unused_string"; then
  echo "SUCCESS: unused_string was detected as unused"
else
  echo "FAILURE: unused_string was not detected as unused"
  exit 1
fi

if echo "$OUTPUT" | grep -q "ic_unused_icon"; then
  echo "SUCCESS: ic_unused_icon was detected as unused"
else
  echo "FAILURE: ic_unused_icon was not detected as unused"
  exit 1
fi

echo "SUCCESS: All analyze tests passed"
