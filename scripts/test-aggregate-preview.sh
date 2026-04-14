#!/bin/bash

echo "=== Test: pruneResourcesPreview (aggregate) should detect unused resources without removing ==="

# Run aggregate pruneResourcesPreview on example module
# Capture output and exit code separately to ensure output is always displayed
OUTPUT=$(./gradlew :example:pruneResourcesPreview --no-configuration-cache 2>&1)
GRADLE_EXIT_CODE=$?

echo "$OUTPUT"

if [ "$GRADLE_EXIT_CODE" -ne 0 ]; then
  echo "FAILURE: Gradle command failed with exit code $GRADLE_EXIT_CODE"
  exit 1
fi

# Verify that aggregate preview loaded detection results
if echo "$OUTPUT" | grep -q "Resources to prune:"; then
  echo "SUCCESS: Aggregate preview detected unused resources"
else
  echo "FAILURE: Aggregate preview did not report results"
  exit 1
fi

# Verify specific unused resources are detected
if echo "$OUTPUT" | grep -q "ic_unused_icon"; then
  echo "SUCCESS: ic_unused_icon was detected as unused"
else
  echo "FAILURE: ic_unused_icon was not detected as unused"
  exit 1
fi

# Verify that no files were actually deleted
if [ -f "example/src/main/res/drawable/ic_unused_icon.xml" ]; then
  echo "SUCCESS: ic_unused_icon.xml was preserved (preview mode)"
else
  echo "FAILURE: ic_unused_icon.xml was deleted in preview mode"
  exit 1
fi

if [ -f "example/src/main/res/layout/layout_unused.xml" ]; then
  echo "SUCCESS: layout_unused.xml was preserved (preview mode)"
else
  echo "FAILURE: layout_unused.xml was deleted in preview mode"
  exit 1
fi

echo "SUCCESS: All aggregate preview tests passed"
