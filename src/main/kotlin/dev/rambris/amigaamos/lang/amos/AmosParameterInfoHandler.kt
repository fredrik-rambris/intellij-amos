package dev.rambris.amigaamos.lang.amos

import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoHandler
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext

class AmosParameterInfoHandler : ParameterInfoHandler<AmosFile, AmosFunctionSignature> {
    override fun findElementForParameterInfo(context: CreateParameterInfoContext): AmosFile? {
        val file = context.file as? AmosFile ?: return null
        val callContext = AmosFunctionRegistry.findCallContext(file.text, context.offset) ?: return null
        val signatures = AmosFunctionRegistry.signaturesFor(callContext.name, file.project)
        if (signatures.isEmpty()) {
            return null
        }
        context.itemsToShow = signatures.toTypedArray()
        return file
    }

    override fun showParameterInfo(element: AmosFile, context: CreateParameterInfoContext) {
        val callContext = AmosFunctionRegistry.findCallContext(element.text, context.offset) ?: return
        context.showHint(element, callContext.leftParenthesisOffset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): AmosFile? {
        val file = context.file as? AmosFile ?: return null
        return file.takeIf { AmosFunctionRegistry.findCallContext(it.text, context.offset) != null }
    }

    override fun updateParameterInfo(parameterOwner: AmosFile, context: UpdateParameterInfoContext) {
        val callContext = AmosFunctionRegistry.findCallContext(parameterOwner.text, context.offset)
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



