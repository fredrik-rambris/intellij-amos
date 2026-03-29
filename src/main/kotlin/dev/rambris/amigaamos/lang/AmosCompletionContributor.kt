package dev.rambris.amigaamos.lang

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.ProcessingContext
import java.util.Locale

class AmosCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            psiElement().withLanguage(AmosLanguage),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val elementType = parameters.position.node?.elementType
                    if (elementType == AmosTokenTypes.comment || elementType == AmosTokenTypes.string) {
                        return
                    }

                    // Some completion invocations happen from a synthetic leaf near the actual token.
                    val leafAtOffset = parameters.originalFile.findElementAt(parameters.offset)
                    val leafType = leafAtOffset?.node?.elementType
                    if (leafType == AmosTokenTypes.comment || leafType == AmosTokenTypes.string) {
                        return
                    }

                    val source = parameters.originalFile.text
                    val project = parameters.position.project
                    val callContext = AmosFunctionRegistry.findCallContext(source, parameters.offset)
                    val expectedInstructionParameter = if (callContext == null) {
                        AmosInstructionRegistry.expectedParameter(source, parameters.offset, project)
                    } else {
                        null
                    }
                    val expectedParameter = callContext?.let {
                        AmosFunctionRegistry.expectedParameterSpec(it.name, it.currentParameterIndex, project)
                    } ?: expectedInstructionParameter?.parameter
                    val expectedType = expectedParameter?.valueType

                    if (expectedParameter?.kind == AmosParameterKind.KEYWORD) {
                        val keyword = expectedParameter.keyword
                        if (!keyword.isNullOrBlank()) {
                            result.addElement(
                                PrioritizedLookupElement.withPriority(
                                    LookupElementBuilder.create(keyword)
                                        .withTypeText("keyword", true)
                                        .withCaseSensitivity(false),
                                    200.0
                                )
                            )
                        }
                        return
                    }

                    if (isJumpTargetParameter(expectedInstructionParameter)) {
                        AmosStatementSupport.declaredLabels(source).forEach { label ->
                            val labelBuilder = LookupElementBuilder.create(label)
                                .withTypeText("label", true)
                                .withCaseSensitivity(false)
                                .let { base -> if (label.all(Char::isDigit)) base else base.withTailText(":", false) }

                            result.addElement(
                                PrioritizedLookupElement.withPriority(
                                    labelBuilder,
                                    150.0
                                )
                            )
                        }
                    }

                    collectVisibleVariablesBeforeOffset(source, parameters.offset)
                        .filter { expectedType == null || matchesExpectedType(it.type, expectedType) }
                        .forEach { variable ->
                        result.addElement(
                            PrioritizedLookupElement.withPriority(
                                LookupElementBuilder.create(variable.name)
                                    .withTypeText("variable", true)
                                    .withCaseSensitivity(false),
                                100.0
                            )
                        )
                    }

                    AmosDefinitionRegistry.definitions(project)
                        .forEach { definition ->
                            if (expectedType != null && definition.kind == AmosDefinitionKind.FUNCTION) {
                                val returnType = definition.returnType
                                if (returnType == null || !matchesExpectedType(returnType, expectedType)) {
                                    return@forEach
                                }
                            }
                            if (expectedType != null && definition.kind != AmosDefinitionKind.FUNCTION) {
                                return@forEach
                            }

                            val formattedName = definition.formattedName()
                            val builder = LookupElementBuilder.create(definition.name)
                                .withTypeText(definition.kind.name.lowercase(Locale.ROOT), true)
                                .withCaseSensitivity(false)
                                .let { base ->
                                    if (definition.kind == AmosDefinitionKind.FUNCTION) {
                                        val withTail = base.withTailText(
                                            definition.signatures.joinToString(" | ") { " ${it.presentation.substringAfter(it.name)}" },
                                            true
                                        )
                                        if (AmosFunctionRegistry.requiresParentheses(definition.name, project)) {
                                            withTail.withInsertHandler(FunctionParensInsertHandler(formattedName))
                                        } else {
                                            withTail.withInsertHandler(SpaceInsertHandler(formattedName))
                                        }
                                    } else {
                                        base.withTailText(
                                            definition.signatures.joinToString(" | ") {
                                                " ${it.presentation.substringAfterDefinitionName(definition.name).trimStart()}"
                                            },
                                            true
                                        ).withInsertHandler(SpaceInsertHandler(formattedName))
                                    }
                                }

                            result.addElement(builder)
                        }
                }
            }
        )
    }

    private data class VariableCandidate(val name: String, val type: AmosValueType)

    private data class ScopeState(
        val rootVariables: LinkedHashMap<String, VariableCandidate> = linkedMapOf(),
        val globalsEverywhere: LinkedHashSet<String> = linkedSetOf(),
        var insideProcedure: Boolean = false,
        val procedureLocalVariables: LinkedHashMap<String, VariableCandidate> = linkedMapOf(),
        val procedureImportedGlobals: LinkedHashSet<String> = linkedSetOf()
    )

    private fun collectVisibleVariablesBeforeOffset(source: String, offset: Int): List<VariableCandidate> {
        val prefix = source.substring(0, offset.coerceIn(0, source.length))
        val state = ScopeState()

        AmosStatementSupport.splitStatements(prefix).forEach { statement ->
            val normalized = AmosStatementSupport.normalizeForAnalysis(statement)
            if (normalized.isBlank()) {
                return@forEach
            }

            if (statement.startsAtLineStart && startsWithWord(normalized, "PROCEDURE")) {
                state.insideProcedure = true
                state.procedureLocalVariables.clear()
                state.procedureImportedGlobals.clear()
                collectProcedureParameters(normalized).forEach { candidate ->
                    state.procedureLocalVariables.putIfAbsent(candidate.name, candidate)
                }
                return@forEach
            }

            if (statement.startsAtLineStart && startsWithWords(normalized, "END", "PROC")) {
                state.insideProcedure = false
                state.procedureLocalVariables.clear()
                state.procedureImportedGlobals.clear()
                return@forEach
            }

            if (state.insideProcedure) {
                if (startsWithWord(normalized, "GLOBAL") || startsWithWord(normalized, "SHARED")) {
                    collectDeclarationVariableNames(normalized).forEach { state.procedureImportedGlobals += it }
                }
                collectVariableCandidates(normalized).forEach { candidate ->
                    state.procedureLocalVariables.putIfAbsent(candidate.name, candidate)
                }
            } else {
                if (startsWithWord(normalized, "GLOBAL")) {
                    collectDeclarationVariableNames(normalized).forEach { state.globalsEverywhere += it }
                }
                collectVariableCandidates(normalized).forEach { candidate ->
                    state.rootVariables.putIfAbsent(candidate.name, candidate)
                }
            }
        }

        if (!state.insideProcedure) {
            return state.rootVariables.values.toList()
        }

        val visible = linkedMapOf<String, VariableCandidate>()
        state.procedureLocalVariables.forEach { (name, candidate) -> visible.putIfAbsent(name, candidate) }

        val importedGlobalNames = linkedSetOf<String>()
        importedGlobalNames += state.globalsEverywhere
        importedGlobalNames += state.procedureImportedGlobals

        importedGlobalNames.forEach { name ->
            val candidate = state.rootVariables[name] ?: return@forEach
            visible.putIfAbsent(name, candidate)
        }

        return visible.values.toList()
    }

    private fun collectProcedureParameters(statement: String): List<VariableCandidate> {
        val openBracket = statement.indexOf('[')
        if (openBracket < 0) {
            return emptyList()
        }

        val closeBracket = statement.lastIndexOf(']')
        if (closeBracket <= openBracket) {
            return emptyList()
        }

        return collectVariableCandidates(statement.substring(openBracket + 1, closeBracket))
    }

    private fun collectDeclarationVariableNames(statement: String): Set<String> {
        val lexer = AmosLexer()
        lexer.start(statement)

        var consumedDeclarationKeyword = false
        val names = linkedSetOf<String>()
        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType
            val tokenText = statement.substring(lexer.tokenStart, lexer.tokenEnd)

            if (!consumedDeclarationKeyword && tokenType == AmosTokenTypes.keyword) {
                consumedDeclarationKeyword = tokenText.equals("GLOBAL", ignoreCase = true) ||
                    tokenText.equals("SHARED", ignoreCase = true)
                lexer.advance()
                continue
            }

            if (consumedDeclarationKeyword && isVariableToken(tokenType)) {
                names += tokenText.uppercase(Locale.ROOT)
            }

            lexer.advance()
        }

        return names
    }

    private fun collectVariableCandidates(source: String): List<VariableCandidate> {
        val lexer = AmosLexer()
        lexer.start(source)

        val variables = linkedMapOf<String, VariableCandidate>()
        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType
            if (isVariableToken(tokenType)) {
                val text = source.substring(lexer.tokenStart, lexer.tokenEnd)
                val canonicalName = text.uppercase(Locale.ROOT)
                val candidateType = when (tokenType) {
                    AmosTokenTypes.stringVariable -> AmosValueType.STRING
                    AmosTokenTypes.floatVariable -> AmosValueType.REAL
                    else -> AmosValueType.INTEGER
                }
                variables.putIfAbsent(canonicalName, VariableCandidate(canonicalName, candidateType))
            }
            lexer.advance()
        }

        return variables.values.toList()
    }

    private fun startsWithWord(statement: String, keyword: String): Boolean {
        val regex = Regex("^\\s*${Regex.escape(keyword)}\\b", setOf(RegexOption.IGNORE_CASE))
        return regex.containsMatchIn(statement)
    }

    private fun startsWithWords(statement: String, first: String, second: String): Boolean {
        val regex = Regex(
            "^\\s*${Regex.escape(first)}\\b\\s+${Regex.escape(second)}\\b",
            setOf(RegexOption.IGNORE_CASE)
        )
        return regex.containsMatchIn(statement)
    }

    private fun isVariableToken(tokenType: IElementType?): Boolean {
        return tokenType == AmosTokenTypes.identifier ||
            tokenType == AmosTokenTypes.stringVariable ||
            tokenType == AmosTokenTypes.floatVariable
    }

    private fun matchesExpectedType(actual: AmosValueType, expected: AmosValueType): Boolean {
        return when (expected) {
            AmosValueType.ANY -> true
            AmosValueType.NUMERIC -> actual == AmosValueType.INTEGER || actual == AmosValueType.REAL
            else -> actual == expected
        }
    }

    private fun isJumpTargetParameter(expected: AmosExpectedParameter?): Boolean {
        if (expected == null || expected.parameterIndex != 0) {
            return false
        }

        return expected.definition.uppercaseName == "GOTO" || expected.definition.uppercaseName == "GOSUB"
    }

    private fun String.substringAfterDefinitionName(definitionName: String): String {
        return if (startsWith(definitionName, ignoreCase = true)) substring(definitionName.length) else this
    }

    private fun AmosCallableDefinition.formattedName(): String =
        signatures.firstOrNull()?.presentation?.take(name.length)
            ?: AmosCodeStyleFormatter.formatLine(name)

    private class SpaceInsertHandler(private val formattedName: String) :
        InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {

        override fun handleInsert(context: InsertionContext, item: com.intellij.codeInsight.lookup.LookupElement) {
            val document = context.document
            val insertSpace = context.completionChar != ' '
            val inserted = if (insertSpace) "$formattedName " else formattedName
            document.replaceString(context.startOffset, context.tailOffset, inserted)
            context.editor.caretModel.moveToOffset(context.startOffset + inserted.length)
            context.commitDocument()
        }
    }

    private class FunctionParensInsertHandler(private val formattedName: String) :
        InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {

        override fun handleInsert(context: InsertionContext, item: com.intellij.codeInsight.lookup.LookupElement) {
            val document = context.document
            document.replaceString(context.startOffset, context.tailOffset, formattedName)
            val newTailOffset = context.startOffset + formattedName.length
            val hasOpeningParen =
                newTailOffset < document.textLength && document.charsSequence[newTailOffset] == '('
            if (!hasOpeningParen) {
                document.insertString(newTailOffset, "()")
            }
            context.editor.caretModel.moveToOffset(newTailOffset + 1)
            context.commitDocument()
            AutoPopupController.getInstance(context.project)
                .autoPopupParameterInfo(
                    context.editor,
                    context.file.findElementAt((newTailOffset - 1).coerceAtLeast(0))
                )
        }
    }
}
