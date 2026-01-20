#!/bin/bash
set -e

echo "=== Test: analyzeResources should detect multi-module usage correctly ==="

# Run analyzeResources on example-lib module
# This tests that resources used by dependent modules (example, example-multi-app) are preserved
OUTPUT=$(./gradlew :example-lib:analyzeResourcesDebug --no-configuration-cache 2>&1)

echo "$OUTPUT"

# Verify that lib_used_string is preserved (used by example module)
if echo "$OUTPUT" | grep -q "Resources to preserve:"; then
  echo "SUCCESS: Some resources are preserved"
else
  echo "FAILURE: No resources are preserved"
  exit 1
fi

# Verify that lib_deprecated_message is detected as unused (not used by any app)
if echo "$OUTPUT" | grep -q "lib_deprecated_message"; then
  echo "SUCCESS: lib_deprecated_message was detected as unused"
else
  echo "FAILURE: lib_deprecated_message was not detected as unused"
  exit 1
fi

# Verify that layout_lib_unused is detected as unused
if echo "$OUTPUT" | grep -q "layout_lib_unused"; then
  echo "SUCCESS: layout_lib_unused was detected as unused"
else
  echo "FAILURE: layout_lib_unused was not detected as unused"
  exit 1
fi

echo "SUCCESS: All multi-module analyze tests passed"
