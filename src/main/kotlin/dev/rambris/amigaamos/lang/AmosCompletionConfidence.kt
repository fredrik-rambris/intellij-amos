package dev.rambris.amigaamos.lang

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.util.ThreeState
import java.util.Locale

class AmosCompletionConfidence : CompletionConfidence() {

    override fun shouldSkipAutopopup(editor: Editor, element: PsiElement, file: PsiFile, offset: Int): ThreeState {
        if (file !is AmosFile) return ThreeState.UNSURE

        // If the parser expects a required keyword at this point (for example `To` in `For Y=5 To ...`),
        // keep auto-popup off and let explicit Ctrl-Space drive keyword completion.
        val expectedInstructionParameter = AmosInstructionRegistry.expectedParameter(file.text, offset, file.project)
        if (expectedInstructionParameter?.parameter?.kind == AmosParameterKind.KEYWORD) {
            return ThreeState.YES
        }

        val tokenType = element.node?.elementType

        // Never show popup while on a literal, operator, punctuation, string, comment or whitespace
        if (
            tokenType == AmosTokenTypes.number ||
            tokenType == AmosTokenTypes.string ||
            tokenType == AmosTokenTypes.comment ||
            tokenType == AmosTokenTypes.operator ||
            tokenType == AmosTokenTypes.comma ||
            tokenType == AmosTokenTypes.paren ||
            tokenType == TokenType.WHITE_SPACE ||
            tokenType == TokenType.BAD_CHARACTER
        ) {
            return ThreeState.YES
        }

        val tokenText = element.text
        if (tokenText.isEmpty()) return ThreeState.YES

        // Look at what came before the current token in the same statement
        val source = file.text
        val tokenStart = element.textRange.startOffset
        val stmtStart = statementStart(source, tokenStart)
        val prefix = source.substring(stmtStart, tokenStart)

        // In FOR ranges, typing `t`/`to` should not trigger broad autopopup suggestions.
        if (isPotentialForToKeywordPrefix(prefix, tokenText)) {
            return ThreeState.YES
        }

        val prevToken = lastMeaningfulToken(prefix)

        return when {
            // Nothing before → first token of statement: show after 2+ chars
            prevToken == null ->
                if (tokenText.length >= 2) ThreeState.NO else ThreeState.YES

            // After structural keywords that introduce a name (loop var, proc name)
            prevToken.uppercase(Locale.ROOT) in STRUCTURAL_INTRODUCERS ->
                ThreeState.YES

            // Everywhere else (after `=`, `,`, `(`, `To`, `Print`, arithmetic, …):
            // show after the first alphabetic character
            tokenText.first().isLetter() -> ThreeState.NO

            else -> ThreeState.YES
        }
    }

    private fun isPotentialForToKeywordPrefix(prefix: String, tokenText: String): Boolean {
        if (!"TO".startsWith(tokenText.uppercase(Locale.ROOT))) {
            return false
        }

        val normalized = prefix.trimStart().replace(Regex("\\s+"), " ")
        if (!normalized.startsWith("FOR ", ignoreCase = true)) {
            return false
        }

        // Do not treat it as a prefix if a complete TO is already present earlier.
        return !Regex("\\bto\\b", RegexOption.IGNORE_CASE).containsMatchIn(normalized)
    }

    /** Offset of the first character of the statement containing [tokenStart]. */
    private fun statementStart(source: String, tokenStart: Int): Int {
        var i = tokenStart
        while (i > 0) {
            when (source[i - 1]) {
                '\n', '\r', ':' -> return i
            }
            i--
        }
        return 0
    }

    /** Text of the last non-whitespace token in [text], or null if [text] is all whitespace. */
    private fun lastMeaningfulToken(text: String): String? {
        if (text.isBlank()) return null
        val lexer = AmosLexer()
        lexer.start(text)
        var last: String? = null
        while (lexer.tokenType != null) {
            if (lexer.tokenType != TokenType.WHITE_SPACE) {
                last = text.substring(lexer.tokenStart, lexer.tokenEnd)
            }
            lexer.advance()
        }
        return last
    }

    companion object {
        /**
         * Keywords that introduce an identifier name (not a value expression).
         * After these the user is typing a loop variable, procedure name, etc.
         * – not something we should auto-complete from the registry.
         */
        private val STRUCTURAL_INTRODUCERS = setOf("FOR", "PROCEDURE", "NEXT")
    }
}
