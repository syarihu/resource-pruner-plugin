package net.syarihu.resourcepruner.examplelib

import android.content.Context

/**
 * A simple helper class from the library module.
 *
 * This class does NOT use any resources directly.
 * The resources defined in this library are used by the app module,
 * demonstrating cross-module resource usage detection.
 */
class LibraryHelper(private val context: Context) {
  fun getMessage(): String {
    return "Hello from LibraryHelper"
  }
}
