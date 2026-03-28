package dev.rambris.amigaamos.lang

import com.intellij.openapi.project.Project
import com.intellij.psi.TokenType

object AmosInstructionRegistry {
    fun hasSignatures(name: String): Boolean = hasSignatures(name, null)

    fun hasSignatures(name: String, project: Project?): Boolean = signaturesFor(name, project).isNotEmpty()

    fun signaturesFor(name: String): List<String> = signaturesFor(name, null)

    fun signaturesFor(name: String, project: Project?): List<String> {
        val definition = AmosDefinitionRegistry.definitionFor(name, AmosDefinitionKind.INSTRUCTION, project)
            ?: AmosDefinitionRegistry.definitionFor(name, AmosDefinitionKind.STRUCTURE, project)
            ?: return emptyList()

        return definition.signatures.map { signature ->
            signature.presentation.substringAfterDefinitionName(definition.name).trimStart()
        }
    }

    fun expectedParameterType(source: String, offset: Int): AmosValueType? = expectedParameterType(source, offset, null)

    fun expectedParameterType(source: String, offset: Int, project: Project?): AmosValueType? {
        return expectedParameterSpec(source, offset, project)?.valueType
    }

    fun expectedParameterSpec(source: String, offset: Int): AmosParameterSpec? = expectedParameterSpec(source, offset, null)

    fun expectedParameterSpec(source: String, offset: Int, project: Project?): AmosParameterSpec? {
        return expectedParameter(source, offset, project)?.parameter
    }

    fun expectedParameter(source: String, offset: Int): AmosExpectedParameter? = expectedParameter(source, offset, null)

    fun expectedParameter(source: String, offset: Int, project: Project?): AmosExpectedParameter? {
        val statement = statementPrefix(source, offset)
        if (statement.isBlank()) {
            return null
        }
        val endsWithWhitespace = statement.lastOrNull()?.isWhitespace() == true

        val significantTokens = tokenizeSignificant(statement)
        if (significantTokens.isEmpty()) {
            return null
        }

        val definition = matchInstructionDefinition(significantTokens, project) ?: return null
        val remainingTokens = significantTokens.drop(commandTokenCount(definition.name))

        return definition.signatures.firstNotNullOfOrNull { signature ->
            matchSignature(definition, signature, remainingTokens, endsWithWhitespace)
        }
    }

    private fun statementPrefix(source: String, offset: Int): String {
        val clamped = offset.coerceIn(0, source.length)
        var start = clamped
        var startsAtLineStart = true
        while (start > 0) {
            val prev = source[start - 1]
            if (prev == '\n' || prev == '\r' || prev == ':') {
                startsAtLineStart = prev != ':'
                break
            }
            start--
        }
        return AmosStatementSupport.normalizeForAnalysis(source.substring(start, clamped), startsAtLineStart)
    }

    private fun tokenizeSignificant(statement: String): List<TokenSlice> {
        val tokens = mutableListOf<TokenSlice>()
        val lexer = AmosLexer()
        lexer.start(statement)
        while (lexer.tokenType != null) {
            val type = lexer.tokenType
            if (type != TokenType.WHITE_SPACE && type != AmosTokenTypes.comment) {
                tokens += TokenSlice(
                    text = statement.substring(lexer.tokenStart, lexer.tokenEnd),
                    type = type
                )
            }
            lexer.advance()
        }
        return tokens
    }

    private fun matchInstructionDefinition(tokens: List<TokenSlice>, project: Project?): AmosCallableDefinition? {
        val candidates = AmosDefinitionRegistry.definitions(project)
            .filter { it.kind == AmosDefinitionKind.INSTRUCTION || it.kind == AmosDefinitionKind.STRUCTURE }
            .sortedByDescending { commandTokenCount(it.name) }

        return candidates.firstOrNull { definition ->
            val parts = definition.uppercaseName.split(' ')
            if (tokens.size < parts.size) {
                return@firstOrNull false
            }
            parts.indices.all { index -> tokens[index].text.equals(parts[index], ignoreCase = true) }
        }
    }

    private fun commandTokenCount(name: String): Int = name.split(' ').size

    private fun matchSignature(
        definition: AmosCallableDefinition,
        signature: AmosFunctionSignature,
        tokens: List<TokenSlice>,
        endsWithWhitespace: Boolean
    ): AmosExpectedParameter? {
        var tokenIndex = 0

        signature.parameters.forEachIndexed { parameterIndex, parameter ->
            when (parameter.kind) {
                AmosParameterKind.KEYWORD -> {
                    if (tokenIndex >= tokens.size) {
                        return AmosExpectedParameter(definition, signature, parameterIndex, parameter)
                    }

                    val token = tokens[tokenIndex]
                    val keyword = parameter.keyword ?: return null
                    if (token.isWord && keyword.startsWith(token.text, ignoreCase = true)) {
                        if (token.text.equals(keyword, ignoreCase = true)) {
                            tokenIndex++
                            return@forEachIndexed
                        }
                        return AmosExpectedParameter(definition, signature, parameterIndex, parameter)
                    }

                    return null
                }

                AmosParameterKind.VALUE -> {
                    if (tokenIndex >= tokens.size) {
                        return AmosExpectedParameter(definition, signature, parameterIndex, parameter)
                    }

                    val nextKeyword = signature.parameters.drop(parameterIndex + 1)
                        .firstOrNull { it.kind == AmosParameterKind.KEYWORD }
                        ?.keyword
                    val consumed = consumeValue(tokens, tokenIndex, nextKeyword)
                    if (consumed.nextIndex == tokenIndex) {
                        return AmosExpectedParameter(definition, signature, parameterIndex, parameter)
                    }

                    if (consumed.reachedEnd) {
                        val nextParameter = signature.parameters.getOrNull(parameterIndex + 1)
                        if (endsWithWhitespace && nextParameter?.kind == AmosParameterKind.KEYWORD) {
                            return AmosExpectedParameter(definition, signature, parameterIndex + 1, nextParameter)
                        }
                        return AmosExpectedParameter(definition, signature, parameterIndex, parameter)
                    }

                    tokenIndex = consumed.nextIndex
                    if (consumed.delimiter == Delimiter.COMMA) {
                        tokenIndex++
                    }
                }
            }
        }

        return null
    }

    private fun consumeValue(tokens: List<TokenSlice>, startIndex: Int, nextKeyword: String?): ConsumedValue {
        var index = startIndex
        var depth = 0

        while (index < tokens.size) {
            val token = tokens[index]
            when {
                token.isOpenParen -> depth++
                token.isCloseParen && depth > 0 -> depth--
                depth == 0 && token.isComma -> return ConsumedValue(index, false, Delimiter.COMMA)
                depth == 0 && nextKeyword != null && token.isWord && token.text.equals(nextKeyword, ignoreCase = true) -> {
                    return ConsumedValue(index, false, Delimiter.KEYWORD)
                }
            }
            index++
        }

        return ConsumedValue(index, true, null)
    }

    private data class TokenSlice(
        val text: String,
        val type: com.intellij.psi.tree.IElementType?
    ) {
        val isWord: Boolean get() = type == AmosTokenTypes.keyword || type == AmosTokenTypes.identifier || type == AmosTokenTypes.stringVariable || type == AmosTokenTypes.floatVariable
        val isComma: Boolean get() = type == AmosTokenTypes.comma && text == ","
        val isOpenParen: Boolean get() = type == AmosTokenTypes.paren && text == "("
        val isCloseParen: Boolean get() = type == AmosTokenTypes.paren && text == ")"
    }

    private data class ConsumedValue(
        val nextIndex: Int,
        val reachedEnd: Boolean,
        val delimiter: Delimiter?
    )

    private enum class Delimiter {
        COMMA,
        KEYWORD
    }

    private fun String.substringAfterDefinitionName(definitionName: String): String {
        return if (startsWith(definitionName, ignoreCase = true)) substring(definitionName.length) else this
    }
}






