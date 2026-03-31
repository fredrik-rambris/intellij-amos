package dev.rambris.amigaamos.lang.amal
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon
object AmalFileType : LanguageFileType(AmalLanguage) {
    override fun getName(): String = "AMAL File"
    override fun getDescription(): String = "AMOS Animation Language source"
    override fun getDefaultExtension(): String = "amal"
    override fun getIcon(): Icon? = null
}
