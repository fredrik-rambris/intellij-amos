package dev.rambris.amigaamos.lang

import com.intellij.psi.tree.IElementType

open class AmosElementType(debugName: String) : IElementType(debugName, AmosLanguage) {
    override fun toString(): String = "AmosElementType." + super.toString()
}

object AmosElementTypes {
    val statement = AmosElementType("STATEMENT")
    val assignmentStatement = AmosElementType("ASSIGNMENT_STATEMENT")
    val ifBlock = AmosElementType("IF_BLOCK")
    val forBlock = AmosElementType("FOR_BLOCK")
    val whileBlock = AmosElementType("WHILE_BLOCK")
    val repeatBlock = AmosElementType("REPEAT_BLOCK")
    val doBlock = AmosElementType("DO_BLOCK")
}


