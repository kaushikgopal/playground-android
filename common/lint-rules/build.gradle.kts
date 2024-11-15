plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.android.lint.get().pluginId)
}

lint {
    htmlReport = true
    htmlOutput = file("lint-report.html")
    textReport = true
    absolutePaths = false
    ignoreTestSources = true
}


dependencies {
    compileOnly(libs.bundles.lint.api)
    // testImplementation(libs.bundles.lint.tests)
}