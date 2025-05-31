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

import org.fusesource.jansi.Ansi
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrErrorDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrReplSnippet
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstantArray
import org.jetbrains.kotlin.ir.expressions.IrConstantObject
import org.jetbrains.kotlin.ir.expressions.IrConstantPrimitive
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrDynamicMemberExpression
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperatorExpression
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrErrorCallExpression
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrRawFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import java.io.File

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class ColoredRenderIrElementVisitor(
    private val options: DumpIrTreeOptions
) : IrVisitor<String, Nothing?>() {
    private val flagsRenderer = FlagsRenderer(options.declarationFlagsFilter, isReference = false)
    private val variableNameData = VariableNameData(options.normalizeNames)
    private var hideParameterNames = false

    fun withHiddenParameterNames(block: () -> Unit) {
        val oldHideParameterNames = hideParameterNames
        try {
            hideParameterNames = !options.printParameterNamesInOverriddenSymbols
            block()
        }
        finally {
            hideParameterNames = oldHideParameterNames
        }
    }

    fun renderFileEntry(fileEntry: IrFileEntry): String {
        val fullPath = fileEntry.name
        val renderedPath = if (options.printFilePath) fullPath else File(fullPath).name

        // TODO: use offsets in IR deserialization tests, KT-73171
        return "FILE_ENTRY path:$renderedPath"
    }

    fun renderType(type: IrType) = type.renderTypeWithRenderer(this@ColoredRenderIrElementVisitor, options)

    fun renderSymbolReference(symbol: IrSymbol) =
        Ansi.ansi().fgBrightYellow().a(symbol.renderReference()).fgDefault().toString()

    fun renderAsAnnotation(irAnnotation: IrConstructorCall): String =
        StringBuilder().also { it.renderAsAnnotation(irAnnotation, this, options) }.toString()

    private fun IrType.render(): String = renderType(this)

    private fun IrSymbol.renderReference() =
        if (isBound) owner.accept(BoundSymbolReferenceRenderer(variableNameData, hideParameterNames, options), null)
        else Ansi.ansi().declaration("UNBOUND ").a(javaClass.simpleName).toString()

    private class BoundSymbolReferenceRenderer(
        private val variableNameData: VariableNameData,
        private val hideParameterNames: Boolean,
        private val options: DumpIrTreeOptions,
    ) : IrElementVisitor<String, Nothing?> {

        private val flagsRenderer = FlagsRenderer(options.declarationFlagsFilter, isReference = true)

        override fun visitElement(element: IrElement, data: Nothing?) = buildTrimEnd {
            append('{')
            append(element.javaClass.simpleName)
            append('}')
            if (element is IrDeclaration) {
                if (element is IrDeclarationWithName) {
                    append(element.name)
                    append(' ')
                }
                renderDeclaredIn(element)
            }
        }

        override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): String =
            renderTypeParameter(declaration, null, options)

        override fun visitClass(declaration: IrClass, data: Nothing?) =
            renderClassWithRenderer(declaration, null, flagsRenderer, options)

        override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?) = renderEnumEntry(declaration, options)

        override fun visitField(declaration: IrField, data: Nothing?) = buildTrimEnd {
            append(renderField(declaration, null, flagsRenderer, options))
            if (declaration.origin != IrDeclarationOrigin.PROPERTY_BACKING_FIELD) {
                append(" ")
                renderDeclaredIn(declaration)
            }
        }

        override fun visitVariable(declaration: IrVariable, data: Nothing?) = buildTrimEnd {
            if (declaration.isVar) append("var ") else append("val ")

            append(declaration.normalizedName(variableNameData))
            append(": ")
            append(declaration.type.renderTypeWithRenderer(null, options))
            append(' ')
            append(declaration.renderVariableFlags(flagsRenderer))

            renderDeclaredIn(declaration)
        }

        override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?) = buildTrimEnd {
            runUnless(hideParameterNames) {
                append(declaration.renderValueParameterName(options))
                append(": ")
            }
            append(declaration.type.renderTypeWithRenderer(null, options))
            append(' ')
            append(declaration.renderValueParameterFlags(flagsRenderer))
            renderDeclaredIn(declaration)
        }

        override fun visitFunction(declaration: IrFunction, data: Nothing?) = buildTrimEnd {
            append(declaration.visibility)
            append(' ')

            if (declaration is IrSimpleFunction) {
                append(declaration.modality.toString().toLowerCaseAsciiOnly())
                append(' ')
            }

            when (declaration) {
                is IrSimpleFunction -> append("fun ")
                is IrConstructor -> append("constructor ")
            }

            append(declaration.name.asString())
            append(' ')

            renderTypeParameters(declaration)

            appendIterableWith(declaration.valueParameters, "(", ")", ", ") { valueParameter ->
                val varargElementType = valueParameter.varargElementType
                if (varargElementType != null) {
                    append("vararg ")
                    runUnless(hideParameterNames) {
                        append(valueParameter.renderValueParameterName(options))
                        append(": ")
                    }
                    append(varargElementType.renderTypeWithRenderer(null, options))
                }
                else {
                    runUnless(hideParameterNames) {
                        append(valueParameter.renderValueParameterName(options))
                        append(": ")
                    }
                    append(valueParameter.type.renderTypeWithRenderer(null, options))
                }
            }

            if (declaration is IrSimpleFunction) {
                append(": ")
                append(declaration.renderReturnType(null, options))
            }
            append(' ')

            when (declaration) {
                is IrSimpleFunction -> append(declaration.renderSimpleFunctionFlags(flagsRenderer))
                is IrConstructor -> append(declaration.renderConstructorFlags(flagsRenderer))
            }

            renderDeclaredIn(declaration)
        }

        private fun StringBuilder.renderTypeParameters(declaration: IrTypeParametersContainer) {
            if (declaration.typeParameters.isNotEmpty()) {
                appendIterableWith(declaration.typeParameters, "<", ">", ", ") { typeParameter ->
                    append(typeParameter.name.asString())
                }
                append(' ')
            }
        }

        override fun visitProperty(declaration: IrProperty, data: Nothing?) = buildTrimEnd {
            append(declaration.visibility)
            append(' ')
            append(declaration.modality.toString().toLowerCaseAsciiOnly())
            append(' ')

            append(declaration.name.asString())

            val getter = declaration.getter
            if (getter != null) {
                append(": ")
                append(getter.renderReturnType(null, options))
            }
            else declaration.backingField?.type?.let { type ->
                append(": ")
                append(type.renderTypeWithRenderer(null, options))
            }
            append(' ')

            append(declaration.renderPropertyFlags(flagsRenderer))

            renderDeclaredIn(declaration)
        }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): String =
            buildTrimEnd {
                if (declaration.isVar) append("var ") else append("val ")
                append(declaration.name.asString())
                append(": ")
                append(declaration.type.renderTypeWithRenderer(null, options))
                append(" by (...)")
            }

        private fun StringBuilder.renderDeclaredIn(irDeclaration: IrDeclaration) {
            append("declared in ")
            renderParentOfReferencedDeclaration(irDeclaration)
        }

        private fun StringBuilder.renderParentOfReferencedDeclaration(declaration: IrDeclaration) {
            val parent = try {
                declaration.parent
            } catch (e: Exception) {
                append("<no parent>")
                return
            }
            when (parent) {
                is IrPackageFragment -> {
                    val fqn = parent.packageFqName.asString()
                    append(fqn.ifEmpty { "<root>" })
                }

                is IrDeclaration -> {
                    renderParentOfReferencedDeclaration(parent)
                    appendDeclarationNameToFqName(parent, options) {
                        renderElementNameFallback(parent)
                    }
                }

                else -> renderElementNameFallback(parent)
            }
        }

        private fun StringBuilder.renderElementNameFallback(element: Any) {
            append('{')
            append(element.javaClass.simpleName)
            append('}')
        }
    }

    override fun visitElement(element: IrElement, data: Nothing?): String =
        Ansi.ansi().declaration("?ELEMENT? ").a("${element::class.java.simpleName} $element").toString()

    override fun visitDeclaration(declaration: IrDeclarationBase, data: Nothing?): String =
        Ansi.ansi().declaration("?DECLARATION? ").a("${declaration::class.java.simpleName} $declaration").toString()

    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): String {
        return buildString {
            append(Ansi.ansi().declaration("MODULE_FRAGMENT").toString())
            if (options.printModuleName) {
                append(" name:").append(declaration.name)
            }
        }
    }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?): String =
        "EXTERNAL_PACKAGE_FRAGMENT fqName:${declaration.packageFqName}"

    override fun visitFile(declaration: IrFile, data: Nothing?): String {
        val fileName = if (options.printFilePath) declaration.path else declaration.name
        return Ansi.ansi().declaration("FILE ").a("fqName:${declaration.packageFqName} fileName:$fileName").toString()
    }

    override fun visitFunction(declaration: IrFunction, data: Nothing?): String = declaration.runTrimEnd {
        Ansi.ansi().declaration("FUN ").a(renderOriginIfNonTrivial(options)).toString()
    }

    override fun visitScript(declaration: IrScript, data: Nothing?) = Ansi.ansi().declaration("SCRIPT").toString()

    override fun visitReplSnippet(declaration: IrReplSnippet, data: Nothing?): String =
        Ansi.ansi().declaration("REPL_SNIPPET ").a("name:${declaration.name}").toString()

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): String = declaration.runTrimEnd {
        Ansi.ansi().declaration("FUN ").a(
            renderOriginIfNonTrivial(options) + "name:$name " + renderSignatureIfEnabled(options.printSignatures) + "visibility:$visibility modality:$modality " + renderTypeParameters() + " " + renderValueParameterTypes() + " " + "returnType:${
                renderReturnType(
                    this@ColoredRenderIrElementVisitor, options
                )
            } " + renderSimpleFunctionFlags(flagsRenderer)
        ).toString()
    }

    private fun IrFunction.renderValueParameterTypes(): String = buildList {
        addIfNotNull(dispatchReceiverParameter?.run { "\$this:${renderValueParameterType(options)}" })
        addIfNotNull(extensionReceiverParameter?.run { "\$receiver:${type.render()}" })
        valueParameters.mapTo(this) { "${it.renderValueParameterName(options)}:${it.type.render()}" }
    }.joinToString(separator = ", ", prefix = "(", postfix = ")")

    override fun visitConstructor(declaration: IrConstructor, data: Nothing?): String = declaration.runTrimEnd {
        Ansi.ansi().declaration("CONSTRUCTOR ").a(
            renderOriginIfNonTrivial(options) + renderSignatureIfEnabled(options.printSignatures) + "visibility:$visibility " + renderTypeParameters() + " " + renderValueParameterTypes() + " " + "returnType:${
                renderReturnType(
                    this@ColoredRenderIrElementVisitor, options
                )
            } " + renderConstructorFlags(flagsRenderer)
        ).toString()
    }

    override fun visitProperty(declaration: IrProperty, data: Nothing?): String = declaration.runTrimEnd {
        Ansi.ansi().declaration("PROPERTY ").a(
            renderOriginIfNonTrivial(options) + "name:$name " + renderSignatureIfEnabled(options.printSignatures) + "visibility:$visibility modality:$modality " + renderPropertyFlags(
                flagsRenderer
            )
        ).toString()
    }

    override fun visitField(declaration: IrField, data: Nothing?): String =
        renderField(declaration, this, flagsRenderer, options)

    override fun visitClass(declaration: IrClass, data: Nothing?): String =
        renderClassWithRenderer(declaration, this, flagsRenderer, options)

    override fun visitVariable(declaration: IrVariable, data: Nothing?): String = declaration.runTrimEnd {
        Ansi.ansi().declaration("VAR ").a(
            "${renderOriginIfNonTrivial(options)}name:${normalizedName(variableNameData)} type:${type.render()} " + renderVariableFlags(
                flagsRenderer
            )
        ).toString()
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): String =
        renderEnumEntry(declaration, options)

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): String =
        Ansi.ansi().declaration("ANONYMOUS_INITIALIZER ").a("isStatic=${declaration.isStatic}").toString()

    override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): String =
        renderTypeParameter(declaration, this, options)

    override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): String = declaration.runTrimEnd {
        Ansi.ansi()
            .declaration("VALUE_PARAMETER ")
            .a(
                renderOriginIfNonTrivial(options) + "name:${renderValueParameterName(options)} " + (if (indexInOldValueParameters >= 0) "index:$indexInOldValueParameters " else "") + "type:${
                    renderValueParameterType(
                        options
                    )
                } " + (varargElementType?.let { "varargElementType:${it.render()} " }
                    ?: "") + renderValueParameterFlags(
                    flagsRenderer
                ))
            .toString()
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): String =
        declaration.runTrimEnd {
            Ansi.ansi()
                .declaration("LOCAL_DELEGATED_PROPERTY ")
                .a(declaration.renderOriginIfNonTrivial(options) + "name:$name type:${type.render()} flags:${renderLocalDelegatedPropertyFlags()}")
                .toString()
        }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): String = declaration.run {
        Ansi.ansi().declaration("TYPEALIAS ").a(
            declaration.renderOriginIfNonTrivial(options) + "name:$name " + renderSignatureIfEnabled(options.printSignatures) + "visibility:$visibility expandedType:${expandedType.render()}" + renderTypeAliasFlags(
                flagsRenderer
            )
        ).toString()
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): String =
        Ansi.ansi().declaration("EXPRESSION_BODY").toString()

    override fun visitBlockBody(body: IrBlockBody, data: Nothing?): String =
        Ansi.ansi().declaration("BLOCK_BODY").toString()

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): String = "SYNTHETIC_BODY kind=${body.kind}"

    override fun visitExpression(expression: IrExpression, data: Nothing?): String =
        "? ${expression::class.java.simpleName} type=${expression.type.render()}"

    override fun visitConst(expression: IrConst, data: Nothing?): String = Ansi.ansi()
        .declaration("CONST ")
        .a("${expression.kind} type=${expression.type.render()} value=${expression.value?.escapeIfRequired()}")
        .toString()

    private fun Any.escapeIfRequired() = when (this) {
        is String -> Ansi.ansi().stringLiteral("\"${StringUtil.escapeStringCharacters(this)}\"").toString()
        is Char -> Ansi.ansi().stringLiteral("'${StringUtil.escapeStringCharacters(this.toString())}'").toString()
        else -> this
    }

    override fun visitVararg(expression: IrVararg, data: Nothing?): String = Ansi.ansi()
        .declaration("VARARG ")
        .a("type=${expression.type.render()} varargElementType=${expression.varargElementType.render()}")
        .toString()

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): String =
        Ansi.ansi().statement("SPREAD_ELEMENT").toString()

    override fun visitBlock(expression: IrBlock, data: Nothing?): String {
        val prefix = when (expression) {
            is IrReturnableBlock -> "RETURNABLE_"
            is IrInlinedFunctionBlock -> "INLINED_"
            else -> ""
        }
        return Ansi.ansi()
            .declaration("${prefix}BLOCK ")
            .a("type=${expression.type.render()} origin=${expression.origin}")
            .toString()
    }

    override fun visitComposite(expression: IrComposite, data: Nothing?): String = Ansi.ansi()
        .declaration("COMPOSITE ")
        .a("type=${expression.type.render()} origin=${expression.origin}")
        .toString()

    override fun visitReturn(expression: IrReturn, data: Nothing?): String = Ansi.ansi()
        .statement("RETURN ")
        .a("type=${expression.type.render()} from='${expression.returnTargetSymbol.renderReference()}'")
        .toString()

    override fun visitCall(expression: IrCall, data: Nothing?): String = Ansi.ansi()
        .statement("CALL ")
        .a("'${expression.symbol.renderReference()}' ${expression.renderSuperQualifier()}\" + \"type=${expression.type.render()} origin=${expression.origin}")
        .toString()

    private fun IrCall.renderSuperQualifier(): String =
        superQualifierSymbol?.let { "superQualifier='${it.renderReference()}' " } ?: ""

    override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?): String = Ansi.ansi()
        .statement("CONSTRUCTOR_CALL ")
        .a("'${expression.symbol.renderReference()}' type=${expression.type.render()} origin=${expression.origin}")
        .toString()

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): String =
        Ansi.ansi().statement("DELEGATING_CONSTRUCTOR_CALL ").a("'${expression.symbol.renderReference()}'").toString()

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): String =
        Ansi.ansi().statement("ENUM_CONSTRUCTOR_CALL ").a("'${expression.symbol.renderReference()}'").toString()

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?): String =
        Ansi.ansi()
            .statement("INSTANCE_INITIALIZER_CALL ")
            .a("classDescriptor='${expression.classSymbol.renderReference()}' type=${expression.type.render()}")
            .toString()

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): String = Ansi.ansi()
        .statement("GET_VAR ")
        .a("'${expression.symbol.renderReference()}' type=${expression.type.render()} origin=${expression.origin}")
        .toString()

    override fun visitSetValue(expression: IrSetValue, data: Nothing?): String = Ansi.ansi()
        .statement("SET_VAR ")
        .a("'${expression.symbol.renderReference()}' type=${expression.type.render()} origin=${expression.origin}")
        .toString()

    override fun visitGetField(expression: IrGetField, data: Nothing?): String = buildTrimEnd {
        append(
            Ansi.ansi()
                .statement("GET_FIELD ")
                .a("'${expression.symbol.renderReference()}' type=${expression.type.render()}")
                .toString()
        )
        appendSuperQualifierSymbol(expression)
        append(" origin=${expression.origin}")
    }

    override fun visitSetField(expression: IrSetField, data: Nothing?): String = buildTrimEnd {
        append(
            Ansi.ansi()
                .statement("SET_FIELD ")
                .a("'${expression.symbol.renderReference()}' type=${expression.type.render()}")
                .toString()
        )
        appendSuperQualifierSymbol(expression)
        append(" origin=${expression.origin}")
    }

    private fun StringBuilder.appendSuperQualifierSymbol(expression: IrFieldAccessExpression) {
        val superQualifierSymbol = expression.superQualifierSymbol ?: return
        append(" superQualifierSymbol=")
        superQualifierSymbol.owner.renderDeclarationFqn(this, options)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): String = Ansi.ansi()
        .statement("GET_OBJECT ")
        .a("'${expression.symbol.renderReference()}' type=${expression.type.render()}")
        .toString()

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): String = Ansi.ansi()
        .statement("GET_ENUM ")
        .a("'${expression.symbol.renderReference()}' type=${expression.type.render()}")
        .toString()

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): String =
        Ansi.ansi().statement("STRING_CONCATENATION ").a("type=${expression.type.render()}").toString()

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): String = Ansi.ansi()
        .statement("TYPE_OP ")
        .a("type=${expression.type.render()} origin=${expression.operator} typeOperand=${expression.typeOperand.render()}")
        .toString()

    override fun visitWhen(expression: IrWhen, data: Nothing?): String =
        Ansi.ansi().statement("WHEN ").a("type=${expression.type.render()} origin=${expression.origin}").toString()

    override fun visitBranch(branch: IrBranch, data: Nothing?): String = Ansi.ansi().statement("BRANCH").toString()

    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): String =
        Ansi.ansi().statement("WHILE ").a("label=${loop.label} origin=${loop.origin}").toString()

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?): String =
        Ansi.ansi().statement("DO_WHILE ").a("label=${loop.label} origin=${loop.origin}").toString()

    override fun visitBreak(jump: IrBreak, data: Nothing?): String =
        Ansi.ansi().statement("BREAK ").a("label=${jump.label} loop.label=${jump.loop.label}").toString()

    override fun visitContinue(jump: IrContinue, data: Nothing?): String =
        Ansi.ansi().statement("CONTINUE ").a("label=${jump.label} loop.label=${jump.loop.label}").toString()

    override fun visitThrow(expression: IrThrow, data: Nothing?): String =
        Ansi.ansi().statement("THROW ").a("type=${expression.type.render()}").toString()

    override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): String =
        Ansi.ansi().declaration("FUNCTION_REFERENCE ").a(
            "'${expression.symbol.renderReference()}' " + "type=${expression.type.render()} origin=${expression.origin} " + "reflectionTarget=${
                renderReflectionTarget(expression)
            }"
        ).toString()

    override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: Nothing?): String =
        Ansi.ansi().declaration("RICH_FUNCTION_REFERENCE ").a(
            "type=${expression.type.render()} origin=${expression.origin} " + renderFlagsListWithoutFiltering(
                "unit_conversion".takeIf { expression.hasUnitConversion },
                "suspend_conversion".takeIf { expression.hasSuspendConversion },
                "vararg_conversion".takeIf { expression.hasVarargConversion },
                "restricted_suspension".takeIf { expression.isRestrictedSuspension },
            ) + "reflectionTarget='${expression.reflectionTargetSymbol?.renderReference()}'"
        ).toString()

    override fun visitRichPropertyReference(expression: IrRichPropertyReference, data: Nothing?): String = Ansi.ansi()
        .declaration("RICH_PROPERTY_REFERENCE ")
        .a("type=${expression.type.render()} origin=${expression.origin} " + "reflectionTarget='${expression.reflectionTargetSymbol?.renderReference()}'")
        .toString()

    override fun visitRawFunctionReference(expression: IrRawFunctionReference, data: Nothing?): String = Ansi.ansi()
        .declaration("RAW_FUNCTION_REFERENCE ")
        .a("'${expression.symbol.renderReference()}' type=${expression.type.render()}")
        .toString()

    private fun renderReflectionTarget(expression: IrFunctionReference) =
        if (expression.symbol == expression.reflectionTarget) "<same>"
        else expression.reflectionTarget?.renderReference()

    override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): String = buildTrimEnd {
        append(Ansi.ansi().declaration("PROPERTY_REFERENCE ").toString())
        append("'${expression.symbol.renderReference()}' ")
        appendNullableAttribute("field=", expression.field) { "'${it.renderReference()}'" }
        appendNullableAttribute("getter=", expression.getter) { "'${it.renderReference()}'" }
        appendNullableAttribute("setter=", expression.setter) { "'${it.renderReference()}'" }
        append("type=${expression.type.render()} ")
        append("origin=${expression.origin}")
    }

    private inline fun <T : Any> StringBuilder.appendNullableAttribute(
        prefix: String, value: T?, toString: (T) -> String
    ) {
        append(prefix)
        if (value != null) {
            append(toString(value))
        }
        else {
            append("null")
        }
        append(" ")
    }

    override fun visitLocalDelegatedPropertyReference(
        expression: IrLocalDelegatedPropertyReference, data: Nothing?
    ): String = buildTrimEnd {
        append(Ansi.ansi().declaration("LOCAL_DELEGATED_PROPERTY_REFERENCE ").toString())
        append("'${expression.symbol.renderReference()}' ")
        append("delegate='${expression.delegate.renderReference()}' ")
        append("getter='${expression.getter.renderReference()}' ")
        appendNullableAttribute("setter=", expression.setter) { "'${it.renderReference()}'" }
        append("type=${expression.type.render()} ")
        append("origin=${expression.origin}")
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?): String = buildTrimEnd {
        append(Ansi.ansi().declaration("FUN_EXPR ").toString())
        append("type=${expression.type.render()} origin=${expression.origin}")
    }

    override fun visitClassReference(expression: IrClassReference, data: Nothing?): String = Ansi.ansi()
        .declaration("CLASS_REFERENCE ")
        .a("'${expression.symbol.renderReference()}' type=${expression.type.render()}")
        .toString()

    override fun visitGetClass(expression: IrGetClass, data: Nothing?): String =
        Ansi.ansi().statement("GET_CLASS ").a("type=${expression.type.render()}").toString()

    override fun visitTry(aTry: IrTry, data: Nothing?): String =
        Ansi.ansi().statement("TRY ").a("type=${aTry.type.render()}").toString()

    override fun visitCatch(aCatch: IrCatch, data: Nothing?): String =
        Ansi.ansi().statement("CATCH ").a("parameter=${aCatch.catchParameter.symbol.renderReference()}").toString()

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: Nothing?): String =
        Ansi.ansi()
            .statement("DYN_OP ")
            .a("operator=${expression.operator} type=${expression.type.render()}")
            .toString()

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: Nothing?): String =
        Ansi.ansi()
            .statement("DYN_MEMBER ")
            .a("memberName='${expression.memberName}' type=${expression.type.render()}")
            .toString()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?): String =
        Ansi.ansi().error("ERROR_DECL ").a(
            "${declaration.descriptor::class.java.simpleName} " + descriptorRendererForErrorDeclarations.renderDescriptor(
                declaration.descriptor.original
            )
        ).toString()

    override fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?): String =
        Ansi.ansi().error("ERROR_EXPR ").a("'${expression.description}' type=${expression.type.render()}").toString()

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?): String =
        Ansi.ansi().error("ERROR_CALL ").a("'${expression.description}' type=${expression.type.render()}").toString()

    override fun visitConstantArray(expression: IrConstantArray, data: Nothing?): String =
        Ansi.ansi().declaration("CONSTANT_ARRAY ").a("type=${expression.type.render()}").toString()

    override fun visitConstantObject(expression: IrConstantObject, data: Nothing?): String = Ansi.ansi()
        .declaration("CONSTANT_OBJECT ")
        .a("type=${expression.type.render()} constructor=${expression.constructor.renderReference()}")
        .toString()

    override fun visitConstantPrimitive(expression: IrConstantPrimitive, data: Nothing?): String =
        Ansi.ansi().declaration("CONSTANT_PRIMITIVE ").a("type=${expression.type.render()}").toString()


    private val descriptorRendererForErrorDeclarations = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES
}