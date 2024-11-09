package utils

import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// TODO: KG upgrade this over
fun Project.kotlinOptions(options: KotlinJvmOptions.() -> Unit) {

    this.tasks.withType<KotlinCompile> {
        kotlinOptions {
            options.invoke(this)
        }
    }
}