package dev.rambris.amigaamos.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class AmosParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = AmosLexer()

    override fun createParser(project: Project?): PsiParser = AmosParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = TokenSet.create(AmosTokenTypes.comment)

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(AmosTokenTypes.string)

    override fun createElement(node: ASTNode): PsiElement {
        return when (node.elementType) {
            AmosElementTypes.assignmentStatement -> AmosAssignmentStatement(node)
            AmosElementTypes.statement -> AmosStatement(node)
            else -> ASTWrapperPsiElement(node)
        }
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = AmosFile(viewProvider)

    override fun getWhitespaceTokens(): TokenSet = TokenSet.create(TokenType.WHITE_SPACE)

    companion object {
        val FILE = IFileElementType(AmosLanguage)
    }
}


