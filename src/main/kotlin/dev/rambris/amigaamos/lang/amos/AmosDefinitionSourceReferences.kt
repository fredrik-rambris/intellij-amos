package dev.rambris.amigaamos.lang.amos

import java.net.URI
import java.nio.file.Path

internal object AmosDefinitionSourceReferences {
    fun toSource(reference: String): AmosDefinitionSource? {
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
                Path.of(trimmed).toUri().toString()
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

