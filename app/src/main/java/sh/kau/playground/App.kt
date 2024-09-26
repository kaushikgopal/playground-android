package sh.kau.playground

import android.app.Application
import android.util.Log
import sh.kau.playground.di.AppComponent

class App : Application() {

  private val appComponent by lazy(LazyThreadSafetyMode.NONE) { AppComponent.from(this) }

  override fun onCreate() {
    super.onCreate()
    Log.i("AppComponent", "Welcome to ${appComponent.provideAppName()}")
  }
}
