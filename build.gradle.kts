import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import java.util.Locale

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "dev.rambris"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testImplementation(kotlin("test"))

    intellijPlatform {
        intellijIdea("2025.2.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)


        // Add plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<RunIdeTask> {
        // Work around 2025.2 sandbox startup crashes caused by early new-UI initialization.
        jvmArgs("-Dide.experimental.ui=false")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

val generateAmosCommandIndex by tasks.registering {
    val manualFile = layout.projectDirectory.file("AmosProManual/14-appendix-g-command-index.html")
    val outputFile = layout.buildDirectory.file("generated/resources/amos/commands.tsv")

    inputs.file(manualFile)
    outputs.file(outputFile)

    doLast {
        val html = manualFile.asFile.readText(Charsets.UTF_8)
        val rowRegex = Regex(
            """<tr[^>]*>\s*<th>\s*<a[^>]*>(.*?)</a>\s*</th>\s*<td>(.*?)</td>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        fun normalizeText(value: String): String {
            return value
                .replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .replace("&quot;", "\"")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        val deduped = linkedMapOf<String, Pair<String, String>>()
        rowRegex.findAll(html).forEach { match ->
            val name = normalizeText(match.groupValues[1])
            val kind = normalizeText(match.groupValues[2])
            if (name.isNotEmpty() && kind.isNotEmpty()) {
                val key = name.uppercase(Locale.ROOT)
                deduped.putIfAbsent(key, name to kind)
            }
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                deduped.values
                    .sortedBy { it.first }
                    .joinToString("\n") { "${it.first}\t${it.second}" },
                Charsets.UTF_8
            )
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateAmosCommandIndex)
    from(generateAmosCommandIndex.map { it.outputs.files.singleFile }) {
        into("amos")
    }
}

