.PHONY: publish build clean test analyze prune help

# Publish to Maven Local (core first, then plugin)
publish:
	./gradlew :resource-pruner-core:publishToMavenLocal :resource-pruner-gradle-plugin:publishToMavenLocal -PexcludeExample --no-configuration-cache

# Build all modules (requires publish first for example)
build:
	./gradlew build --no-configuration-cache

# Build without example
build-core:
	./gradlew :resource-pruner-core:build :resource-pruner-gradle-plugin:build -PexcludeExample --no-configuration-cache

# Publish and build
all: publish build

# Run all tests
test:
	./gradlew :resource-pruner-core:test :resource-pruner-gradle-plugin:test -PexcludeExample --no-configuration-cache

# Analyze resources in example project (dry-run style, shows what would be pruned)
analyze:
	./gradlew :example:analyzeResourcesDebug --no-configuration-cache

# Actually prune resources in example project
prune:
	./gradlew :example:pruneResourcesDebug --no-configuration-cache

# Clean build artifacts
clean:
	./gradlew clean -PexcludeExample

# Run spotless formatting
format:
	./gradlew spotlessApply -PexcludeExample

# Check formatting
check:
	./gradlew spotlessCheck -PexcludeExample --no-configuration-cache

# Full test: publish, build example, analyze, then prune
test-prune: publish
	./gradlew :example:analyzeResourcesDebug --no-configuration-cache
	@echo ""
	@echo "=== Now running prune ==="
	@echo ""
	./gradlew :example:pruneResourcesDebug --no-configuration-cache

# Reset example resources (restore from git)
reset-example:
	git checkout -- example/src/main/res/

help:
	@echo "Available targets:"
	@echo "  make publish      - Publish all modules to Maven Local"
	@echo "  make build        - Build all modules (requires publish first)"
	@echo "  make build-core   - Build core modules only (no example)"
	@echo "  make all          - Publish and build"
	@echo "  make test         - Run all unit tests"
	@echo "  make analyze      - Analyze resources in example (show unused)"
	@echo "  make prune        - Prune unused resources in example"
	@echo "  make test-prune   - Full test: publish, analyze, and prune"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make format       - Apply spotless formatting"
	@echo "  make check        - Check formatting"
	@echo "  make reset-example - Reset example resources from git"
