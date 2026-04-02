package dev.rambris.amigaamos.lang.amos

import java.net.URI
import java.nio.file.Path

internal object AmosDefinitionSourceReferences {
    fun toSource(reference: String, projectBasePath: String? = null): AmosDefinitionSource? {
        val trimmed = reference.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        if (trimmed.startsWith("classpath:")) {
            val resourcePath = trimmed.removePrefix("classpath:").trim()
            if (resourcePath.isEmpty()) {
                return null
            }
            return amosClasspathDefinitionSource(
                resourcePath = resourcePath,
                classLoader = AmosDefinitionSourceReferences::class.java.classLoader,
                origin = "settings-classpath:$resourcePath"
            )
        }

        val normalizedUri = runCatching {
            val parsed = URI(trimmed)
            if (parsed.scheme.isNullOrBlank()) {
                val path = Path.of(trimmed)
                val resolved = when {
                    path.isAbsolute -> path
                    !projectBasePath.isNullOrBlank() -> Path.of(projectBasePath).resolve(path)
                    else -> path
                }
                resolved.normalize().toUri().toString()
            } else {
                parsed.toString()
            }
        }.getOrNull() ?: return null

        return amosUriDefinitionSource(
            uri = normalizedUri,
            origin = "settings-uri:$normalizedUri"
        )
    }
}

