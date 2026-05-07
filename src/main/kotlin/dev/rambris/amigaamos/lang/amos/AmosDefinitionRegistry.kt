package dev.rambris.amigaamos.lang.amos

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import kotlinx.serialization.json.Json
import java.net.URI
import java.util.Collections
import java.util.Locale
import java.util.WeakHashMap

enum class AmosDefinitionSourceKind {
    CORE,
    BUNDLED,
    GLOBAL,
    PROJECT
}

data class AmosDefinitionFileEntry(
    val sourceKind: AmosDefinitionSourceKind,
    val reference: String,
    val extensionId: String?,
    val extensionName: String?,
    val extensionFilename: String?,
    val extensionVendor: String?,
    val extensionVersion: String?,
    val slot: Int?,
    val canDisable: Boolean,
    val canRemove: Boolean,
    val enabled: Boolean,
    val parseError: String?
)

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
        val projectModCount: Long,
        val appModCount: Long,
        val definitions: List<AmosCallableDefinition>
    )

    private data class ConfiguredSource(
        val source: AmosDefinitionSource,
        val sourceKind: AmosDefinitionSourceKind,
        val reference: String,
        val canDisable: Boolean,
        val canRemove: Boolean
    )

    private data class ParsedConfiguredSource(
        val configured: ConfiguredSource,
        val file: AmosDefinitionFile?,
        val parseError: String?
    )

    val definitions: List<AmosCallableDefinition> by lazy {
        loadFromSources(defaultSources())
    }

    fun definitions(project: Project?): List<AmosCallableDefinition> {
        if (project == null) {
            return definitions
        }

        val projectSettings = project.service<AmosDefinitionProjectSettings>()
        val appSettings = applicationSettings()
        val projectModCount = projectSettings.modificationCount
        val appModCount = appSettings.modificationCount
        val cached = projectCache[project]
        if (cached != null && cached.projectModCount == projectModCount && cached.appModCount == appModCount) {
            return cached.definitions
        }

        val globalDisabled = appSettings.disabledExtensionIdsNormalized()
        val projectDisabled = projectSettings.disabledExtensionIdsNormalized()
        val parsedSources = parseConfiguredSources(configuredSources(project))

        val enabledFiles = parsedSources.filter { parsed ->
            val extensionId = parsed.file?.extension?.extensionId()
            if (extensionId == null || parsed.configured.sourceKind == AmosDefinitionSourceKind.CORE) {
                return@filter true
            }
            if (
                parsed.configured.sourceKind == AmosDefinitionSourceKind.GLOBAL ||
                parsed.configured.sourceKind == AmosDefinitionSourceKind.BUNDLED
            ) {
                return@filter extensionId !in globalDisabled
            }
            if (parsed.configured.sourceKind == AmosDefinitionSourceKind.PROJECT) {
                return@filter extensionId !in projectDisabled
            }
            false
        }.mapNotNull { it.file?.let { file -> parsedToLoaded(it.configured.source, file) } }

        val loaded = loadFromLoadedFiles(enabledFiles)

        projectCache[project] = CachedProjectDefinitions(projectModCount, appModCount, loaded)
        return loaded
    }

    fun definitionFileEntries(project: Project?): List<AmosDefinitionFileEntry> {
        val projectSettings = project?.service<AmosDefinitionProjectSettings>()
        val appSettings = applicationSettings()
        val globalDisabled = appSettings.disabledExtensionIdsNormalized()
        val projectDisabled = projectSettings?.disabledExtensionIdsNormalized().orEmpty()

        return parseConfiguredSources(configuredSources(project)).map { parsed ->
            val extension = parsed.file?.extension
            val extensionId = extension?.extensionId()
            val enabled = when {
                extensionId == null -> true
                parsed.configured.sourceKind == AmosDefinitionSourceKind.CORE -> true
                parsed.configured.sourceKind == AmosDefinitionSourceKind.GLOBAL ||
                    parsed.configured.sourceKind == AmosDefinitionSourceKind.BUNDLED -> extensionId !in globalDisabled
                else -> extensionId !in projectDisabled
            }

            AmosDefinitionFileEntry(
                sourceKind = parsed.configured.sourceKind,
                reference = parsed.configured.reference,
                extensionId = extensionId,
                extensionName = extension?.name?.trim()?.takeIf(String::isNotEmpty) ?: extension?.id?.trim(),
                extensionFilename = extension?.filename?.trim()?.takeIf(String::isNotEmpty),
                extensionVendor = extension?.vendor?.trim()?.takeIf(String::isNotEmpty),
                extensionVersion = extension?.version?.trim()?.takeIf(String::isNotEmpty),
                slot = extension?.slot,
                canDisable = parsed.configured.canDisable && extensionId != null,
                canRemove = parsed.configured.canRemove,
                enabled = enabled,
                parseError = parsed.parseError
            )
        }
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
        val loadedFiles = sources
            .distinctBy { Triple(it.resourcePath, it.uri, it.classLoader) }
            .mapNotNull { source ->
                readDefinitionFile(source)?.let { file -> parsedToLoaded(source, file) }
            }
        return loadFromLoadedFiles(loadedFiles)
    }

    private fun loadFromLoadedFiles(loadedFiles: List<LoadedDefinitionFile>): List<AmosCallableDefinition> {
        val merged = linkedMapOf<Pair<String, AmosDefinitionKind>, AmosCallableDefinition>()

        loadedFiles.forEach { loaded ->
            loaded.file.definitions.forEach { entry ->
                if (entry.kind == AmosDefinitionKind.STRUCTURE && !loaded.source.allowStructures) {
                    return@forEach
                }
                val definition = entry.toRuntimeDefinition()
                    .copy(extensionId = loaded.extensionId, extensionName = loaded.extensionName)
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

    private fun configuredSources(project: Project?): List<ConfiguredSource> {
        val configured = mutableListOf<ConfiguredSource>()

        coreResourcePaths.forEach { path ->
            configured += ConfiguredSource(
                source = amosClasspathDefinitionSource(
                    resourcePath = path,
                    classLoader = AmosDefinitionRegistry::class.java.classLoader,
                    origin = "core:$path",
                    allowStructures = true
                ),
                sourceKind = AmosDefinitionSourceKind.CORE,
                reference = "classpath:$path",
                canDisable = false,
                canRemove = false
            )
        }

        runCatching {
            extensionPoint.extensionList.forEach { bean ->
                val source = bean.toSource() ?: return@forEach
                configured += ConfiguredSource(
                    source = source,
                    sourceKind = AmosDefinitionSourceKind.BUNDLED,
                    reference = "classpath:${source.resourcePath ?: ""}",
                    canDisable = true,
                    canRemove = false
                )
            }
        }

        applicationSettings().additionalDefinitionSourceEntries().forEach { (reference, source) ->
            configured += ConfiguredSource(
                source = source,
                sourceKind = AmosDefinitionSourceKind.GLOBAL,
                reference = reference,
                canDisable = true,
                canRemove = true
            )
        }

        if (project != null) {
            project.service<AmosDefinitionProjectSettings>().additionalDefinitionSourceEntries().forEach { (reference, source) ->
                configured += ConfiguredSource(
                    source = source,
                    sourceKind = AmosDefinitionSourceKind.PROJECT,
                    reference = reference,
                    canDisable = true,
                    canRemove = true
                )
            }
        }

        return configured.distinctBy { Triple(it.source.resourcePath, it.source.uri, it.reference) }
    }

    private fun parseConfiguredSources(sources: List<ConfiguredSource>): List<ParsedConfiguredSource> {
        return sources.map { configured ->
            val parsed = runCatching {
                readDefinitionFile(configured.source)
            }
            ParsedConfiguredSource(
                configured = configured,
                file = parsed.getOrNull(),
                parseError = parsed.exceptionOrNull()?.message
            )
        }
    }

    private data class LoadedDefinitionFile(
        val source: AmosDefinitionSource,
        val file: AmosDefinitionFile,
        val extensionId: String?,
        val extensionName: String?
    )

    private fun parsedToLoaded(source: AmosDefinitionSource, file: AmosDefinitionFile): LoadedDefinitionFile {
        val extensionId = file.extension?.extensionId()
        val extensionName = file.extension?.name?.trim()?.takeIf { it.isNotEmpty() }
            ?: file.extension?.id?.trim()?.takeIf { it.isNotEmpty() }
        return LoadedDefinitionFile(source, file, extensionId, extensionName)
    }

    private fun readDefinitionFile(source: AmosDefinitionSource): AmosDefinitionFile? {
        val stream = source.inputStream() ?: return null
        return stream.bufferedReader(Charsets.UTF_8).use {
            json.decodeFromString<AmosDefinitionFile>(it.readText())
        }
    }

    private fun applicationSettings(): AmosDefinitionApplicationSettings {
        return ApplicationManager.getApplication().service()
    }

    private fun AmosDefinitionEntry.toRuntimeDefinition(): AmosCallableDefinition {
        return AmosCallableDefinition(
            name = name,
            kind = kind,
            returnType = returnType,
            documentation = documentation,
            link = link?.let { runCatching { URI(it) }.getOrNull() },
            signatures = signatures.mapNotNull { signature ->
                val presentation = signature.presentation ?: return@mapNotNull null
                AmosFunctionSignature(
                    name = name,
                    presentation = presentation,
                    parameterRanges = computeParameterRanges(presentation, signature.parameters),
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
        uri != null -> runCatching { URI(uri).toURL().openStream() }.getOrNull()
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





