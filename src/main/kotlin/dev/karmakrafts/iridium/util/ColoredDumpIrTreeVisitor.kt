/*
 * Copyright 2025 (C) Karma Krafts & associates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.karmakrafts.iridium.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrConstantArray
import org.jetbrains.kotlin.ir.expressions.IrConstantObject
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperatorExpression
import org.jetbrains.kotlin.ir.expressions.IrErrorCallExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.classTypeArgumentsCount
import org.jetbrains.kotlin.ir.expressions.outerClassReceiver
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class ColoredDumpIrTreeVisitor(
    out: Appendable,
    private val options: DumpIrTreeOptions = DumpIrTreeOptions(),
) : IrElementVisitor<Unit, String> {

    private val printer = Printer(out, "  ")
    private val elementRenderer = ColoredRenderIrElementVisitor(options)
    private fun IrType.render() = elementRenderer.renderType(this)

    private fun List<IrDeclaration>.ordered(): List<IrDeclaration> = if (options.stableOrder) stableOrdered() else this

    private fun IrDeclaration.isHidden(): Boolean = options.isHiddenDeclaration(this)

    override fun visitElement(element: IrElement, data: String) {
        element.dumpLabeledElementWith(data) {
            if (element is IrAnnotationContainer) {
                dumpAnnotations(element)
            }
            element.acceptChildren(this@ColoredDumpIrTreeVisitor, "")
        }
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.files.dumpElements()
        }
    }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.declarations.ordered().dumpElements()
        }
    }

    override fun visitFile(declaration: IrFile, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.declarations.ordered().dumpElements()
        }
    }

    override fun visitClass(declaration: IrClass, data: String) {
        if (declaration.isHidden()) return
        if (declaration.isExpect && !options.printExpectDeclarations) return
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            runIf(options.printSealedSubclasses) {
                declaration.sealedSubclasses.dumpItems("sealedSubclasses") { it.dump() }
            }
            declaration.thisReceiver?.accept(this, "\$this")
            declaration.typeParameters.dumpElements()
            declaration.declarations.ordered().dumpElements()
        }
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: String) {
        if (declaration.isHidden()) return
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.typeParameters.dumpElements()
        }
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: String) {
        if (declaration.isHidden()) return
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: String) {
        if (declaration.isHidden()) return
        if (declaration.isExpect && !options.printExpectDeclarations) return
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.correspondingPropertySymbol?.dumpInternal("correspondingProperty")
            declaration.overriddenSymbols.dumpFakeOverrideSymbols()
            declaration.typeParameters.dumpElements()
            declaration.dispatchReceiverParameter?.accept(this, "\$this")

            val contextReceiverParametersCount = declaration.contextReceiverParametersCount
            if (contextReceiverParametersCount > 0) {
                printer.println("contextReceiverParametersCount: $contextReceiverParametersCount")
            }

            declaration.extensionReceiverParameter?.accept(this, "\$receiver")
            declaration.valueParameters.dumpElements()
            declaration.body?.accept(this, "")
        }
    }

    private fun dumpAnnotations(element: IrAnnotationContainer) {
        element.annotations.filterOutSourceRetentions(options)
            .dumpItems("annotations") { irAnnotation: IrConstructorCall ->
                printer.println(elementRenderer.renderAsAnnotation(irAnnotation))
            }
    }

    private fun IrSymbol.dump(label: String? = null) = printer.println(
        elementRenderer.renderSymbolReference(this).let {
            if (label != null) "$label: $it" else it
        })

    override fun visitConstructor(declaration: IrConstructor, data: String) {
        if (declaration.isHidden()) return
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.typeParameters.dumpElements()
            declaration.dispatchReceiverParameter?.accept(this, "\$outer")
            declaration.valueParameters.dumpElements()
            declaration.body?.accept(this, "")
        }
    }

    override fun visitProperty(declaration: IrProperty, data: String) {
        if (declaration.isHidden()) return
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.overriddenSymbols.dumpFakeOverrideSymbols()
            declaration.backingField?.accept(this, "")
            declaration.getter?.accept(this, "")
            declaration.setter?.accept(this, "")
        }
    }

    override fun visitField(declaration: IrField, data: String) {
        if (declaration.isHidden()) return
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.initializer?.accept(this, "")
        }
    }

    private fun List<IrElement>.dumpElements() {
        forEach { it.accept(this@ColoredDumpIrTreeVisitor, "") }
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.explicitReceiver?.accept(this, "receiver")
            expression.arguments.dumpElements()
        }
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: String) {
        if (declaration.isHidden()) return
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.initializerExpression?.accept(this, "init")
            declaration.correspondingClass?.accept(this, "class")
        }
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: String) {
        expression.dumpLabeledElementWith(data) {
            dumpTypeArguments(expression)
            expression.dispatchReceiver?.accept(this, "\$this")
            expression.extensionReceiver?.accept(this, "\$receiver")
            val valueParameterNames = expression.getValueParameterNamesForDebug(options)
            for (index in 0 until expression.valueArgumentsCount) {
                expression.getValueArgument(index)?.accept(this, valueParameterNames[index])
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: String) {
        expression.dumpLabeledElementWith(data) {
            dumpTypeArguments(expression)
            expression.outerClassReceiver?.accept(this, "\$outer")
            dumpConstructorValueArguments(expression)
        }
    }

    private fun dumpConstructorValueArguments(expression: IrConstructorCall) {
        val valueParameterNames = expression.getValueParameterNamesForDebug(options)
        for (index in 0 until expression.valueArgumentsCount) {
            expression.getValueArgument(index)?.accept(this, valueParameterNames[index])
        }
    }

    private fun dumpTypeArguments(expression: IrMemberAccessExpression<*>) {
        val typeParameterNames = expression.getTypeParameterNames(expression.typeArguments.size)
        for (index in 0 until expression.typeArguments.size) {
            printer.println("<${typeParameterNames[index]}>: ${expression.renderTypeArgument(index)}")
        }
    }

    private fun dumpTypeArguments(expression: IrConstructorCall) {
        val typeParameterNames = expression.getTypeParameterNames(expression.typeArguments.size)
        for (index in 0 until expression.typeArguments.size) {
            val typeParameterName = typeParameterNames[index]
            val parameterLabel = if (index < expression.classTypeArgumentsCount) "class: $typeParameterName"
            else typeParameterName
            printer.println("<$parameterLabel>: ${expression.renderTypeArgument(index)}")
        }
    }

    private fun IrMemberAccessExpression<*>.getTypeParameterNames(expectedCount: Int): List<String> =
        if (symbol.isBound) symbol.owner.getTypeParameterNames(expectedCount)
        else getPlaceholderParameterNames(expectedCount)

    private fun IrSymbolOwner.getTypeParameterNames(expectedCount: Int): List<String> =
        if (this is IrTypeParametersContainer) {
            val typeParameters = if (this is IrConstructor) getFullTypeParametersList() else this.typeParameters
            (0 until expectedCount).map {
                if (it < typeParameters.size) typeParameters[it].name.asString()
                else "${it + 1}"
            }
        }
        else {
            getPlaceholderParameterNames(expectedCount)
        }

    private fun IrConstructor.getFullTypeParametersList(): List<IrTypeParameter> {
        val parentClass = try {
            parent as? IrClass ?: return typeParameters
        } catch (e: Exception) {
            return typeParameters
        }
        return parentClass.typeParameters + typeParameters
    }

    private fun IrMemberAccessExpression<*>.renderTypeArgument(index: Int): String =
        this.typeArguments[index]?.render() ?: "<none>"

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: String) {
        inlinedBlock.dumpLabeledElementWith(data) {
            inlinedBlock.inlinedFunctionSymbol?.dumpInternal("inlinedFunctionSymbol")
            inlinedBlock.inlinedFunctionFileEntry.dumpInternal("inlinedFunctionFileEntry")
            inlinedBlock.acceptChildren(this, "")
        }
    }

    override fun visitGetField(expression: IrGetField, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver?.accept(this, "receiver")
        }
    }

    override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: String) {
        expression.dumpLabeledElementWith(data) {
            val names = expression.invokeFunction.getValueParameterNamesForDebug(expression.boundValues.size, options)
            expression.overriddenFunctionSymbol.dumpInternal("overriddenFunctionSymbol")
            expression.boundValues.forEachIndexed { index, value ->
                value.accept(this, "bound ${names[index]}")
            }
            expression.invokeFunction.accept(this, "invoke")
        }
    }

    override fun visitRichPropertyReference(expression: IrRichPropertyReference, data: String) {
        expression.dumpLabeledElementWith(data) {
            val names = expression.getterFunction.getValueParameterNamesForDebug(expression.boundValues.size, options)
            expression.boundValues.forEachIndexed { index, value ->
                value.accept(this, "bound ${names[index]}")
            }
            expression.getterFunction.accept(this, "getter")
            expression.setterFunction?.accept(this, "setter")
        }
    }

    override fun visitSetField(expression: IrSetField, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver?.accept(this, "receiver")
            expression.value.accept(this, "value")
        }
    }

    override fun visitWhen(expression: IrWhen, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.branches.dumpElements()
        }
    }

    override fun visitBranch(branch: IrBranch, data: String) {
        branch.dumpLabeledElementWith(data) {
            branch.condition.accept(this, "if")
            branch.result.accept(this, "then")
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: String) {
        loop.dumpLabeledElementWith(data) {
            loop.condition.accept(this, "condition")
            loop.body?.accept(this, "body")
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: String) {
        loop.dumpLabeledElementWith(data) {
            loop.body?.accept(this, "body")
            loop.condition.accept(this, "condition")
        }
    }

    override fun visitTry(aTry: IrTry, data: String) {
        aTry.dumpLabeledElementWith(data) {
            aTry.tryResult.accept(this, "try")
            aTry.catches.dumpElements()
            aTry.finallyExpression?.accept(this, "finally")
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.acceptChildren(this, "")
        }
    }

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver.accept(this, "receiver")
            for ((i, arg) in expression.arguments.withIndex()) {
                arg.accept(this, i.toString())
            }
        }
    }

    override fun visitConstantArray(expression: IrConstantArray, data: String) {
        expression.dumpLabeledElementWith(data) {
            for ((i, value) in expression.elements.withIndex()) {
                value.accept(this, i.toString())
            }
        }
    }

    override fun visitConstantObject(expression: IrConstantObject, data: String) {
        expression.dumpLabeledElementWith(data) {
            for ((index, argument) in expression.valueArguments.withIndex()) {
                argument.accept(this, expression.constructor.owner.valueParameters[index].name.toString())
            }
        }
    }

    private inline fun IrElement.dumpLabeledElementWith(label: String, body: () -> Unit) {
        printer.println(accept(elementRenderer, null).withLabel(label))
        indented(body)
    }

    private inline fun <T> Collection<T>.dumpItems(caption: String, renderElement: (T) -> Unit) {
        if (isEmpty()) return
        indented(caption) {
            forEach {
                renderElement(it)
            }
        }
    }

    private fun Collection<IrSymbol>.dumpFakeOverrideSymbols() {
        if (isEmpty()) return
        elementRenderer.withHiddenParameterNames {
            indented("overridden") {
                map(elementRenderer::renderSymbolReference).applyIf(options.stableOrderOfOverriddenSymbols) { sorted() }
                    .forEach { printer.println(it) }
            }
        }
    }

    private fun IrSymbol.dumpInternal(label: String? = null) {
        if (isBound) owner.dumpInternal(label)
        else printer.println("$label: UNBOUND ${javaClass.simpleName}")
    }

    private fun IrElement.dumpInternal(label: String? = null) {
        if (label != null) {
            printer.println("$label: ", accept(elementRenderer, null))
        }
        else {
            printer.println(accept(elementRenderer, null))
        }
    }

    private fun IrFileEntry.dumpInternal(label: String? = null) {
        val prefix = if (label != null) "$label: " else ""
        val renderedText = elementRenderer.renderFileEntry(this)
        printer.println(prefix + renderedText)
    }

    private inline fun indented(label: String, body: () -> Unit) {
        printer.println("$label:")
        indented(body)
    }

    private inline fun indented(body: () -> Unit) {
        printer.pushIndent()
        body()
        printer.popIndent()
    }

    private fun String.withLabel(label: String) = if (label.isEmpty()) this else "$label: $this"
}