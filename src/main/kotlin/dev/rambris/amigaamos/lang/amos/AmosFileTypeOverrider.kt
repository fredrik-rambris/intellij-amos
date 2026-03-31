package dev.rambris.amigaamos.lang.amos

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile

class AmosFileTypeOverrider : FileTypeRegistry.FileTypeDetector {

    private val extensions = setOf("asc", "amosasc", "amosbas")

    override fun detect(file: VirtualFile, firstBytes: com.intellij.openapi.util.io.ByteSequence, firstCharsIfText: CharSequence?): FileType? {
        return if (file.extension?.lowercase() in extensions) AmosFileType else null
    }
}

