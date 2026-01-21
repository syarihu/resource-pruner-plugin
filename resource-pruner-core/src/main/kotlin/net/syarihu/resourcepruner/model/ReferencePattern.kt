package net.syarihu.resourcepruner.model

/**
 * Represents how a resource is referenced in the code.
 */
enum class ReferencePattern {
  /** XML reference (e.g., @string/xxx) */
  XML_REFERENCE,

  /** Kotlin R class reference (e.g., R.drawable.xxx) */
  KOTLIN_R_CLASS,

  /** Java R class reference (e.g., R.drawable.xxx) */
  JAVA_R_CLASS,

  /** ViewBinding reference (e.g., binding.xxxLayout) */
  VIEW_BINDING,

  /** Paraphrase library reference (e.g., FormattedResource(R.string.xxx)) */
  PARAPHRASE,
}
