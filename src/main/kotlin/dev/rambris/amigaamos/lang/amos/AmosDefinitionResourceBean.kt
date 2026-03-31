package dev.rambris.amigaamos.lang.amos

import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.util.xmlb.annotations.Attribute

class AmosDefinitionResourceBean : AbstractExtensionPointBean() {
    @Attribute("resourcePath")
    var resourcePath: String = ""

    fun toSource(): AmosDefinitionSource? {
        if (resourcePath.isBlank()) {
            return null
        }
        val classLoader = pluginDescriptor?.pluginClassLoader ?: javaClass.classLoader
        return amosClasspathDefinitionSource(
            resourcePath = resourcePath,
            classLoader = classLoader,
            origin = pluginId?.idString ?: "unknown-plugin"
        )
    }
}

