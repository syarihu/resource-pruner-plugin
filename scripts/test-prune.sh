#!/bin/bash

echo "=== Test: pruneResources should remove unused resources ==="

# First, copy the example resources to a temp location for restoration
TEMP_DIR=$(mktemp -d)
cp -r example/src/main/res "$TEMP_DIR/res_backup"

# Run pruneResources on example module
# Capture output and exit code separately to ensure output is always displayed
OUTPUT=$(./gradlew :example:pruneResourcesDebug --no-configuration-cache 2>&1) || GRADLE_EXIT_CODE=$?

echo "$OUTPUT"

if [ -n "$GRADLE_EXIT_CODE" ] && [ "$GRADLE_EXIT_CODE" -ne 0 ]; then
  echo "FAILURE: Gradle command failed with exit code $GRADLE_EXIT_CODE"
  # Restore resources
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
fi

# Verify that resources were pruned
if echo "$OUTPUT" | grep -q "Resources pruned:"; then
  echo "SUCCESS: pruneResources removed unused resources"
else
  echo "FAILURE: pruneResources did not remove any resources"
  # Restore resources
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
fi

# Verify specific files were removed
if [ -f "example/src/main/res/drawable/ic_unused_icon.xml" ]; then
  echo "FAILURE: ic_unused_icon.xml was not removed"
  # Restore resources
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
else
  echo "SUCCESS: ic_unused_icon.xml was removed"
fi

# Verify that used resources are still present
if [ -f "example/src/main/res/drawable/ic_used_icon.xml" ]; then
  echo "SUCCESS: ic_used_icon.xml was preserved"
else
  echo "FAILURE: ic_used_icon.xml was incorrectly removed"
  # Restore resources
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
fi

# Restore resources for future tests
rm -rf example/src/main/res
cp -r "$TEMP_DIR/res_backup" example/src/main/res
rm -rf "$TEMP_DIR"

echo "SUCCESS: All prune tests passed"
