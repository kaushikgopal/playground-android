# Playground Android

This is a template project for Android development. I use it as a way
to test new concepts or integrate libraries that are otherwise hard
in a more complex project.

Some of the concepts implemented here:

- [x] multi module setup (see `:common` & `:domain` & `:features`)
- [x] sharing build logic with [gradle convention plugin](https://docs.gradle.org/current/samples/sample_convention_plugins.html)
- [x] gradle version catalog, BOM & Bundles
- [x] compose-navigation between feature modules
- [x] dependency injection with kotlin-inject(-anvil)
- [ ] custom lint-rules
- [ ] USF architecture (much like [usf-movies-android](https://github.com/kaushikgopal/movies-usf-android))
- [x] logcat lib and injecting multiple loggers

# Getting started
- Download this repository and open the template folder on Android Studio
- in libs.versions.toml change app-namespace to your desired package name
- in AndroidStudio rename the app package sh.kau.playground
- Check if the manifest package was renamed along with the package
- In app/build.gradle.kts, change the applicationId to the new app package


Ready to Use and you're ready to start working on your new app!