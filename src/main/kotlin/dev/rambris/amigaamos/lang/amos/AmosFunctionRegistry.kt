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
                isFunctionToken(tokenType) && hasSignatures(tokenText) -> {
                    pendingFunctionName = tokenText.uppercase(Locale.ROOT)
                }
                tokenType == AmosTokenTypes.paren && tokenText == "(" -> {
                    stack += CallFrame(pendingFunctionName, lexer.tokenStart, 0)
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




