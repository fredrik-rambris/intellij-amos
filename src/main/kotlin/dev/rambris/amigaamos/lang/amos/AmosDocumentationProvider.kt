package dev.rambris.amigaamos.lang.amos

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import java.util.Locale

class AmosDocumentationProvider : AbstractDocumentationProvider() {
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (file.language != AmosLanguage) {
            return null
        }

        val atOffset = file.findElementAt(targetOffset)
        return nearestWordElement(atOffset ?: contextElement ?: return null)
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val sourceElement = originalElement ?: element ?: return null
        if (sourceElement.containingFile?.language != AmosLanguage) {
            return null
        }

        val definition = resolveDefinition(sourceElement) ?: return null
        return buildDoc(definition)
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        val sourceElement = originalElement ?: element ?: return null
        if (sourceElement.containingFile?.language != AmosLanguage) {
            return null
        }

        val definition = resolveDefinition(sourceElement) ?: return null
        val kind = definition.kind.name.lowercase(Locale.ROOT)
        return "${definition.name} ($kind)"
    }

    private fun resolveDefinition(sourceElement: PsiElement): AmosCallableDefinition? {
        val current = nearestWordElement(sourceElement) ?: return null
        val previous = PsiTreeUtil.prevVisibleLeaf(current)?.takeIf { it.isWordLike() }
        val previousPrevious = previous?.let { PsiTreeUtil.prevVisibleLeaf(it)?.takeIf { leaf -> leaf.isWordLike() } }
        val next = PsiTreeUtil.nextVisibleLeaf(current)?.takeIf { it.isWordLike() }
        val nextNext = next?.let { PsiTreeUtil.nextVisibleLeaf(it)?.takeIf { leaf -> leaf.isWordLike() } }

        val word = current.text
        val candidates = buildList {
            previous?.let { add("${it.text} $word") }
            next?.let { add("$word ${it.text}") }
            if (previousPrevious != null) {
                add("${previousPrevious.text} ${previous.text} $word")
            }
            if (nextNext != null) {
                add("$word ${next.text} ${nextNext.text}")
            }
            add(word)
        }

        return candidates
            .asSequence()
            .distinct()
            .mapNotNull { candidate -> AmosDefinitionRegistry.definitionFor(candidate, null, sourceElement.project) }
            .firstOrNull()
    }

    private fun nearestWordElement(start: PsiElement): PsiElement? {
        if (start.isWordLike()) {
            return start
        }

        return PsiTreeUtil.prevVisibleLeaf(start)?.takeIf { it.isWordLike() }
            ?: PsiTreeUtil.nextVisibleLeaf(start)?.takeIf { it.isWordLike() }
    }

    private fun PsiElement.isWordLike(): Boolean {
        val tokenType = node?.elementType
        return tokenType == AmosTokenTypes.keyword ||
            tokenType == AmosTokenTypes.identifier ||
            tokenType == AmosTokenTypes.stringVariable ||
            tokenType == AmosTokenTypes.floatVariable ||
            (tokenType != null && AmosTokenTypes.allBlockKeywords.contains(tokenType))
    }

    private fun buildDoc(definition: AmosCallableDefinition): String {
        val kind = definition.kind.name.lowercase(Locale.ROOT)
        val summary = definition.documentation ?: "No documentation available."
        val link = definition.link?.toString()

        val signaturesHtml = if (definition.signatures.isEmpty()) {
            ""
        } else {
            val signatureRows = definition.signatures.joinToString("<br>") { signature ->
                "<code>${escapeHtml(signature.presentation)}</code>"
            }
            "<h3>signatures</h3><p>$signatureRows</p>"
        }

        val returnTypeHtml = definition.returnType?.let { type ->
            "<p><b>returns:</b> ${type.name.lowercase(Locale.ROOT)}</p>"
        }.orEmpty()

        val extensionHtml = definition.extensionName?.let { extensionName ->
            "<p><b>extension:</b> ${escapeHtml(extensionName)}</p>"
        }.orEmpty()

        val linkHtml = link?.let {
            "<p><b>documentation:</b> <a href=\"${escapeHtml(it)}\">${escapeHtml(it)}</a></p>"
        }.orEmpty()

        return buildString {
            append("<h2>")
            append(escapeHtml(definition.name))
            append("</h2>")
            append("<p><b>kind:</b> ")
            append(escapeHtml(kind))
            append("</p>")
            append(returnTypeHtml)
            append("<p>")
            append(escapeHtml(summary))
            append("</p>")
            append(signaturesHtml)
            append(extensionHtml)
            append(linkHtml)
        }
    }


    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}


