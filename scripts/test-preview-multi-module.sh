#!/bin/bash

echo "=== Test: pruneResourcesPreview should detect multi-module usage correctly ==="

# Run pruneResourcesPreview on example-lib module
# This also covers #57: configuring this task must not realize tasks of dependent apps.
# example-multi-app enables Compose (AGP may throw while creating a Compose task), and
# -PresourcePruner.assertNoEagerRealization makes it fail if any guarded task gets realized,
# so the test does not depend on AGP throwing.
# This tests that resources used by dependent modules (example, example-multi-app) are preserved
# Capture output and exit code separately to ensure output is always displayed
OUTPUT=$(./gradlew :example-lib:pruneResourcesPreviewDebug -PresourcePruner.assertNoEagerRealization --no-configuration-cache 2>&1) || GRADLE_EXIT_CODE=$?

echo "$OUTPUT"

if [ -n "$GRADLE_EXIT_CODE" ] && [ "$GRADLE_EXIT_CODE" -ne 0 ]; then
  echo "FAILURE: Gradle command failed with exit code $GRADLE_EXIT_CODE"
  if echo "$OUTPUT" | grep -q "realized eagerly"; then
    echo "The plugin realized tasks of a dependent project while configuring (see #57)"
  fi
  exit 1
fi

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

echo "SUCCESS: All multi-module preview tests passed"
