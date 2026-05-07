package dev.rambris.amigaamos.lang.amos

import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoHandler
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext

class AmosParameterInfoHandler : ParameterInfoHandler<AmosFile, AmosFunctionSignature> {
    override fun findElementForParameterInfo(context: CreateParameterInfoContext): AmosFile? {
        val file = context.file as? AmosFile ?: return null

        val funcContext = AmosFunctionRegistry.findCallContext(file.text, context.offset)
        if (funcContext != null) {
            val signatures = AmosFunctionRegistry.signaturesFor(funcContext.name, file.project)
            if (signatures.isNotEmpty()) {
                context.itemsToShow = signatures.toTypedArray()
                return file
            }
        }

        val instrContext = AmosFunctionRegistry.findInstructionCallContext(file.text, context.offset, file.project)
        if (instrContext != null) {
            val signatures = AmosFunctionRegistry.signaturesForInstruction(instrContext.name, file.project)
            if (signatures.isNotEmpty()) {
                context.itemsToShow = signatures.toTypedArray()
                return file
            }
        }

        return null
    }

    override fun showParameterInfo(element: AmosFile, context: CreateParameterInfoContext) {
        val callContext = AmosFunctionRegistry.findCallContext(element.text, context.offset)
            ?: AmosFunctionRegistry.findInstructionCallContext(element.text, context.offset, element.project)
            ?: return
        context.showHint(element, callContext.leftParenthesisOffset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): AmosFile? {
        val file = context.file as? AmosFile ?: return null
        val hasContext = AmosFunctionRegistry.findCallContext(file.text, context.offset) != null
            || AmosFunctionRegistry.findInstructionCallContext(file.text, context.offset, file.project) != null
        return file.takeIf { hasContext }
    }

    override fun updateParameterInfo(parameterOwner: AmosFile, context: UpdateParameterInfoContext) {
        val callContext = AmosFunctionRegistry.findCallContext(parameterOwner.text, context.offset)
            ?: AmosFunctionRegistry.findInstructionCallContext(parameterOwner.text, context.offset, parameterOwner.project)
        if (callContext == null) {
            context.removeHint()
            return
        }
        context.setCurrentParameter(callContext.currentParameterIndex)
    }

    override fun updateUI(signature: AmosFunctionSignature, context: ParameterInfoUIContext) {
        val currentParameter = context.currentParameterIndex
        val range = signature.parameterRanges.getOrNull(currentParameter)
        val highlightStart = range?.startOffset ?: -1
        val highlightEnd = range?.endOffset ?: -1
        context.setupUIComponentPresentation(
            signature.presentation,
            highlightStart,
            highlightEnd,
            false,
            false,
            false,
            context.defaultParameterColor
        )
    }
}



