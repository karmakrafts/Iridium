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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.IrCapturedType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFileClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.SpecialNames.IMPLICIT_SET_PARAMETER
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal fun Ansi.statement(s: String): Ansi = fgBrightMagenta().a(s).fgDefault()
internal fun Ansi.declaration(s: String): Ansi = fgMagenta().a(s).fgDefault()
internal fun Ansi.type(s: String): Ansi = fgBrightBlue().a(s).fgDefault()
internal fun Ansi.typeParameter(s: String): Ansi = fgCyan().a(s).fgDefault()
internal fun Ansi.name(s: String): Ansi = fgYellow().a(s).fgDefault()
internal fun Ansi.annotation(s: String): Ansi = fgBrightYellow().a(s).fgDefault()
internal fun Ansi.stringLiteral(s: String): Ansi = fgBrightGreen().a(s).fgDefault()
internal fun Ansi.error(s: String): Ansi = fgBrightRed().a(s).fgDefault()
internal fun Ansi.special(s: String): Ansi = fgBrightCyan().a(s).fgDefault()

internal fun IrDeclarationWithName.renderSignatureIfEnabled(printSignatures: Boolean): String =
    if (printSignatures) symbol.signature?.let { "signature:${it.render()} " }.orEmpty() else ""

internal fun IrDeclaration.renderOriginIfNonTrivial(options: DumpIrTreeOptions): String {
    val originsToSkipFromRendering: HashSet<IrDeclarationOrigin> = hashSetOf(IrDeclarationOrigin.DEFINED)
    if (!options.renderOriginForExternalDeclarations) {
        originsToSkipFromRendering.add(IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB)
    }
    return if (origin in originsToSkipFromRendering) "" else "$origin "
}

internal fun renderEnumEntry(declaration: IrEnumEntry, options: DumpIrTreeOptions) = declaration.runTrimEnd {
    Ansi.ansi()
        .declaration("ENUM_ENTRY ")
        .a(renderOriginIfNonTrivial(options) + "name:$name " + renderSignatureIfEnabled(options.printSignatures))
        .toString()
}

internal inline fun buildTrimEnd(fn: StringBuilder.() -> Unit): String = buildString(fn).trimEnd()

internal inline fun <T> T.runTrimEnd(fn: T.() -> String): String = run(fn).trimEnd()

internal class VariableNameData(val normalizeNames: Boolean) {
    val nameMap: MutableMap<IrVariableSymbol, String> = mutableMapOf()
    var temporaryIndex: Int = 0
}

internal class FlagsRenderer(
    private val flagsFilter: DumpIrTreeOptions.FlagsFilter, private val isReference: Boolean
) {
    fun renderFlagsList(declaration: IrDeclaration, vararg flags: String?): String {
        val flagsList = flagsFilter.filterFlags(declaration, isReference, flags.filterNotNull())
        if (flagsList.isEmpty()) return ""
        return flagsList.joinToString(prefix = "[", postfix = "] ", separator = ",")
    }
}

internal fun List<IrDeclaration>.stableOrdered(): List<IrDeclaration> {
    val strictOrder = hashMapOf<IrDeclaration, Int>()

    var idx = 0

    forEach {
        val shouldPreserveRelativeOrder = when (it) {
            is IrProperty -> it.backingField != null && !it.isConst
            is IrAnonymousInitializer, is IrEnumEntry, is IrField -> true
            else -> false
        }
        if (shouldPreserveRelativeOrder) {
            strictOrder[it] = idx++
        }
    }

    return sortedWith { a, b ->
        val strictA = strictOrder[a] ?: Int.MAX_VALUE
        val strictB = strictOrder[b] ?: Int.MAX_VALUE

        if (strictA == strictB) {
            val rA = a.render()
            val rB = b.render()
            rA.compareTo(rB)
        }
        else strictA - strictB
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrMemberAccessExpression<*>.getValueParameterNamesForDebug(options: DumpIrTreeOptions): List<String> {
    val expectedCount = valueArgumentsCount
    if (symbol.isBound) {
        val owner = symbol.owner
        if (owner is IrFunction) {
            return owner.getValueParameterNamesForDebug(expectedCount, options)
        }
    }
    return getPlaceholderParameterNames(expectedCount)
}

internal fun IrFunction.getValueParameterNamesForDebug(
    expectedCount: Int,
    options: DumpIrTreeOptions,
): List<String> = (0 until expectedCount).map {
    if (it < valueParameters.size) valueParameters[it].renderValueParameterName(options)
    else "${it + 1}"
}

internal fun getPlaceholderParameterNames(expectedCount: Int) = (1..expectedCount).map { "$it" }

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun List<IrConstructorCall>.filterOutSourceRetentions(options: DumpIrTreeOptions): List<IrConstructorCall> =
    applyIf(!options.printSourceRetentionAnnotations) {
        filterNot { it: IrConstructorCall ->
            (it.symbol.owner.returnType.classifierOrNull?.owner as? IrClass)?.annotations?.any { it: IrConstructorCall ->
                it.symbol.owner.returnType.classFqName?.asString() == Retention::class.java.name && (it.arguments.first() as? IrGetEnumValue)?.symbol?.owner?.name?.asString() == AnnotationRetention.SOURCE.name
            } == true
        }
    }

internal fun IrValueParameter.renderValueParameterName(options: DumpIrTreeOptions): String {
    val name = runIf(name == IMPLICIT_SET_PARAMETER) { options.replaceImplicitSetterParameterNameWith } ?: name
    return Ansi.ansi().name(name.asString()).toString()
}

internal fun IrTypeArgument.renderTypeArgument(
    renderer: ColoredRenderIrElementVisitor?, options: DumpIrTreeOptions
): String = when (this) {
    is IrStarProjection -> "*"

    is IrTypeProjection -> buildTrimEnd {
        append(variance.label)
        if (variance != Variance.INVARIANT) append(' ')
        append(type.renderTypeWithRenderer(renderer, options))
    }
}

internal fun IrType.renderTypeInner(renderer: ColoredRenderIrElementVisitor?, options: DumpIrTreeOptions) =
    when (this) {
        is IrDynamicType -> Ansi.ansi().special("dynamic").toString()

        is IrErrorType -> Ansi.ansi()
            .error("IrErrorType(${options.verboseErrorTypes.ifTrue { originalKotlinType }})")
            .toString()

        is IrCapturedType -> "IrCapturedType(${constructor.argument.renderTypeArgument(renderer, options)}"

        is IrSimpleType -> buildTrimEnd {
            val isDefinitelyNotNullType =
                classifier is IrTypeParameterSymbol && nullability == SimpleTypeNullability.DEFINITELY_NOT_NULL
            if (isDefinitelyNotNullType) append("{")
            append(classifier.renderClassifierFqn(options))
            if (arguments.isNotEmpty()) {
                append(
                    arguments.joinToString(prefix = "<", postfix = ">", separator = ", ") {
                        it.renderTypeArgument(renderer, options)
                    })
            }
            if (isDefinitelyNotNullType) {
                append(" & Any}")
            }
            else if (isMarkedNullable()) {
                append('?')
            }
            if (options.printTypeAbbreviations) abbreviation?.let {
                append(it.renderTypeAbbreviation(renderer, options))
            }
        }
    }

internal val IrFunction.safeReturnType: IrType?
    get() = try {
        returnType
    } catch (e: UninitializedPropertyAccessException) {
        null
    }

internal fun renderTypeAnnotations(
    annotations: List<IrConstructorCall>, renderer: ColoredRenderIrElementVisitor?, options: DumpIrTreeOptions
): String = annotations.filterOutSourceRetentions(options).let {
    if (it.isEmpty()) ""
    else buildString {
        appendIterableWith(it, prefix = "", postfix = " ", separator = " ") {
            append("@[")
            renderAsAnnotation(it, renderer, options)
            append("]")
        }
    }
}

internal fun IrFunction.renderReturnType(renderer: ColoredRenderIrElementVisitor?, options: DumpIrTreeOptions): String =
    safeReturnType?.renderTypeWithRenderer(renderer, options) ?: "<Uninitialized>"

internal fun IrType.renderTypeWithRenderer(
    renderer: ColoredRenderIrElementVisitor?, options: DumpIrTreeOptions
): String = "${renderTypeAnnotations(annotations, renderer, options)}${renderTypeInner(renderer, options)}"

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun StringBuilder.renderAsAnnotation(
    irAnnotation: IrConstructorCall,
    renderer: ColoredRenderIrElementVisitor?,
    options: DumpIrTreeOptions,
) {
    val annotationClassName =
        irAnnotation.symbol.takeIf { it.isBound }?.owner?.parentAsClass?.name?.asString() ?: "<unbound>"
    append(Ansi.ansi().annotation(annotationClassName).toString())

    if (irAnnotation.typeArguments.isNotEmpty()) {
        irAnnotation.typeArguments.joinTo(this, ", ", "<", ">") {
            it?.renderTypeWithRenderer(renderer, options) ?: Ansi.ansi().special("null").toString()
        }
    }

    if (irAnnotation.valueArgumentsCount == 0) return

    val valueParameterNames = irAnnotation.getValueParameterNamesForDebug(options)

    appendIterableWith(0 until irAnnotation.valueArgumentsCount, separator = ", ", prefix = "(", postfix = ")") {
        append(Ansi.ansi().name(valueParameterNames[it]).toString())
        append(" = ")
        renderAsAnnotationArgument(irAnnotation.getValueArgument(it), renderer, options)
    }
}

internal fun StringBuilder.renderIrConstAsAnnotationArgument(const: IrConst) {
    append(
        when (const.kind) {
            IrConstKind.String -> Ansi.ansi().fgBrightGreen().a("\"").toString()
            IrConstKind.Char -> Ansi.ansi().fgBrightGreen().a("'").toString()
            else -> ""
        }
    )
    append(const.value.toString())
    append(
        when (const.kind) {
            IrConstKind.String -> Ansi.ansi().a("\"").fgDefault().toString()
            IrConstKind.Char -> Ansi.ansi().a("'").fgDefault().toString()
            else -> ""
        }
    )
}

internal fun StringBuilder.renderAsAnnotationArgument(
    irElement: IrElement?, renderer: ColoredRenderIrElementVisitor?, options: DumpIrTreeOptions
) {
    when (irElement) {
        null -> append(Ansi.ansi().special("<null>").toString())
        is IrConstructorCall -> renderAsAnnotation(irElement, renderer, options)
        is IrConst -> {
            renderIrConstAsAnnotationArgument(irElement)
        }

        is IrVararg -> {
            appendIterableWith(irElement.elements, prefix = "[", postfix = "]", separator = ", ") {
                renderAsAnnotationArgument(it, renderer, options)
            }
            append(" type=${irElement.type.renderTypeWithRenderer(renderer, options)}")
            append(" varargElementType=${irElement.varargElementType.renderTypeWithRenderer(renderer, options)}")
        }

        else -> if (renderer != null) {
            append(irElement.accept(renderer, null))
        }
        else {
            append("...")
        }
    }
}

internal fun IrVariable.normalizedName(data: VariableNameData): String {
    if (data.normalizeNames && (origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE || origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR)) {
        return data.nameMap.getOrPut(symbol) { "tmp_${data.temporaryIndex++}" }
    }
    return name.asString()
}

internal inline fun <T, Buffer : Appendable> Buffer.appendIterableWith(
    iterable: Iterable<T>, prefix: String, postfix: String, separator: String, renderItem: Buffer.(T) -> Unit
) {
    append(prefix)
    var isFirst = true
    for (item in iterable) {
        if (!isFirst) append(separator)
        renderItem(item)
        isFirst = false
    }
    append(postfix)
}

internal inline fun StringBuilder.appendDeclarationNameToFqName(
    declaration: IrDeclaration, options: DumpIrTreeOptions, fallback: () -> Unit
) {
    if (!declaration.isFileClass || options.printFacadeClassInFqNames) {
        append('.')
        if (declaration is IrDeclarationWithName) {
            append(declaration.name)
        }
        else {
            fallback()
        }
    }
}

internal fun DescriptorRenderer.renderDescriptor(descriptor: DeclarationDescriptor): String =
    if (descriptor is ReceiverParameterDescriptor) "this@${descriptor.containingDeclaration.name}: ${descriptor.type}"
    else render(descriptor)

internal fun IrClass.renderClassFqn(options: DumpIrTreeOptions): String =
    Ansi.ansi().type(StringBuilder().also { renderDeclarationFqn(it, options) }.toString()).toString()

internal fun IrScript.renderScriptFqn(options: DumpIrTreeOptions): String =
    StringBuilder().also { renderDeclarationFqn(it, options) }.toString()

internal fun IrFunction.renderTypeParameters(): String =
    typeParameters.joinToString(separator = ", ", prefix = "<", postfix = ">") {
        Ansi.ansi().typeParameter(it.name.asString()).toString()
    }

internal fun renderFlagsListWithoutFiltering(vararg flags: String?) = flags.filterNotNull().run {
    if (isNotEmpty()) joinToString(prefix = "[", postfix = "] ", separator = ",")
    else ""
}

internal fun IrTypeAlias.renderTypeAliasFlags(renderer: FlagsRenderer): String = renderer.renderFlagsList(
    declaration = this, "actual".takeIf { isActual })

internal fun IrLocalDelegatedProperty.renderLocalDelegatedPropertyFlags() = if (isVar) "var" else "val"

internal fun IrValueParameter.renderValueParameterType(options: DumpIrTreeOptions): String {
    return if (!options.printDispatchReceiverTypeInFakeOverrides && kind == IrParameterKind.DispatchReceiver && (parent as? IrFunction)?.isFakeOverride == true) {
        "HIDDEN_DISPATCH_RECEIVER_TYPE"
    }
    else {
        type.renderTypeWithRenderer(ColoredRenderIrElementVisitor(options), options)
    }
}

internal fun IrClass.renderClassFlags(renderer: FlagsRenderer) = renderer.renderFlagsList(
    declaration = this,
    "companion".takeIf { isCompanion },
    "inner".takeIf { isInner },
    "data".takeIf { isData },
    "external".takeIf { isExternal },
    "value".takeIf { isValue },
    "expect".takeIf { isExpect },
    "fun".takeIf { isFun })

internal fun IrProperty.renderPropertyFlags(renderer: FlagsRenderer) = renderer.renderFlagsList(
    declaration = this,
    "external".takeIf { isExternal },
    "const".takeIf { isConst },
    "lateinit".takeIf { isLateinit },
    "delegated".takeIf { isDelegated },
    "expect".takeIf { isExpect },
    "fake_override".takeIf { isFakeOverride },
    if (isVar) "var" else "val"
)

internal fun IrField.renderFieldFlags(renderer: FlagsRenderer) = renderer.renderFlagsList(
    declaration = this,
    "final".takeIf { isFinal },
    "external".takeIf { isExternal },
    "static".takeIf { isStatic },
)

internal fun IrVariable.renderVariableFlags(renderer: FlagsRenderer): String = renderer.renderFlagsList(
    declaration = this,
    "const".takeIf { isConst },
    "lateinit".takeIf { isLateinit },
    if (isVar) "var" else "val"
)

internal fun IrValueParameter.renderValueParameterFlags(renderer: FlagsRenderer): String = renderer.renderFlagsList(
    declaration = this,
    "vararg".takeIf { varargElementType != null },
    "crossinline".takeIf { isCrossinline },
    "noinline".takeIf { isNoinline },
    "assignable".takeIf { isAssignable })

internal fun IrConstructor.renderConstructorFlags(renderer: FlagsRenderer) =
    renderer.renderFlagsList(
        declaration = this,
        "inline".takeIf { isInline },
        "external".takeIf { isExternal },
        "primary".takeIf { isPrimary },
        "expect".takeIf { isExpect })

internal fun IrSimpleFunction.renderSimpleFunctionFlags(renderer: FlagsRenderer): String = renderer.renderFlagsList(
    declaration = this,
    "tailrec".takeIf { isTailrec },
    "inline".takeIf { isInline },
    "external".takeIf { isExternal },
    "suspend".takeIf { isSuspend },
    "expect".takeIf { isExpect },
    "fake_override".takeIf { isFakeOverride },
    "operator".takeIf { isOperator },
    "infix".takeIf { isInfix })

internal fun renderField(
    declaration: IrField,
    renderer: ColoredRenderIrElementVisitor?,
    flagsRenderer: FlagsRenderer,
    options: DumpIrTreeOptions
) = declaration.runTrimEnd {
    "FIELD ${renderOriginIfNonTrivial(options)}name:$name ${renderSignatureIfEnabled(options.printSignatures)}type:${
        type.renderTypeWithRenderer(
            renderer, options
        )
    } visibility:$visibility ${renderFieldFlags(flagsRenderer)}"
}

internal fun renderClassWithRenderer(
    declaration: IrClass,
    renderer: ColoredRenderIrElementVisitor?,
    flagsRenderer: FlagsRenderer,
    options: DumpIrTreeOptions,
) = declaration.runTrimEnd {
    "CLASS ${renderOriginIfNonTrivial(options)}" + "$kind name:$name " + renderSignatureIfEnabled(options.printSignatures) + "modality:$modality visibility:$visibility " + renderClassFlags(
        flagsRenderer
    ) + "superTypes:[${superTypes.joinToString(separator = "; ") { it.renderTypeWithRenderer(renderer, options) }}]"
}

internal fun renderTypeParameter(
    declaration: IrTypeParameter, renderer: ColoredRenderIrElementVisitor?, options: DumpIrTreeOptions
) = declaration.runTrimEnd {
    Ansi.ansi()
        .declaration("TYPE_PARAMETER ")
        .a(renderOriginIfNonTrivial(options) + "name:")
        .typeParameter("$name ")
        .a(
            "index:$index variance:$variance " + renderSignatureIfEnabled(
            options.printSignatures
        ) + "superTypes:[${
            superTypes.joinToString(separator = "; ") {
                it.renderTypeWithRenderer(
                    renderer, options
                )
            }
        }] " + "reified:$isReified")
        .toString()
}

internal fun IrTypeParameter.renderTypeParameterFqn(options: DumpIrTreeOptions): String =
    Ansi.ansi().typeParameter(StringBuilder().also { sb ->
        sb.append(name.asString())
        sb.append(" of ")
        renderDeclarationParentFqn(sb, options)
    }.toString()).toString()

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrClassifierSymbol.renderClassifierFqn(options: DumpIrTreeOptions): String =
    if (isBound) when (val owner = owner) {
        is IrClass -> owner.renderClassFqn(options)
        is IrScript -> owner.renderScriptFqn(options)
        is IrTypeParameter -> owner.renderTypeParameterFqn(options)
        else -> "`unexpected classifier: ${owner.render(options)}`"
    }
    else "<unbound ${this.javaClass.simpleName}>"

internal fun IrDeclaration.renderDeclarationParentFqn(sb: StringBuilder, options: DumpIrTreeOptions) {
    try {
        val parent = this.parent
        if (parent is IrDeclaration) {
            parent.renderDeclarationFqn(sb, options)
        }
        else if (parent is IrPackageFragment) {
            sb.append(parent.packageFqName.toString())
        }
    } catch (e: UninitializedPropertyAccessException) {
        sb.append("<uninitialized parent>")
    }
}

internal fun IrDeclaration.renderDeclarationFqn(sb: StringBuilder, options: DumpIrTreeOptions) {
    renderDeclarationParentFqn(sb, options)
    sb.appendDeclarationNameToFqName(this, options) {
        sb.append(this)
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrTypeAliasSymbol.renderTypeAliasFqn(options: DumpIrTreeOptions): String =
    if (isBound) StringBuilder().also { owner.renderDeclarationFqn(it, options) }.toString()
    else "<unbound $this>"

internal fun IrTypeAbbreviation.renderTypeAbbreviation(
    renderer: ColoredRenderIrElementVisitor?, options: DumpIrTreeOptions
): String = buildString {
    append("{ ")
    append(renderTypeAnnotations(annotations, renderer, options))
    append(typeAlias.renderTypeAliasFqn(options))
    if (arguments.isNotEmpty()) {
        append(
            arguments.joinToString(prefix = "<", postfix = ">", separator = ", ") {
                it.renderTypeArgument(renderer, options)
            })
    }
    if (hasQuestionMark) {
        append('?')
    }
    append(" }")
}