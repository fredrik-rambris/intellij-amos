package dev.rambris.amigaamos.lang.amui
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon
object AmuiFileType : LanguageFileType(AmuiLanguage) {
    override fun getName(): String = "AMOS Interface File"
    override fun getDescription(): String = "AMOS Interface Language source"
    override fun getDefaultExtension(): String = "amui"
    override fun getIcon(): Icon? = null
}
