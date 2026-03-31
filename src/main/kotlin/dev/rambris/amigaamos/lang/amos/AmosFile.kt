package dev.rambris.amigaamos.lang.amos

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class AmosFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AmosLanguage) {
    override fun getFileType() = AmosFileType

    override fun toString(): String = "AMOS File"
}
