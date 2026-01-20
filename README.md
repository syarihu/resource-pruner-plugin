# Resource Pruner Plugin

<img alt="license-scribe-plugin-logo" src="docs/images/resource-pruner-plugin-logo.png" />

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Gradle plugin that acts as your project's gardener, carefully pruning unused resources from your Android codebase to keep it clean and maintainable.

## Features

- **Accurate Detection**: Detects resource usage through multiple patterns:
  - Direct R class references (`R.drawable.icon`, `R.string.app_name`)
  - XML resource references (`@string/app_name`, `@drawable/icon`)
  - ViewBinding usage (`ActivityMainBinding`, `FragmentHomeBinding`)
  - Paraphrase library support (`FormattedResources.greeting_format()`)
  - AndroidManifest.xml references (`@style/Theme.App`)

- **Smart Generated Code Handling**: Unlike simple grep-based tools, this plugin correctly handles code generation:
  - ViewBinding: Only detects layouts that are actually used via binding classes
  - Paraphrase: Only detects ICU-formatted strings where `FormattedResources.xxx()` is actually called

- **Safe Operation**:
  - Analyze mode to preview changes before pruning
  - Configurable exclusion patterns for resources that should never be removed
  - Per-variant tasks for fine-grained control

- **Multi-Module Support**: Library resources used by dependent app modules are correctly detected as used, preventing false positive pruning

## Requirements

- Android Gradle Plugin 8.0+
- Gradle 8.0+
- Java 17+

## Installation

Add the plugin to your Android module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") // or com.android.library
    id("net.syarihu.resource-pruner") version "0.1.0"
}
```

## Usage

### Analyze Resources

Preview which resources would be removed without actually deleting them:

```bash
./gradlew analyzeResourcesDebug
# or for release variant
./gradlew analyzeResourcesRelease
```

Example output:
```
=== Analysis Results ===
Total resources: 24
Resources to preserve: 12
Resources to prune: 12

Unused resources:
  drawable: 1
    - ic_unused_icon
  layout: 1
    - layout_unused
  string: 4
    - unused_string
    - another_unused
    - deprecated_message
    - unused_paraphrase
```

### Prune Resources

Remove unused resources from your project:

```bash
./gradlew pruneResourcesDebug
# or for release variant
./gradlew pruneResourcesRelease
```

> **Warning**: This operation modifies your source files. Make sure to commit your changes before running, or use `analyzeResources` first to preview.

## Configuration

Configure the plugin in your module's `build.gradle.kts`:

```kotlin
resourcePruner {
    // Exclude resources matching these regex patterns from pruning
    excludeNames.addAll(
        "^ic_launcher.*",     // Preserve launcher icons
        "^google_play_.*",    // Preserve Google Play assets
        "^app_name$",         // Preserve app name
    )

    // Optional: Only target specific resource types
    targetResourceTypes.addAll("drawable", "string", "layout")

    // Optional: Specify which source sets to scan
    sourceSets.addAll("main", "debug")
}
```

### Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `excludeNames` | `List<String>` | Regex patterns for resource names to exclude from pruning |
| `targetResourceTypes` | `Set<String>` | Resource types to target (empty = all types) |
| `sourceSets` | `Set<String>` | Source sets to scan for usage (default: `["main"]`) |
| `scanDependentProjects` | `Boolean` | Scan dependent projects for resource usage (default: `true`) |

### Supported Resource Types

- `drawable`, `mipmap` - Image resources
- `layout` - Layout XML files
- `menu` - Menu XML files
- `string`, `plurals` - String resources
- `color` - Color resources
- `dimen` - Dimension resources
- `style` - Style resources
- `animator`, `anim` - Animation resources
- `bool`, `integer`, `array` - Value resources
- `attr` - Attribute resources

## How It Works

### Detection Pipeline

1. **Resource Collection**: Scans `res/` directories to find all defined resources
2. **Usage Detection**: Analyzes source code and XML files for resource references
3. **Smart Filtering**:
   - Skips ViewBinding generated code (detects actual binding class usage instead)
   - Skips Paraphrase generated code (detects actual `FormattedResources.xxx()` calls instead)
4. **Exclusion Matching**: Applies configured exclusion patterns
5. **Analysis/Pruning**: Reports or removes unused resources

### ViewBinding Support

The plugin correctly handles ViewBinding-only layouts:

```kotlin
// This layout is detected as used even without R.layout.xxx reference
class ItemViewHolder(
    private val binding: ItemViewbindingOnlyBinding
) : RecyclerView.ViewHolder(binding.root)
```

### Paraphrase Support

The plugin correctly handles [Paraphrase](https://github.com/cashapp/paraphrase) ICU-formatted strings:

```xml
<!-- strings.xml -->
<string name="greeting_format">Hello, {name}!</string>
<string name="unused_format">Unused: {value}</string>
```

```kotlin
// Only greeting_format is detected as used
val greeting = getString(FormattedResources.greeting_format(name = "World"))
// unused_format will be marked for pruning since FormattedResources.unused_format() is never called
```

### Multi-Module Support

The plugin automatically detects resource usage across module boundaries. When analyzing a library module, it scans the source code of all projects that depend on it.

```
project-root/
├── app/                    # Uses resources from common-ui
│   └── build.gradle.kts    # implementation(project(":common-ui"))
└── common-ui/              # Library with shared resources
    └── build.gradle.kts    # Has resource-pruner plugin
```

When you run `./gradlew :common-ui:analyzeResourcesDebug`, the plugin will:
1. Find all projects that depend on `common-ui` (in this case, `app`)
2. Scan those projects' source code for resource references
3. Correctly identify library resources that are used by dependent modules

This prevents false positives where library resources appear unused because they're only referenced in app modules.

To disable this feature (e.g., for standalone library development):

```kotlin
resourcePruner {
    scanDependentProjects.set(false)
}
```

## Example Project

See the `example/` and `example-lib/` directories for a complete example demonstrating:
- ViewBinding detection (including ViewBinding-only layouts)
- Paraphrase integration
- Multi-module resource usage detection
- Various unused resources that get pruned

## License

```
Copyright 2026 syarihu

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
