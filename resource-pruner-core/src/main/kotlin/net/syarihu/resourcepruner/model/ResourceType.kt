package net.syarihu.resourcepruner.model

/**
 * Represents the type of Android resource.
 *
 * Resources are categorized into two main groups:
 * - [File]: Resources that exist as individual files (e.g., drawables, layouts)
 * - [Value]: Resources defined within XML value files (e.g., strings, colors)
 */
sealed class ResourceType {
  /**
   * The resource type name used in R class references (e.g., "drawable", "string").
   */
  abstract val typeName: String

  /**
   * File-based resources that exist as individual files in the res/ directory.
   */
  sealed class File(
    override val typeName: String,
  ) : ResourceType() {
    /** Drawable resources (res/drawable/) */
    data object Drawable : File("drawable")

    /** Layout resources (res/layout/) */
    data object Layout : File("layout")

    /** Menu resources (res/menu/) */
    data object Menu : File("menu")

    /** Mipmap resources (res/mipmap/) */
    data object Mipmap : File("mipmap")

    /** Animator resources (res/animator/) */
    data object Animator : File("animator")

    /** Animation resources (res/anim/) */
    data object Anim : File("anim")

    /** Color state list resources (res/color/) */
    data object ColorStateList : File("color")
  }

  /**
   * Value-based resources defined as elements within XML files in res/values/.
   */
  sealed class Value(
    override val typeName: String,
  ) : ResourceType() {
    /** String resources (<string>) */
    data object StringRes : Value("string")

    /** Color resources (<color>) */
    data object Color : Value("color")

    /** Dimension resources (<dimen>) */
    data object Dimen : Value("dimen")

    /** Style resources (<style>) */
    data object Style : Value("style")

    /** Boolean resources (<bool>) */
    data object Bool : Value("bool")

    /** Integer resources (<integer>) */
    data object Integer : Value("integer")

    /** Array resources (<array>, <string-array>, <integer-array>) */
    data object Array : Value("array")

    /** Attribute resources (<attr>) */
    data object Attr : Value("attr")

    /** Plurals resources (<plurals>) */
    data object Plurals : Value("plurals")
  }

  companion object {
    /**
     * All file-based resource types.
     */
    val fileTypes: List<File> = listOf(
      File.Drawable,
      File.Layout,
      File.Menu,
      File.Mipmap,
      File.Animator,
      File.Anim,
      File.ColorStateList,
    )

    /**
     * All value-based resource types.
     */
    val valueTypes: List<Value> = listOf(
      Value.StringRes,
      Value.Color,
      Value.Dimen,
      Value.Style,
      Value.Bool,
      Value.Integer,
      Value.Array,
      Value.Attr,
      Value.Plurals,
    )

    /**
     * All resource types.
     * Value types come first to prioritize them when looking up by type name
     * (e.g., "color" should resolve to Value.Color, not File.ColorStateList).
     */
    val allTypes: List<ResourceType> = valueTypes + fileTypes

    /**
     * Returns the [ResourceType] for the given type name, or null if not found.
     * Prefers Value types over File types when names conflict (e.g., "color").
     */
    fun fromTypeName(typeName: String): ResourceType? = allTypes.find { it.typeName == typeName }

    /**
     * Returns the [ResourceType.File] for the given directory name, or null if not found.
     *
     * @param dirName The directory name (e.g., "drawable", "drawable-hdpi", "layout-land")
     */
    fun fromDirectoryName(dirName: String): File? {
      val baseName = dirName.substringBefore('-')
      return fileTypes.find { it.typeName == baseName }
    }
  }
}
