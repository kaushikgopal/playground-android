package sh.kau.playground.coroutines

import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import sh.kau.playground.coroutines.CoroutineConstants.IO
import sh.kau.playground.coroutines.CoroutineConstants.SINGLE_THREAD
import sh.kau.playground.coroutines.CoroutineConstants.SUPERVISOR
import sh.kau.playground.shared.di.Named
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesSubcomponent(CoroutinesScope::class)
@SingleIn(CoroutinesScope::class)
interface CoroutineComponent {

  // region: IO

  // Dispatchers.IO
  // - uses a shared pool of on-demand created threads
  // - designed for offloading of IO-intensive blocking operations
  //   (file I/O; blocking socket I/O)

  @Provides @Named(IO) fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

  @Provides
  @Named(IO + SUPERVISOR)
  fun provideIoSupervisorScope(
      exceptionHandler: CoroutineExceptionHandler,
      @Named(IO) ioDispatcher: CoroutineDispatcher
  ): CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob() + exceptionHandler)

  @Provides
  @Named(IO + SUPERVISOR)
  fun provideIoSupervisorContext(
      @Named(IO) ioDispatcher: CoroutineDispatcher,
      exceptionHandler: CoroutineExceptionHandler,
  ): CoroutineContext = ioDispatcher + SupervisorJob() + exceptionHandler

  // endregion

  // region: Single Threaded

  // Executors.newSingleThreadExecutor()
  // - creates a dispatcher that has only as single-thread for tasks
  //   (sequential tasks; controlled concurrency)

  @Provides
  @Named(SINGLE_THREAD)
  fun provideSingleThreadedDispatcher(): CoroutineDispatcher =
      Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  @Provides
  @Named(SINGLE_THREAD + SUPERVISOR)
  fun provideSingleThreadedSupervisorContext(
      @Named(SINGLE_THREAD) singleThreadedDispatcher: CoroutineDispatcher,
      exceptionHandler: CoroutineExceptionHandler,
  ): CoroutineContext = singleThreadedDispatcher + SupervisorJob() + exceptionHandler

  // endregion

  @Provides
  fun provideDefaultCoroutineExceptionHandler(): CoroutineExceptionHandler =
      CoroutineExceptionHandler { _, throwable ->
//        logcat(ERROR) { (throwable ?: RuntimeException("Uncaught coroutine exception")).asLog() }
      }

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createCoroutineComponent(): CoroutineComponent
  }
}

@Scope
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
)
annotation class CoroutinesScope
