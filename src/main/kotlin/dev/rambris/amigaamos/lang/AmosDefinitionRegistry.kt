package dev.rambris.amigaamos.lang

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import kotlinx.serialization.json.Json
import java.net.URI
import java.util.Collections
import java.util.Locale
import java.util.WeakHashMap

object AmosDefinitionRegistry {
    private val extensionPoint = ExtensionPointName<AmosDefinitionResourceBean>(
        "dev.rambris.amigaamos.definitionResource"
    )

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val projectCache = Collections.synchronizedMap(WeakHashMap<Project, CachedProjectDefinitions>())

    private val coreResourcePaths = listOf(
        "amos/definitions/core.json"
    )

    private data class CachedProjectDefinitions(
        val modCount: Long,
        val definitions: List<AmosCallableDefinition>
    )

    val definitions: List<AmosCallableDefinition> by lazy {
        loadFromSources(defaultSources())
    }

    fun definitions(project: Project?): List<AmosCallableDefinition> {
        if (project == null) {
            return definitions
        }

        val settings = project.service<AmosDefinitionProjectSettings>()
        val modCount = settings.modificationCount
        val cached = projectCache[project]
        if (cached != null && cached.modCount == modCount) {
            return cached.definitions
        }

        val disabled = settings.disabledExtensionIdsNormalized()
        val loaded = loadFromSources(defaultSources() + settings.additionalDefinitionSources())
            .filter { definition ->
                val extensionId = definition.extensionId
                extensionId == null || extensionId !in disabled
            }

        projectCache[project] = CachedProjectDefinitions(modCount, loaded)
        return loaded
    }

    fun definitionsFor(name: String): List<AmosCallableDefinition> = definitionsFor(name, null)

    fun definitionFor(name: String): AmosCallableDefinition? = definitionFor(name, null, null)

    fun definitionFor(name: String, kind: AmosDefinitionKind?): AmosCallableDefinition? {
        return definitionFor(name, kind, null)
    }

    fun definitionsByKind(kind: AmosDefinitionKind): List<AmosCallableDefinition> = definitionsByKind(kind, null)

    fun definitionsFor(name: String, project: Project?): List<AmosCallableDefinition> {
        val normalizedName = name.uppercase(Locale.ROOT)
        return definitions(project).filter { it.uppercaseName == normalizedName }
    }

    fun definitionFor(name: String, kind: AmosDefinitionKind?, project: Project? = null): AmosCallableDefinition? {
        val matches = definitionsFor(name, project)
        if (kind != null) {
            return matches.firstOrNull { it.kind == kind }
        }

        val preferredKinds = listOf(
            AmosDefinitionKind.INSTRUCTION,
            AmosDefinitionKind.STRUCTURE,
            AmosDefinitionKind.FUNCTION
        )
        return preferredKinds.asSequence().mapNotNull { preferred ->
            matches.firstOrNull { it.kind == preferred }
        }.firstOrNull()
    }

    fun definitionsByKind(kind: AmosDefinitionKind, project: Project?): List<AmosCallableDefinition> {
        return definitions(project).filter { it.kind == kind }
    }

    fun loadFromSources(sources: List<AmosDefinitionSource>): List<AmosCallableDefinition> {
        val merged = linkedMapOf<Pair<String, AmosDefinitionKind>, AmosCallableDefinition>()
        sources.distinctBy { Triple(it.resourcePath, it.uri, it.classLoader) }.forEach { source ->
            val stream = source.inputStream() ?: return@forEach
            val file = stream.bufferedReader(Charsets.UTF_8).use {
                json.decodeFromString<AmosDefinitionFile>(it.readText())
            }
            val extensionId = file.extension?.extensionId()
            val extensionName = file.extension?.name?.trim()?.takeIf { it.isNotEmpty() }
                ?: file.extension?.id?.trim()?.takeIf { it.isNotEmpty() }

            file.definitions.forEach { entry ->
                if (entry.kind == AmosDefinitionKind.STRUCTURE && !source.allowStructures) {
                    return@forEach
                }
                val definition = entry.toRuntimeDefinition()
                    .copy(extensionId = extensionId, extensionName = extensionName)
                val key = definition.uppercaseName to definition.kind
                val existing = merged[key]
                merged[key] = if (existing == null) definition else existing.mergeWith(definition)
            }
        }

        return merged.values.sortedWith(compareBy({ it.name }, { it.kind.name }))
    }

    private fun defaultSources(): List<AmosDefinitionSource> {
        val sources = coreResourcePaths.map { path ->
            amosClasspathDefinitionSource(
                resourcePath = path,
                classLoader = AmosDefinitionRegistry::class.java.classLoader,
                origin = "core:$path",
                allowStructures = true
            )
        }.toMutableList()
        runCatching {
            extensionPoint.extensionList.mapNotNull { it.toSource() }
        }.getOrDefault(emptyList()).forEach { sources += it }
        return sources
    }

    private fun AmosDefinitionEntry.toRuntimeDefinition(): AmosCallableDefinition {
        return AmosCallableDefinition(
            name = name,
            kind = kind,
            returnType = returnType,
            documentation = documentation,
            link = link?.let { runCatching { URI(it) }.getOrNull() },
            signatures = signatures.map { signature ->
                AmosFunctionSignature(
                    name = name,
                    presentation = signature.presentation,
                    parameterRanges = computeParameterRanges(signature.presentation, signature.parameters),
                    parameters = signature.parameters.map { parameter ->
                        AmosParameterSpec(
                            kind = parameter.kind,
                            name = parameter.name,
                            valueType = parameter.valueType,
                            keyword = parameter.keyword,
                            optional = parameter.optional,
                            documentation = parameter.documentation
                        )
                    },
                    documentation = signature.documentation
                )
            },
            extensionId = null,
            extensionName = null
        )
    }

    private fun AmosCallableDefinition.mergeWith(other: AmosCallableDefinition): AmosCallableDefinition {
        val mergedSignatures = (signatures + other.signatures)
            .distinctBy { it.presentation }

        return copy(
            returnType = other.returnType ?: returnType,
            documentation = other.documentation ?: documentation,
            link = other.link ?: link,
            signatures = mergedSignatures,
            extensionId = other.extensionId ?: extensionId,
            extensionName = other.extensionName ?: extensionName
        )
    }

    private fun AmosDefinitionSource.inputStream() = when {
        resourcePath != null && classLoader != null -> classLoader.getResourceAsStream(resourcePath)
        uri != null -> uri?.let { runCatching { URI(it).toURL().openStream() }.getOrNull() }
        else -> null
    }

    private fun computeParameterRanges(
        presentation: String,
        parameters: List<AmosParameterEntry>
    ): List<TextRange> {
        var searchStart = presentation.indexOf('(').takeIf { it >= 0 } ?: 0
        return parameters.map { parameter ->
            val needle = parameter.keyword ?: parameter.name ?: parameter.valueType?.name?.lowercase(Locale.ROOT)
            if (needle.isNullOrBlank()) {
                TextRange.EMPTY_RANGE
            } else {
                val foundAt = presentation.indexOf(needle, startIndex = searchStart, ignoreCase = true)
                if (foundAt >= 0) {
                    searchStart = foundAt + needle.length
                    TextRange(foundAt, foundAt + needle.length)
                } else {
                    TextRange.EMPTY_RANGE
                }
            }
        }
    }
}





