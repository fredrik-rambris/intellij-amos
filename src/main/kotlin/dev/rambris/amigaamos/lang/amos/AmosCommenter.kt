package dev.rambris.amigaamos.lang.amos

import com.intellij.lang.Commenter

class AmosCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "'"

    override fun getBlockCommentPrefix(): String? = null

    override fun getBlockCommentSuffix(): String? = null

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}
