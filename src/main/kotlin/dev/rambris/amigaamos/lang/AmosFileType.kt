package dev.rambris.amigaamos.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object AmosFileType : LanguageFileType(AmosLanguage) {
    override fun getName(): String = "AMOS File"

    override fun getDescription(): String = "AMOS Professional Basic ASCII export"

    override fun getDefaultExtension(): String = "asc"

    override fun getIcon(): Icon? = null
}

