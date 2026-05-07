package dev.rambris.amigaamos.lang.amos

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.openapi.project.Project
import java.util.Locale

object AmosFunctionRegistry {
    private data class CallFrame(
        val name: String?,
        val leftParenthesisOffset: Int,
        var currentParameterIndex: Int
    )

    fun signaturesFor(name: String): List<AmosFunctionSignature> = signaturesFor(name, null)

    fun signaturesFor(name: String, project: Project?): List<AmosFunctionSignature> {
        return AmosDefinitionRegistry.definitionFor(name, AmosDefinitionKind.FUNCTION, project)?.signatures.orEmpty()
    }

    fun hasSignatures(name: String): Boolean = hasSignatures(name, null)

    fun hasSignatures(name: String, project: Project?): Boolean = signaturesFor(name, project).isNotEmpty()

    fun requiresParentheses(name: String): Boolean = requiresParentheses(name, null)

    fun requiresParentheses(name: String, project: Project?): Boolean {
        return signaturesFor(name, project).any { it.presentation.contains('(') }
    }

    fun returnTypeOf(name: String): AmosValueType? = returnTypeOf(name, null)

    fun returnTypeOf(name: String, project: Project?): AmosValueType? {
        return AmosDefinitionRegistry.definitionFor(name, AmosDefinitionKind.FUNCTION, project)?.returnType
    }

    fun signaturesReturning(type: AmosValueType): Set<String> = signaturesReturning(type, null)

    fun signaturesReturning(type: AmosValueType, project: Project?): Set<String> {
        return AmosDefinitionRegistry.definitionsByKind(AmosDefinitionKind.FUNCTION, project)
            .filter { definition ->
                val actual = definition.returnType ?: return@filter false
                actual == type || (type == AmosValueType.NUMERIC && (actual == AmosValueType.INTEGER || actual == AmosValueType.REAL))
            }
            .map { it.uppercaseName }
            .toSet()
    }

    fun expectedParameterType(functionName: String, parameterIndex: Int): AmosValueType? {
        return expectedParameterType(functionName, parameterIndex, null)
    }

    fun expectedParameterType(functionName: String, parameterIndex: Int, project: Project?): AmosValueType? {
        return expectedParameterSpec(functionName, parameterIndex, project)?.valueType
    }

    fun expectedParameterSpec(functionName: String, parameterIndex: Int): AmosParameterSpec? {
        return expectedParameterSpec(functionName, parameterIndex, null)
    }

    fun expectedParameterSpec(functionName: String, parameterIndex: Int, project: Project?): AmosParameterSpec? {
        val options = signaturesFor(functionName, project)
            .mapNotNull { it.parameters.getOrNull(parameterIndex) }
            .distinctBy { Triple(it.kind, it.valueType, it.keyword) }
        if (options.isEmpty()) {
            return null
        }
        return if (options.size == 1) options.first() else AmosParameterSpec(
            kind = AmosParameterKind.VALUE,
            name = null,
            valueType = AmosValueType.ANY,
            keyword = null,
            optional = false,
            documentation = null
        )
    }

    fun signaturesForInstruction(name: String, project: Project?): List<AmosFunctionSignature> {
        return AmosDefinitionRegistry.definitionFor(name, AmosDefinitionKind.INSTRUCTION, project)?.signatures.orEmpty()
    }

    fun findInstructionCallContext(source: String, offset: Int, project: Project?): AmosFunctionCallContext? {
        val clampedOffset = offset.coerceIn(0, source.length)

        val instructionNames = AmosDefinitionRegistry.definitions(project)
            .asSequence()
            .filter { it.kind == AmosDefinitionKind.INSTRUCTION && it.signatures.isNotEmpty() }
            .map { it.uppercaseName }
            .toHashSet()

        val lexer = AmosLexer()
        lexer.start(source, 0, clampedOffset, 0)

        var collectingKeywords = true
        val stmtKeywords = mutableListOf<String>()
        var instructionName: String? = null
        var paramStartOffset = -1
        var parameterIndex = 0
        var inParameters = false

        fun reset() {
            collectingKeywords = true
            stmtKeywords.clear()
            instructionName = null
            paramStartOffset = -1
            parameterIndex = 0
            inParameters = false
        }

        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType
            val tokenStart = lexer.tokenStart
            val tokenText = source.substring(tokenStart, lexer.tokenEnd)

            when {
                tokenType == TokenType.WHITE_SPACE -> {
                    if ('\n' in tokenText || '\r' in tokenText) reset()
                }
                tokenType == AmosTokenTypes.operator && tokenText == ":" -> reset()
                tokenType == AmosTokenTypes.comment -> reset()
                inParameters && tokenType == AmosTokenTypes.comma -> parameterIndex++
                inParameters -> Unit
                collectingKeywords && tokenType == AmosTokenTypes.keyword -> {
                    stmtKeywords.add(tokenText.uppercase(Locale.ROOT))
                    val candidate = stmtKeywords.joinToString(" ")
                    if (candidate in instructionNames) instructionName = candidate
                }
                collectingKeywords -> {
                    collectingKeywords = false
                    if (instructionName != null) {
                        inParameters = true
                        paramStartOffset = tokenStart
                    }
                }
            }

            lexer.advance()
        }

        val finalName = instructionName ?: return null
        return AmosFunctionCallContext(
            name = finalName,
            leftParenthesisOffset = if (paramStartOffset >= 0) paramStartOffset else clampedOffset,
            currentParameterIndex = parameterIndex
        )
    }

    fun findCallContext(source: String, offset: Int): AmosFunctionCallContext? {
        val clampedOffset = offset.coerceIn(0, source.length)
        val lexer = AmosLexer()
        lexer.start(source, 0, clampedOffset, 0)

        var pendingFunctionName: String? = null
        val stack = mutableListOf<CallFrame>()
        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType
            val tokenText = source.substring(lexer.tokenStart, lexer.tokenEnd)

            when {
                tokenType == TokenType.WHITE_SPACE -> Unit
                isFunctionToken(tokenType) -> {
                    val word = tokenText.uppercase(Locale.ROOT)
                    val combined = pendingFunctionName?.let { "$it $word" }
                    pendingFunctionName = when {
                        combined != null && hasSignatures(combined) -> combined
                        hasSignatures(word) -> word
                        combined != null -> combined
                        else -> word
                    }
                }
                tokenType == AmosTokenTypes.paren && tokenText == "(" -> {
                    val name = pendingFunctionName?.takeIf { hasSignatures(it) }
                    stack += CallFrame(name, lexer.tokenStart, 0)
                    pendingFunctionName = null
                }
                tokenType == AmosTokenTypes.paren && tokenText == ")" -> {
                    if (stack.isNotEmpty()) {
                        stack.removeLast()
                    }
                    pendingFunctionName = null
                }
                tokenType == AmosTokenTypes.comma -> {
                    if (stack.isNotEmpty()) {
                        stack.last().currentParameterIndex += 1
                    }
                    pendingFunctionName = null
                }
                else -> pendingFunctionName = null
            }

            lexer.advance()
        }

        val frame = stack.lastOrNull { it.name != null } ?: return null
        return AmosFunctionCallContext(
            name = frame.name!!,
            leftParenthesisOffset = frame.leftParenthesisOffset,
            currentParameterIndex = frame.currentParameterIndex
        )
    }

    private fun isFunctionToken(tokenType: IElementType?): Boolean {
        return tokenType == AmosTokenTypes.keyword ||
            tokenType == AmosTokenTypes.identifier ||
            tokenType == AmosTokenTypes.stringVariable ||
            tokenType == AmosTokenTypes.floatVariable
    }
}




