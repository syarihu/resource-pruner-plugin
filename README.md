# Resource Pruner Plugin

<img alt="license-scribe-plugin-logo" src="docs/images/resource-pruner-plugin-logo.png" />

[![Build and Test](https://github.com/syarihu/resource-pruner-plugin/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/syarihu/resource-pruner-plugin/actions/workflows/build-and-test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/net.syarihu.resourcepruner/resource-pruner-gradle-plugin)](https://central.sonatype.com/namespace/net.syarihu.resourcepruner)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Gradle plugin that acts as your project's gardener, carefully pruning unused resources from your Android codebase to keep it clean and maintainable.

## Features

- **Accurate Detection**: Detects resource usage through multiple patterns:
  - Direct R class references (`R.drawable.icon`, `R.string.app_name`)
  - Import aliases (`import ...R as LibR` → `LibR.drawable.icon`)
  - XML resource references (`@string/app_name`, `@drawable/icon`)
  - Theme attribute references (`?attr/colorPrimary`)
  - Custom View attribute references via R.styleable (`R.styleable.CustomView_customAttr`)
  - Implicit style parent references via dot notation (`TextStyle.Body` inherits from `TextStyle`)
  - ViewBinding usage (`ActivityMainBinding`, `FragmentHomeBinding`)
  - Paraphrase library support (`FormattedResources.greeting_format()`)
  - AndroidManifest.xml references (`@style/Theme.App`)

- **Smart Generated Code Handling**: This plugin is aware of code generation and correctly handles:
  - ViewBinding: Only detects layouts that are actually used via binding classes
  - Paraphrase: Only detects ICU-formatted strings where `FormattedResources.xxx()` is actually called

- **Safe Operation**:
  - Preview mode to see changes before pruning
  - Configurable exclusion patterns for resources that should never be removed
  - Per-variant tasks for fine-grained control

- **Multi-Module Support**: Library resources used by dependent app modules are correctly detected as used, preventing false positive pruning

- **Build Variant Aware**: Detects resources across all source sets for each variant:
  - Base resources (`src/main/res`)
  - Build type specific resources (`src/debug/res`, `src/release/res`)
  - Product flavor specific resources (`src/staging/res`, etc.)

## Requirements

- Android Gradle Plugin 8.0+
- Gradle 8.0+
- Java 17+

## Installation

Add the following to your project:

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}
```

### build.gradle.kts (app or library module)

```kotlin
plugins {
    id("com.android.application") // or com.android.library
    id("net.syarihu.resource-pruner") version "<latest-version>"
}
```

### For Local Development

#### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        mavenLocal()  // For local development
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}
```

#### build.gradle.kts (app or library module)

```kotlin
plugins {
    id("com.android.application") // or com.android.library
    id("net.syarihu.resource-pruner") version "<latest-version>-SNAPSHOT"
}
```

## Usage

### Preview Resources

Preview which resources would be removed without actually deleting them:

```bash
./gradlew pruneResourcesPreviewDebug
# or for release variant
./gradlew pruneResourcesPreviewRelease
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

To see which directories are being scanned, use `--info`:

```bash
./gradlew pruneResourcesPreviewDebug --info
```

### Prune Resources

Remove unused resources from your project:

```bash
./gradlew pruneResourcesDebug
# or for release variant
./gradlew pruneResourcesRelease
```

> **Warning**: This operation modifies your source files. Make sure to commit your changes before running, or use `pruneResourcesPreview` first to preview.

## Configuration

Configure the plugin in your module's `build.gradle.kts`:

```kotlin
resourcePruner {
    // Exclude resources matching these regex patterns from pruning
    excludeResourceNamePatterns.addAll(
        "^ic_launcher.*",     // Preserve launcher icons
        "^google_play_.*",    // Preserve Google Play assets
        "^app_name$",         // Preserve app name
    )

    // Optional: Only target specific resource types
    targetResourceTypes.addAll("drawable", "string", "layout")

    // Optional: Exclude specific resource types from pruning
    excludeResourceTypes.addAll("menu")  // Preserve all menu resources

    // Optional: Specify which source sets to scan
    sourceSets.addAll("main", "debug")
}
```

### Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `excludeResourceNamePatterns` | `List<String>` | Regex patterns for resource names to exclude from pruning |
| `targetResourceTypes` | `Set<String>` | Resource types to target (empty = all types) |
| `excludeResourceTypes` | `Set<String>` | Resource types to exclude from pruning (applied after targetResourceTypes) |
| `sourceSets` | `Set<String>` | Source sets to scan for usage (default: `["main"]`) |
| `scanDependentProjects` | `Boolean` | Scan dependent projects for resource usage (default: `true`) |
| `cascadePrune` | `Boolean` | Repeatedly prune until no unused resources remain (default: `true`, max 5 iterations) |

### Supported Resource Types

- `drawable`, `mipmap` - Image resources
- `layout` - Layout XML files
- `menu` - Menu XML files
- `raw` - Raw resources (audio, video, JSON, text, etc.)
- `string`, `plurals` - String resources
- `color` - Color resources
- `dimen` - Dimension resources
- `style` - Style resources
- `animator`, `anim` - Animation resources
- `bool`, `integer`, `array` - Value resources
- `attr` - Attribute resources

### Using with build-logic (Convention Plugins)

For large multi-module projects, you can centralize the plugin configuration using Gradle's [convention plugins](https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html) pattern.

> **Note for Gradle 8.x users:** This plugin is compiled with Kotlin 2.2.x, while Gradle 8.x uses Kotlin 2.0.x internally. If you encounter a `Module was compiled with an incompatible version of Kotlin` error, add the following to your `build-logic/build.gradle.kts`:
>
> ```kotlin
> tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
>     compilerOptions {
>         freeCompilerArgs.add("-Xskip-metadata-version-check")
>     }
> }
> ```
>
> This is not needed when using Gradle 9.x or when applying the plugin directly via `plugins { }` block without convention plugins.

**1. Add the plugin to your version catalog (`gradle/libs.versions.toml`):**

```toml
[versions]
resource-pruner = "<latest-version>"

[libraries]
resource-pruner-gradle-plugin = { module = "net.syarihu.resourcepruner:resource-pruner-gradle-plugin", version.ref = "resource-pruner" }
```

**2. Register the plugin in your root `settings.gradle.kts`:**

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    plugins {
        id("net.syarihu.resource-pruner") version "<latest-version>"
    }
}
```

**3. Configure `build-logic/settings.gradle.kts`:**

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    plugins {
        id("net.syarihu.resource-pruner") version "<latest-version>"
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
```

**4. Add the plugin dependency to `build-logic/build.gradle.kts`:**

```kotlin
plugins {
    `kotlin-dsl`
}

// For Gradle 8.x only - see note above
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.resource.pruner.gradle.plugin)
}
```

**5. Create a configuration function (`ResourcePrunerConfiguration.kt`):**

```kotlin
import net.syarihu.resourcepruner.gradle.ResourcePrunerExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.configureResourcePruner() {
    extensions.configure<ResourcePrunerExtension> {
        excludeResourceNamePatterns.addAll(
            "^ic_launcher.*",     // Preserve launcher icons
            "^google_play_.*",    // Preserve Google Play assets
        )
    }
}
```

**6. Apply in your convention plugins (precompiled script plugin example):**

```kotlin
// convention.android.app.gradle.kts
plugins {
    id("com.android.application")
    id("net.syarihu.resource-pruner")
}

configureResourcePruner()
```

```kotlin
// convention.android.library.gradle.kts
plugins {
    id("com.android.library")
    id("net.syarihu.resource-pruner")
}

configureResourcePruner()
```

Or if you prefer class-based convention plugins:

```kotlin
// AndroidApplicationConventionPlugin.kt
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("net.syarihu.resource-pruner")
            configureResourcePruner()
        }
    }
}
```

This ensures consistent resource pruner configuration across all modules in your project.

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

**Important:** You only need to apply the plugin to modules whose resources you want to manage. Dependent modules (app modules that use the library) do NOT need the plugin - they will be scanned automatically.

```
project-root/
├── app/                    # Uses resources from common-ui
│   └── build.gradle.kts    # implementation(project(":common-ui"))
│                           # ← No plugin needed here
└── common-ui/              # Library with shared resources
    └── build.gradle.kts    # Has resource-pruner plugin
```

When you run `./gradlew :common-ui:pruneResourcesPreviewDebug`, the plugin will:
1. Find all projects that depend on `common-ui` (in this case, `app`)
2. Scan those projects' source code for resource references
3. Correctly identify library resources that are used by dependent modules

This prevents false positives where library resources appear unused because they're only referenced in app modules.

For libraries used by **multiple modules**, the plugin scans all of them to ensure resources are only pruned when truly unused:

```
project-root/
├── app-a/                  # Uses resource_a from common-ui
├── app-b/                  # Uses resource_b from common-ui
└── common-ui/              # Has resource_a, resource_b, resource_c
                            # → Only resource_c will be pruned
```

If your app module also has its own resources to manage, you can apply the plugin there too:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("net.syarihu.resource-pruner")  // Optional: only if app has its own resources to prune
}
```

To disable dependent project scanning (e.g., for standalone library development):

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

## Acknowledgements

This project was inspired by [gradle-unused-resources-remover-plugin](https://github.com/konifar/gradle-unused-resources-remover-plugin).

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
