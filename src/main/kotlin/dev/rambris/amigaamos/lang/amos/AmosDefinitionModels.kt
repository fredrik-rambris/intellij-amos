package dev.rambris.amigaamos.lang.amos

import com.intellij.openapi.util.TextRange
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI
import java.util.Locale

@Serializable
@SerialName("valueType")
enum class AmosValueType {
    @SerialName("string")
    STRING,

    @SerialName("integer")
    INTEGER,

    @SerialName("real")
    REAL,

    @SerialName("numeric")
    NUMERIC,

    @SerialName("any")
    ANY
}

@Serializable
@SerialName("definitionKind")
enum class AmosDefinitionKind {
    @SerialName("function")
    FUNCTION,

    @SerialName("instruction")
    INSTRUCTION,

    @SerialName("structure")
    STRUCTURE
}

@Serializable
@SerialName("parameterKind")
enum class AmosParameterKind {
    @SerialName("value")
    VALUE,

    @SerialName("keyword")
    KEYWORD
}

@Serializable
data class AmosDefinitionFile(
    val extension: AmosExtensionMetadata? = null,
    val definitions: List<AmosDefinitionEntry> = emptyList()
)

@Serializable
data class AmosExtensionMetadata(
    val id: String? = null,
    val name: String? = null,
    val filename: String? = null,
    val slot: Int? = null,
    val version: String? = null,
    val vendor: String? = null
) {
    fun extensionId(): String? {
        return (id ?: name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.lowercase(Locale.ROOT)
    }
}

@Serializable
data class AmosDefinitionEntry(
    val name: String,
    val kind: AmosDefinitionKind,
    val returnType: AmosValueType? = null,
    val documentation: String? = null,
    val link: String? = null,
    val signatures: List<AmosSignatureEntry> = emptyList()
)

@Serializable
data class AmosSignatureEntry(
    val presentation: String,
    val documentation: String? = null,
    val parameters: List<AmosParameterEntry> = emptyList()
)

@Serializable
data class AmosParameterEntry(
    val kind: AmosParameterKind = AmosParameterKind.VALUE,
    val name: String? = null,
    val valueType: AmosValueType? = null,
    val keyword: String? = null,
    val optional: Boolean = false,
    val documentation: String? = null
)

data class AmosParameterSpec(
    val kind: AmosParameterKind,
    val name: String?,
    val valueType: AmosValueType?,
    val keyword: String?,
    val optional: Boolean,
    val documentation: String?
) {
    fun displayText(): String {
        return when (kind) {
            AmosParameterKind.KEYWORD -> keyword ?: ""
            AmosParameterKind.VALUE -> name ?: valueType?.name?.lowercase(Locale.ROOT) ?: "value"
        }
    }
}

data class AmosFunctionSignature(
    val name: String,
    val presentation: String,
    val parameterRanges: List<TextRange>,
    val parameters: List<AmosParameterSpec>,
    val documentation: String?
)

data class AmosCallableDefinition(
    val name: String,
    val kind: AmosDefinitionKind,
    val returnType: AmosValueType?,
    val documentation: String?,
    val link: URI?,
    val signatures: List<AmosFunctionSignature>,
    val extensionId: String?,
    val extensionName: String?
) {
    val uppercaseName: String = name.uppercase(Locale.ROOT)
}

data class AmosFunctionCallContext(
    val name: String,
    val leftParenthesisOffset: Int,
    val currentParameterIndex: Int
)

data class AmosExpectedParameter(
    val definition: AmosCallableDefinition,
    val signature: AmosFunctionSignature,
    val parameterIndex: Int,
    val parameter: AmosParameterSpec
)

data class AmosDefinitionSource(
    val origin: String,
    val resourcePath: String? = null,
    val classLoader: ClassLoader? = null,
    val uri: String? = null,
    val allowStructures: Boolean = false
)

fun amosClasspathDefinitionSource(
    resourcePath: String,
    classLoader: ClassLoader,
    origin: String,
    allowStructures: Boolean = false
): AmosDefinitionSource {
    return AmosDefinitionSource(
        origin = origin,
        resourcePath = resourcePath,
        classLoader = classLoader,
        allowStructures = allowStructures
    )
}

fun amosUriDefinitionSource(
    uri: String,
    origin: String,
    allowStructures: Boolean = false
): AmosDefinitionSource {
    return AmosDefinitionSource(
        origin = origin,
        uri = uri,
        allowStructures = allowStructures
    )
}


