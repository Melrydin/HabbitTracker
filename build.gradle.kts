import org.gradle.api.attributes.Bundling

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

/**
 * ktlint runs through its CLI rather than a Gradle plugin: on AGP 9 the common
 * plugins only register tasks for the build scripts and leave the Kotlin sources
 * unchecked. The code style comes from `.editorconfig`.
 */
val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(libs.ktlint.cli) {
        // ktlint-cli ships in two variants; without this attribute Gradle cannot
        // decide between them.
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
    }
}

private val ktlintTargets = arrayOf("**/src/**/*.kt", "**/*.kts", "!**/build/**")

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Checks every Kotlin file against the code style."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(*ktlintTargets)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Formats every Kotlin file according to the code style."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("--format", *ktlintTargets)
}
