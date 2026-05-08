package dev.rambris.amigaamos.lang.amos

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.ProcessingContext
import java.util.Locale

class AmosReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: com.intellij.psi.PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            psiElement(),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val file = element.containingFile as? AmosFile ?: return PsiReference.EMPTY_ARRAY
                    val tokenType = element.node?.elementType ?: return PsiReference.EMPTY_ARRAY
                    if (!isNavigableToken(tokenType)) {
                        return PsiReference.EMPTY_ARRAY
                    }

                    val name = element.text.trim()
                    if (name.isEmpty()) {
                        return PsiReference.EMPTY_ARRAY
                    }

                    val source = file.text
                    val symbolIndex = AmosSymbolIndex.build(source)
                    val declarationOffset = symbolIndex.resolveOffset(
                        source = source,
                        tokenType = tokenType,
                        name = name,
                        elementOffset = element.textRange.startOffset
                    ) ?: return PsiReference.EMPTY_ARRAY

                    if (declarationOffset == element.textRange.startOffset) {
                        return PsiReference.EMPTY_ARRAY
                    }

                    return arrayOf(
                        AmosSymbolReference(
                            element = element,
                            declarationOffset = declarationOffset
                        )
                    )
                }
            }
        )
    }

    private fun isNavigableToken(type: IElementType): Boolean {
        return type == AmosTokenTypes.identifier ||
            type == AmosTokenTypes.keyword ||
            type == AmosTokenTypes.stringVariable ||
            type == AmosTokenTypes.floatVariable ||
            type == AmosTokenTypes.number
    }
}

private class AmosSymbolReference(
    element: PsiElement,
    private val declarationOffset: Int
) : PsiReferenceBase<PsiElement>(element, TextRange(0, element.textLength), false) {
    override fun resolve(): PsiElement? {
        return element.containingFile.findElementAt(declarationOffset)
    }

    override fun isReferenceTo(target: PsiElement): Boolean {
        return resolve() == target
    }
}

internal object AmosSymbolIndex {
    private val symbolicLabelPattern = Regex("""[A-Za-z_][A-Za-z0-9_$#]*""")
    private val numberedLinePattern = Regex("""^(\d+)([ \t]+)(.+)$""")

    fun build(source: String): SymbolTable {
        val procedureDeclarations = linkedMapOf<String, Int>()
        val labelDeclarations = linkedMapOf<String, Int>()
        val variableDeclarations = linkedMapOf<String, Int>()

        AmosStatementSupport.splitStatements(source).forEach { statement ->
            val statementText = statement.text

            if (statement.startsAtLineStart) {
                collectLabelDeclarations(statement, labelDeclarations)
            }

            val normalized = AmosStatementSupport.normalizeForAnalysis(statement)
            if (normalized.isBlank()) {
                return@forEach
            }

            val tokenSlices = tokenizeStatement(statementText, statement.startOffset)
            if (tokenSlices.isEmpty()) {
                return@forEach
            }

            collectProcedureDeclaration(tokenSlices, procedureDeclarations)
            collectVariablesFromDeclarationKeywords(tokenSlices, variableDeclarations)
            collectVariablesFromProcedureParameters(tokenSlices, variableDeclarations)
            collectVariablesFromAssignment(tokenSlices, variableDeclarations)
        }

        return SymbolTable(
            procedureDeclarations = procedureDeclarations,
            labelDeclarations = labelDeclarations,
            variableDeclarations = variableDeclarations
        )
    }

    private fun collectLabelDeclarations(
        statement: AmosStatementSegment,
        labels: MutableMap<String, Int>
    ) {
        val trimmed = statement.text.trimStart()
        if (trimmed.isEmpty()) {
            return
        }

        if (statement.endsWithColonSeparator) {
            val labelText = trimmed
            if (symbolicLabelPattern.matches(labelText) && labelText.uppercase(Locale.ROOT) !in AmosCommandIndex.keywordsByWord) {
                val leading = statement.text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                labels.putIfAbsent(labelText.uppercase(Locale.ROOT), statement.startOffset + leading)
                return
            }
        }

        val numbered = numberedLinePattern.matchEntire(trimmed)
        if (numbered != null) {
            val leading = statement.text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
            labels.putIfAbsent(numbered.groupValues[1], statement.startOffset + leading)
        }
    }

    private fun collectProcedureDeclaration(tokens: List<TokenSlice>, procedures: MutableMap<String, Int>) {
        val firstWordIndex = tokens.indexOfFirst { it.isWord }
        if (firstWordIndex < 0 || !tokens[firstWordIndex].text.equals("PROCEDURE", ignoreCase = true)) {
            return
        }

        val nameToken = tokens.drop(firstWordIndex + 1).firstOrNull { it.isWord } ?: return
        procedures.putIfAbsent(nameToken.canonicalName, nameToken.startOffset)
    }

    private fun collectVariablesFromDeclarationKeywords(
        tokens: List<TokenSlice>,
        variables: MutableMap<String, Int>
    ) {
        val firstWordIndex = tokens.indexOfFirst { it.isWord }
        if (firstWordIndex < 0) {
            return
        }

        val keyword = tokens[firstWordIndex].text.uppercase(Locale.ROOT)
        if (keyword !in setOf("GLOBAL", "SHARED", "DIM")) {
            return
        }

        tokens.drop(firstWordIndex + 1)
            .filter { it.isVariable }
            .forEach { token -> variables.putIfAbsent(token.canonicalName, token.startOffset) }
    }

    private fun collectVariablesFromProcedureParameters(
        tokens: List<TokenSlice>,
        variables: MutableMap<String, Int>
    ) {
        val firstWord = tokens.firstOrNull { it.isWord } ?: return
        if (!firstWord.text.equals("PROCEDURE", ignoreCase = true)) {
            return
        }

        val open = tokens.indexOfFirst { it.isOpenBracket }
        val close = tokens.indexOfLast { it.isCloseBracket }
        if (open < 0 || close <= open) {
            return
        }

        tokens.subList(open + 1, close)
            .filter { it.isVariable }
            .forEach { token -> variables.putIfAbsent(token.canonicalName, token.startOffset) }
    }

    private fun collectVariablesFromAssignment(
        tokens: List<TokenSlice>,
        variables: MutableMap<String, Int>
    ) {
        val firstWordIndex = tokens.indexOfFirst { it.isWord || it.isNumber }
        if (firstWordIndex < 0) {
            return
        }

        var cursor = firstWordIndex
        if (tokens[cursor].isNumber) {
            val hasFollowingWord = tokens.drop(cursor + 1).any { it.isWord }
            if (!hasFollowingWord) {
                return
            }
            cursor = (cursor + 1 until tokens.size).firstOrNull { tokens[it].isWord } ?: -1
            if (cursor < 0) {
                return
            }
        }

        val candidate = tokens[cursor]
        if (!candidate.isVariable) {
            return
        }

        val hasEquals = tokens.drop(cursor + 1).any { it.isEquals }
        if (hasEquals) {
            variables.putIfAbsent(candidate.canonicalName, candidate.startOffset)
        }
    }

    private fun tokenizeStatement(text: String, absoluteStartOffset: Int): List<TokenSlice> {
        val tokens = mutableListOf<TokenSlice>()
        val lexer = AmosLexer()
        lexer.start(text)

        while (lexer.tokenType != null) {
            val type = lexer.tokenType
            if (type != TokenType.WHITE_SPACE && type != AmosTokenTypes.comment && type != AmosTokenTypes.string) {
                val tokenText = text.substring(lexer.tokenStart, lexer.tokenEnd)
                tokens += TokenSlice(
                    text = tokenText,
                    type = type,
                    startOffset = absoluteStartOffset + lexer.tokenStart
                )
            }
            lexer.advance()
        }

        return tokens
    }

    data class SymbolTable(
        val procedureDeclarations: Map<String, Int>,
        val labelDeclarations: Map<String, Int>,
        val variableDeclarations: Map<String, Int>
    ) {
        fun resolveOffset(source: String, tokenType: IElementType, name: String, elementOffset: Int): Int? {
            val canonicalName = name.uppercase(Locale.ROOT)

            if (isJumpTargetPosition(source, elementOffset)) {
                if (tokenType == AmosTokenTypes.number) {
                    return labelDeclarations[name]
                }
                return labelDeclarations[canonicalName]
            }

            if (isProcedureToken(tokenType) && isProcedureReferencePosition(source, tokenType, elementOffset)) {
                val procedureOffset = procedureDeclarations[canonicalName]
                if (procedureOffset != null) {
                    return procedureOffset
                }
            }

            return when (tokenType) {
                AmosTokenTypes.identifier,
                AmosTokenTypes.stringVariable,
                AmosTokenTypes.floatVariable -> variableDeclarations[canonicalName]
                else -> null
            }
        }

        fun usageOffsetsForDeclaration(source: String, declarationOffset: Int): List<Int> {
            val usages = mutableListOf<Int>()
            val lexer = AmosLexer()
            lexer.start(source)

            while (lexer.tokenType != null) {
                val tokenType = lexer.tokenType
                if (tokenType == AmosTokenTypes.identifier ||
                    tokenType == AmosTokenTypes.keyword ||
                    tokenType == AmosTokenTypes.stringVariable ||
                    tokenType == AmosTokenTypes.floatVariable ||
                    tokenType == AmosTokenTypes.number
                ) {
                    val start = lexer.tokenStart
                    if (start != declarationOffset) {
                        val text = source.substring(lexer.tokenStart, lexer.tokenEnd)
                        val resolved = resolveOffset(source, tokenType, text, start)
                        if (resolved == declarationOffset) {
                            usages += start
                        }
                    }
                }
                lexer.advance()
            }

            return usages
        }

        private fun isJumpTargetPosition(source: String, elementOffset: Int): Boolean {
            val clampedOffset = elementOffset.coerceIn(0, source.length)
            var start = clampedOffset
            var startsAtLineStart = true
            while (start > 0) {
                val previous = source[start - 1]
                if (previous == '\n' || previous == '\r' || previous == ':') {
                    startsAtLineStart = previous != ':'
                    break
                }
                start--
            }

            val prefix = AmosStatementSupport.normalizeForAnalysis(
                source.substring(start, clampedOffset),
                startsAtLineStart
            ).uppercase(Locale.ROOT)

            return prefix.startsWith("GOTO ") || prefix.startsWith("GOSUB ")
        }

        private fun isProcedureToken(tokenType: IElementType): Boolean {
            return tokenType == AmosTokenTypes.identifier || tokenType == AmosTokenTypes.keyword
        }

        private fun isProcedureReferencePosition(source: String, tokenType: IElementType, elementOffset: Int): Boolean {
            if (tokenType == AmosTokenTypes.identifier) {
                return true
            }

            val clampedOffset = elementOffset.coerceIn(0, source.length)
            var start = clampedOffset
            var startsAtLineStart = true
            while (start > 0) {
                val previous = source[start - 1]
                if (previous == '\n' || previous == '\r' || previous == ':') {
                    startsAtLineStart = previous != ':'
                    break
                }
                start--
            }

            val prefix = AmosStatementSupport.normalizeForAnalysis(
                source.substring(start, clampedOffset),
                startsAtLineStart
            ).uppercase(Locale.ROOT)

            if (prefix.startsWith("PROCEDURE ") || prefix.startsWith("PROC ")) {
                return true
            }

            val procIndex = prefix.lastIndexOf(" PROC ")
            if (procIndex < 0) {
                return false
            }

            val tailAfterProc = prefix.substring(procIndex + " PROC ".length)
            return tailAfterProc.isEmpty() || tailAfterProc.matches(Regex("""[A-Z0-9_$#,\s]+"""))
        }
    }

    private data class TokenSlice(
        val text: String,
        val type: IElementType?,
        val startOffset: Int
    ) {
        val canonicalName: String = text.uppercase(Locale.ROOT)
        val isWord: Boolean =
            type == AmosTokenTypes.identifier ||
                type == AmosTokenTypes.stringVariable ||
                type == AmosTokenTypes.floatVariable ||
                type == AmosTokenTypes.keyword ||
                (type != null && AmosTokenTypes.allBlockKeywords.contains(type))
        val isVariable: Boolean =
            type == AmosTokenTypes.identifier ||
                type == AmosTokenTypes.stringVariable ||
                type == AmosTokenTypes.floatVariable
        val isNumber: Boolean = type == AmosTokenTypes.number && text.all(Char::isDigit)
        val isEquals: Boolean = type == AmosTokenTypes.operator && text == "="
        val isOpenBracket: Boolean = text == "["
        val isCloseBracket: Boolean = text == "]"
    }
}








