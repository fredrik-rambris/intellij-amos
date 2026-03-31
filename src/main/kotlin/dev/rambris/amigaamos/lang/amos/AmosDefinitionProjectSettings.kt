package dev.rambris.amigaamos.lang.amos

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.SimpleModificationTracker
import java.util.Locale

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
            .map { it.lowercase(Locale.ROOT) }
            .toSet()
    }

    fun additionalSourceReferences(): List<String> {
        return state.additionalSources
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .distinct()
    }

    fun additionalDefinitionSourceEntries(): List<Pair<String, AmosDefinitionSource>> {
        return additionalSourceReferences().mapNotNull { reference ->
            AmosDefinitionSourceReferences.toSource(reference)?.let { source -> reference to source }
        }
    }

    fun additionalDefinitionSources(): List<AmosDefinitionSource> {
        return additionalDefinitionSourceEntries().map { it.second }
    }

    fun update(additionalSources: List<String>, disabledExtensionIds: Set<String>) {
        val normalizedSources = additionalSources
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .distinct()
        val normalizedDisabled = disabledExtensionIds
            .asSequence()
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .map { it.lowercase(Locale.ROOT) }
            .toSet()
            .toMutableList()

        val nextState = State(
            disabledExtensionIds = normalizedDisabled,
            additionalSources = normalizedSources.toMutableList()
        )
        if (nextState != state) {
            state = nextState
            incModificationCount()
        }
    }
}

