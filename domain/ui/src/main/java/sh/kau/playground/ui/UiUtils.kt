package sh.kau.playground.ui

import android.content.Context
import sh.kau.playground.shared.App

/**
 * This provides a type-safe way to access the Application instance from within a Composable,
 * typically used when trying to get a DI component setup.
 */
fun Context.app(): App {
  return applicationContext as App
}
