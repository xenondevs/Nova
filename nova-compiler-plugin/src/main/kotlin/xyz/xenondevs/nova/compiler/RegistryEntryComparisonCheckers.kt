package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirEqualityOperatorCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.AbstractTypeChecker

private val REGISTRY_ENTRY_CLASS_ID = ClassId.topLevel(FqName("xyz.xenondevs.nova.registry.RegistryEntry"))

internal object RegistryEntryEqualityChecker : FirEqualityOperatorCallChecker(MppCheckerKind.Common) {
    
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirEqualityOperatorCall) {
        if (expression.operation != FirOperation.EQ && expression.operation != FirOperation.NOT_EQ)
            return
        
        val arguments = expression.argumentList.arguments
        if (arguments.size == 2 && isRegistryEntryValueComparison(arguments[0].resolvedType, arguments[1].resolvedType))
            reporter.reportOn(expression.source, NovaDiagnostics.REGISTRY_ENTRY_COMPARISON)
    }
}

internal object RegistryEntryEqualsCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() ?: return
        if (symbol.name.asString() != "equals")
            return
        
        val argument = expression.argumentList.arguments.singleOrNull() ?: return
        val receiver = expression.explicitReceiver ?: expression.dispatchReceiver ?: expression.extensionReceiver ?: return
        if (isRegistryEntryValueComparison(receiver.resolvedType, argument.resolvedType))
            reporter.reportOn(expression.source, NovaDiagnostics.REGISTRY_ENTRY_COMPARISON)
    }
}

context(context: CheckerContext)
private fun isRegistryEntryValueComparison(left: ConeKotlinType, right: ConeKotlinType): Boolean =
    isRegistryEntryValuePair(left, right) || isRegistryEntryValuePair(right, left)

context(context: CheckerContext)
private fun isRegistryEntryValuePair(entry: ConeKotlinType, value: ConeKotlinType): Boolean {
    val entryValueType = entry.findSupertype(REGISTRY_ENTRY_CLASS_ID)?.typeArguments?.firstOrNull()?.type ?: return false
    return AbstractTypeChecker.equalTypes(
        context.session.typeContext,
        entryValueType.withNullability(false, context.session.typeContext),
        value.withNullability(false, context.session.typeContext)
    )
}
