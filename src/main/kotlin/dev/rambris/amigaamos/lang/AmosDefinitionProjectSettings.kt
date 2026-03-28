package dev.rambris.amigaamos.lang

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.SimpleModificationTracker
import java.net.URI
import java.nio.file.Path

@Service(Service.Level.PROJECT)
@State(name = "AmosDefinitionProjectSettings", storages = [Storage("amiga-amos.xml")])
class AmosDefinitionProjectSettings :
    PersistentStateComponent<AmosDefinitionProjectSettings.State>,
    SimpleModificationTracker() {

    data class State(
        var disabledExtensionIds: MutableList<String> = mutableListOf(),
        var additionalSources: MutableList<String> = mutableListOf()
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
        incModificationCount()
    }

    fun disabledExtensionIdsNormalized(): Set<String> {
        return state.disabledExtensionIds
            .asSequence()
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .map { it.lowercase() }
            .toSet()
    }

    fun additionalDefinitionSources(): List<AmosDefinitionSource> {
        return state.additionalSources.mapNotNull(::toSource)
    }

    private fun toSource(reference: String): AmosDefinitionSource? {
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
                classLoader = AmosDefinitionProjectSettings::class.java.classLoader,
                origin = "project-classpath:$resourcePath"
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
            origin = "project-uri:$normalizedUri"
        )
    }
}

