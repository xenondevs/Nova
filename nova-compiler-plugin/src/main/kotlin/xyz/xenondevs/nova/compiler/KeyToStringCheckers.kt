package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirStringConcatenationCallChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private val NAMESPACED_KEY_CLASS_ID = ClassId.topLevel(FqName("org.bukkit.NamespacedKey"))

internal object KeyToStringCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() ?: return
        if (symbol.name.asString() != "toString" || expression.argumentList.arguments.isNotEmpty())
            return
        
        val receiver = expression.explicitReceiver ?: expression.dispatchReceiver ?: expression.extensionReceiver
        if (receiver?.resolvedType?.needsExplicitKeyStringConversion() == true)
            reporter.reportOn(expression.calleeReference.source, NovaDiagnostics.KEY_TO_STRING)
    }
}

internal object KeyStringInterpolationChecker : FirStringConcatenationCallChecker(MppCheckerKind.Common) {
    
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStringConcatenationCall) {
        for (argument in expression.argumentList.arguments) {
            if (argument.resolvedType.needsExplicitKeyStringConversion())
                reporter.reportOn(argument.source, NovaDiagnostics.KEY_TO_STRING)
        }
    }
}

context(context: CheckerContext)
private fun ConeKotlinType.needsExplicitKeyStringConversion(): Boolean =
    isClassOrSubclassOf(KEY_CLASS_ID) && !isClassOrSubclassOf(NAMESPACED_KEY_CLASS_ID)
