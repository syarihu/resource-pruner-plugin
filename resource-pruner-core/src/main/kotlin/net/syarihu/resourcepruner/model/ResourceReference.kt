package net.syarihu.resourcepruner.model

/**
 * Represents a reference to a resource found in the codebase.
 *
 * @property resourceName The name of the referenced resource
 * @property resourceType The type of the referenced resource
 * @property location The location of the reference
 */
data class ResourceReference(
  val resourceName: String,
  val resourceType: ResourceType,
  val location: ReferenceLocation,
)
