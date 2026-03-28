package dev.rambris.amigaamos.lang

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

object AmosRenameSupport {
    data class SymbolTarget(
        val declarationOffset: Int,
        val currentName: String,
        val tokenType: IElementType,
        val canonicalName: String,
        val resolveByDeclaration: Boolean
    )

    fun promptAndRenameAtCaret(editor: Editor, file: PsiFile): Boolean {
        val target = findTarget(file, editor.caretModel.offset) ?: return false
        val newName = Messages.showInputDialog(
            file.project,
            "Rename '${target.currentName}' to:",
            "Rename AMOS Symbol",
            Messages.getQuestionIcon(),
            target.currentName,
            null
        ) ?: return false

        return renameAtOffset(file, editor.caretModel.offset, newName)
    }

    fun renameAtOffset(file: PsiFile, offset: Int, newName: String): Boolean {
        val target = findTarget(file, offset) ?: return false
        val normalizedNewName = newName.trim()
        if (!isValidNewName(target, normalizedNewName)) {
            return false
        }

        val source = file.text
        val ranges = collectRangesForDeclaration(source, target)
        if (ranges.isEmpty()) {
            return false
        }

        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return false
        WriteCommandAction.runWriteCommandAction(file.project) {
            ranges.sortedByDescending { it.first }.forEach { (start, end) ->
                document.replaceString(start, end, normalizedNewName)
            }
            PsiDocumentManager.getInstance(file.project).commitDocument(document)
        }

        return true
    }

    fun renameAtOffsetPreview(file: PsiFile, offset: Int): Boolean {
        return findTarget(file, offset) != null
    }

    private fun findTarget(file: PsiFile, offset: Int): SymbolTarget? {
        val source = file.text
        val table = AmosSymbolIndex.build(source)
        val token = tokenAtOffset(source, offset) ?: return null
        if (!isNavigableToken(token.type)) {
            return null
        }

        val declarationOffset = table.resolveOffset(source, token.type, token.text, token.startOffset)
            ?: labelDeclarationOffsetAtToken(table, token)
            ?: firstMatchingTokenOffset(source, token)
            ?: return null

        val resolvedByDeclaration = table.resolveOffset(source, token.type, token.text, token.startOffset) != null ||
            labelDeclarationOffsetAtToken(table, token) != null

        return SymbolTarget(
            declarationOffset = declarationOffset,
            currentName = token.text,
            tokenType = token.type,
            canonicalName = token.text.uppercase(),
            resolveByDeclaration = resolvedByDeclaration
        )
    }

    private fun firstMatchingTokenOffset(source: String, token: TokenSlice): Int? {
        return tokenize(source)
            .firstOrNull { it.type == token.type && it.text.equals(token.text, ignoreCase = true) }
            ?.startOffset
    }

    private fun labelDeclarationOffsetAtToken(table: AmosSymbolIndex.SymbolTable, token: TokenSlice): Int? {
        val key = if (token.type == AmosTokenTypes.number) token.text else token.text.uppercase()
        val declarationOffset = table.labelDeclarations[key] ?: return null
        return if (declarationOffset == token.startOffset) declarationOffset else null
    }

    private fun collectRangesForDeclaration(source: String, target: SymbolTarget): List<Pair<Int, Int>> {
        val table = AmosSymbolIndex.build(source)
        val ranges = linkedSetOf<Pair<Int, Int>>()
        tokenize(source).forEach { token ->
            if (!isNavigableToken(token.type)) {
                return@forEach
            }

            val matches = if (target.resolveByDeclaration) {
                val resolved = table.resolveOffset(source, token.type, token.text, token.startOffset)
                resolved == target.declarationOffset || token.startOffset == target.declarationOffset
            } else {
                token.type == target.tokenType && token.text.equals(target.canonicalName, ignoreCase = true)
            }

            if (matches) {
                ranges += token.startOffset to token.endOffset
            }
        }

        return ranges.toList()
    }

    private fun isValidNewName(target: SymbolTarget, newName: String): Boolean {
        if (newName.isEmpty()) {
            return false
        }

        return when (target.tokenType) {
            AmosTokenTypes.stringVariable -> Regex("""[A-Za-z_][A-Za-z0-9_]*\$""").matches(newName)
            AmosTokenTypes.floatVariable -> Regex("""[A-Za-z_][A-Za-z0-9_]*#""").matches(newName)
            AmosTokenTypes.number -> newName.all(Char::isDigit)
            else -> Regex("""[A-Za-z_][A-Za-z0-9_$#]*""").matches(newName)
        }
    }

    private fun tokenAtOffset(source: String, offset: Int): TokenSlice? {
        val clamped = offset.coerceIn(0, source.length)
        val tokens = tokenize(source)
        val atCaret = tokens.firstOrNull { token ->
            clamped >= token.startOffset && clamped < token.endOffset
        }

        if (atCaret != null && isNavigableToken(atCaret.type)) {
            return atCaret
        }

        if (clamped > 0) {
            val previous = clamped - 1
            val previousToken = tokens.firstOrNull { token ->
                previous >= token.startOffset && previous < token.endOffset
            }
            if (previousToken != null && isNavigableToken(previousToken.type)) {
                return previousToken
            }
        }

        return null
    }

    private fun tokenize(source: String): List<TokenSlice> {
        val lexer = AmosLexer()
        lexer.start(source)

        val tokens = mutableListOf<TokenSlice>()
        while (lexer.tokenType != null) {
            val type = lexer.tokenType
            if (type != null) {
                tokens += TokenSlice(
                    text = source.substring(lexer.tokenStart, lexer.tokenEnd),
                    type = type,
                    startOffset = lexer.tokenStart,
                    endOffset = lexer.tokenEnd
                )
            }
            lexer.advance()
        }

        return tokens
    }

    private fun isNavigableToken(type: IElementType): Boolean {
        return type == AmosTokenTypes.identifier ||
            type == AmosTokenTypes.stringVariable ||
            type == AmosTokenTypes.floatVariable ||
            type == AmosTokenTypes.number
    }

    private data class TokenSlice(
        val text: String,
        val type: IElementType,
        val startOffset: Int,
        val endOffset: Int
    )
}





