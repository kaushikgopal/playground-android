package sh.kau.playground.app

import android.os.Build
import android.os.StrictMode

/**
 * Enables StrictMode in debug builds so accidental blocking calls on the main thread surface
 * immediately during development.
 */
object StrictModeInitializer {
  fun enableStrictMode() {
    val threadPolicy =
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()

    val vmPolicyBuilder = StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().penaltyLog()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vmPolicyBuilder.detectContentUriWithoutPermission()
    }

    StrictMode.setThreadPolicy(threadPolicy)
    StrictMode.setVmPolicy(vmPolicyBuilder.build())
  }
}
