package net.syarihu.resourcepruner.model

/**
 * Represents a resource detected in the Android project.
 *
 * @property name The resource name (e.g., "ic_launcher", "app_name")
 * @property type The type of resource
 * @property location The location where the resource is defined
 * @property qualifiers Resource qualifiers (e.g., "hdpi", "night", "land")
 */
data class DetectedResource(
  val name: String,
  val type: ResourceType,
  val location: ResourceLocation,
  val qualifiers: Set<String> = emptySet(),
)
