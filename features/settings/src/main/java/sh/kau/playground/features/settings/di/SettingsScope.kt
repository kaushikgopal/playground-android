package sh.kau.playground.features.settings.di

import me.tatarka.inject.annotations.Scope

@Scope
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
)
annotation class SettingsScope