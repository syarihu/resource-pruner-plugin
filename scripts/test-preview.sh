#!/bin/bash

echo "=== Test: pruneResourcesPreview should detect unused resources correctly ==="

# Run pruneResourcesPreview on example module
# Capture output and exit code separately to ensure output is always displayed
OUTPUT=$(./gradlew :example:pruneResourcesPreviewDebug --no-configuration-cache 2>&1) || GRADLE_EXIT_CODE=$?

echo "$OUTPUT"

if [ -n "$GRADLE_EXIT_CODE" ] && [ "$GRADLE_EXIT_CODE" -ne 0 ]; then
  echo "FAILURE: Gradle command failed with exit code $GRADLE_EXIT_CODE"
  exit 1
fi

# Verify that unused resources are detected
if echo "$OUTPUT" | grep -q "Resources to prune:"; then
  echo "SUCCESS: pruneResourcesPreview detected unused resources"
else
  echo "FAILURE: pruneResourcesPreview did not detect unused resources"
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

echo "SUCCESS: All preview tests passed"
