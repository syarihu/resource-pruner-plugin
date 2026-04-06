#!/bin/bash

echo "=== Test: pruneResources (aggregate) should remove unused resources across all variants ==="

# First, copy the example resources to a temp location for restoration
TEMP_DIR=$(mktemp -d)
cp -r example/src/main/res "$TEMP_DIR/res_backup"

# Run aggregate pruneResources on example module
# Capture output and exit code separately to ensure output is always displayed
OUTPUT=$(./gradlew :example:pruneResources --no-configuration-cache 2>&1) || GRADLE_EXIT_CODE=$?

echo "$OUTPUT"

if [ -n "$GRADLE_EXIT_CODE" ] && [ "$GRADLE_EXIT_CODE" -ne 0 ]; then
  echo "FAILURE: Gradle command failed with exit code $GRADLE_EXIT_CODE"
  # Restore resources
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
fi

# Verify that aggregate pruning ran
if echo "$OUTPUT" | grep -q "Resources unused across all variants:"; then
  echo "SUCCESS: Aggregate task loaded detection results from all variants"
else
  echo "FAILURE: Aggregate task did not load detection results"
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
fi

if echo "$OUTPUT" | grep -q "Resources pruned:"; then
  echo "SUCCESS: Aggregate pruneResources removed unused resources"
else
  echo "FAILURE: Aggregate pruneResources did not remove any resources"
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
fi

# Verify specific files were removed
if [ -f "example/src/main/res/drawable/ic_unused_icon.xml" ]; then
  echo "FAILURE: ic_unused_icon.xml was not removed"
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
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
fi

# Verify that unused layout was removed
if [ -f "example/src/main/res/layout/layout_unused.xml" ]; then
  echo "FAILURE: layout_unused.xml was not removed"
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
else
  echo "SUCCESS: layout_unused.xml was removed"
fi

# Verify that used ViewBinding layouts are still present
if [ -f "example/src/main/res/layout/activity_main.xml" ]; then
  echo "SUCCESS: activity_main.xml was preserved (used via ViewBinding)"
else
  echo "FAILURE: activity_main.xml was incorrectly removed"
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
fi

if [ -f "example/src/main/res/layout/item_viewbinding_only.xml" ]; then
  echo "SUCCESS: item_viewbinding_only.xml was preserved (used via ViewBinding only)"
else
  echo "FAILURE: item_viewbinding_only.xml was incorrectly removed"
  rm -rf example/src/main/res
  cp -r "$TEMP_DIR/res_backup" example/src/main/res
  rm -rf "$TEMP_DIR"
  exit 1
fi

# Restore resources for future tests
rm -rf example/src/main/res
cp -r "$TEMP_DIR/res_backup" example/src/main/res
rm -rf "$TEMP_DIR"

echo "SUCCESS: All aggregate prune tests passed"
