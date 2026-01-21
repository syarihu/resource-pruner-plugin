package net.syarihu.resourcepruner.examplemultiapp

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import net.syarihu.resourcepruner.examplelib.R as LibR

/**
 * Second app module for verifying multi-app resource detection.
 *
 * This module uses DIFFERENT resources from example-lib than the example module:
 * - example uses: lib_used_string, ic_lib_used_icon, layout_lib_used
 * - example-multi-app uses: lib_unused_string, ic_lib_unused_icon
 *
 * When running pruneResourcesPreview on example-lib, only resources unused by BOTH apps
 * should be marked for pruning:
 * - lib_deprecated_message (unused by both)
 * - layout_lib_unused (unused by both)
 *
 * Resources used by only one app should be preserved:
 * - lib_used_string (used by example only) → preserved
 * - lib_unused_string (used by example-multi-app only) → preserved
 */
class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Create a simple layout programmatically
    val textView = TextView(this).apply {
      // Use lib_unused_string - this resource was marked as "unused" when only example existed
      // But now example-multi-app uses it, so it should be preserved
      text = getString(LibR.string.lib_unused_string)
    }

    val imageView = ImageView(this).apply {
      // Use ic_lib_unused_icon - this was also "unused" with only example
      // Now it should be preserved because example-multi-app uses it
      setImageResource(LibR.drawable.ic_lib_unused_icon)
    }

    setContentView(textView)

    // Log for demonstration
    Log.d("MainActivity", "example-multi-app using lib resources that example doesn't use")
  }
}
