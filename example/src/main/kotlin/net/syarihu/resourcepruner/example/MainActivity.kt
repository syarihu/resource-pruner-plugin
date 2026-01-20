package net.syarihu.resourcepruner.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.syarihu.resourcepruner.example.databinding.ActivityMainBinding

/**
 * Example activity to demonstrate resource-pruner-plugin.
 *
 * This activity uses:
 * - R.layout.activity_main (via ViewBinding)
 * - R.string.app_name (via AndroidManifest)
 * - R.string.hello_world (directly)
 * - R.color.primary (via theme/styles)
 * - R.drawable.ic_used_icon (directly)
 *
 * Resources NOT used (should be pruned):
 * - R.string.unused_string
 * - R.drawable.ic_unused_icon
 * - R.color.unused_color
 * - R.layout.layout_unused
 * - R.menu.menu_unused
 */
class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // Use some resources
    binding.textView.text = getString(R.string.hello_world)
    binding.imageView.setImageResource(R.drawable.ic_used_icon)
  }
}
