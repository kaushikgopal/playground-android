package sh.kau.playground.landing.di

import dev.zacsweers.metro.Scope

@Scope
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
)
annotation class LandingScope
