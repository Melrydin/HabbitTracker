import org.gradle.api.attributes.Bundling

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

/**
 * ktlint laeuft ueber das CLI statt ueber ein Gradle-Plugin: die gaengigen Plugins
 * registrieren unter AGP 9 keine Tasks fuer die Kotlin-Quellen, sondern nur fuer
 * die Build-Skripte. Der Codestil kommt aus `.editorconfig`.
 */
val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(libs.ktlint.cli) {
        // ktlint-cli wird in zwei Varianten veroeffentlicht, ohne dieses Attribut
        // kann Gradle sich nicht entscheiden.
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
    }
}

private val ktlintTargets = arrayOf("**/src/**/*.kt", "**/*.kts", "!**/build/**")

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Prueft alle Kotlin-Dateien gegen den Codestil."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(*ktlintTargets)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Formatiert alle Kotlin-Dateien nach dem Codestil."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("--format", *ktlintTargets)
}
