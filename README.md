# Playground Android

This is a template project for Android development. I use it as a way
to test new concepts or integrate libraries that are otherwise hard
in a more complex project.


Some of the concepts implemented here:

- [x] gradle [version catalog](https://github.com/kaushikgopal/playground-android/blob/master/gradle/libs.versions.toml), BOM & Bundles (one source of truth)
- [x] [sharing build logic](./build-logic/README.md) with [gradle convention plugin](https://docs.gradle.org/current/samples/sample_convention_plugins.html)
- [x] [custom lint-rules](https://github.com/kaushikgopal/playground-android/pull/5/files)
- [x] [Makefile](https://github.com/kaushikgopal/playground-android/blob/master/Makefile) with common cli commands
- [x] [logcat](https://github.com/square/logcat) lib and injecting [multiple loggers](https://github.com/kaushikgopal/playground-android/blob/master/common/log/src/main/java/sh/kau/playground/common/log/CompositeLogger.kt)
- [x] basic networking with [ktor](https://ktor.io/docs/client.html) [#10](https://github.com/kaushikgopal/playground-android/pull/10/files#diff-61300620752e698467343ba4270127d0cbb3c9e3153bb001ff51102244d2c7b2)
- [ ] [Coil](https://coil-kt.github.io/coil/) for image loading
- [x] [multi module](#app-module-diagram-multi-module-setup) setup
- [x] compose-navigation between feature modules
- [x] [dependency injection with kotlin-inject-anvil](https://github.com/kaushikgopal/playground-android/pull/12)
  - [x] [function-injection](https://github.com/kaushikgopal/playground-android/pull/9/commits/aad254957a003982633006fb2f350ee7a372f11d) demo in `@Composable`
- [x] USF architecture (much like [usf-movies-android](https://github.com/kaushikgopal/movies-usf-android))

# Getting started

1. Clone the repo in your android folder
```shell
git clone https://github.com/kaushikgopal/playground-android.git android
cd android
# assuming you are already tracking in git
trash .git
trash .idea 
```

2. Run the package rename script
```shell
 ./rename_package.sh app.awesome.android
```

3. Clean & Rebuild app
```shell
make clean
make
```
Ready to go!

# App module diagram (multi-module setup)

Below diagram should give you an idea of how the inter module dependencies are setup.
In practice, when you add feature modules it is pretty straightforward as the
core requirements are already setup. See the [Landing feature's build.gradle.kts file](https://github.com/kaushikgopal/playground-android/blob/master/features/landing/build.gradle.kts)
as an example for how simple the build script for new features land up being.

The dependency graph is widest at the top (`:app` module)
and becomes more focused and self-contained as you move down through domain and common modules.
So modules at the bottom have lesser dependencies and are more self-contained.

![App module diagram](./app-module-diagram.webp)

- new features are added to :features module
- the core :app module itself assembles all the dependencies (and is intentionally lean)
- template.feature = custom gradle plugin that sets up a fully functional feature for your app
- template.android = custom gradle plugin that has the things you need for a pure android lib
    - think jvm target, minSDK etc. that you don't want to repeat everywhere
- :common modules are shared but can be hot-swapped with another implementation (possible)
- :domain modules are specific to the app but also shared (but not intended to be swapped out)

I talked about this in ep #252 of
[Fragmented](https://fragmentedpodcast.com/episodes/252)
