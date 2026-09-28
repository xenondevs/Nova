package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenPropertiesSafe
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private val TYPED_KEY_CLASS_ID = ClassId.topLevel(FqName("io.papermc.paper.registry.TypedKey"))

internal object TypedKeyConversionChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        if (expression !is FirExpression || expression is FirBlock || expression is FirWrappedArgumentExpression)
            return
        if (!expression.resolvedType.isClassOrSubclassOf(TYPED_KEY_CLASS_ID))
            return
        
        val expectation = expression.findTypeExpectation() ?: return
        if (expectation.type.isKeyWithoutTypedKey()
            && expectation.parentExpressions.none { it.resolvedType.isClassOrSubclassOf(TYPED_KEY_CLASS_ID) }) {
            reporter.reportOn(expression.source, NovaDiagnostics.TYPED_KEY_AS_KEY)
        }
    }
}

internal object TypedKeyMemberAccessChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() ?: return
        if (symbol.callableId?.classId != KEY_CLASS_ID || symbol.name.asString() == "key")
            return
        val receiver = expression.explicitReceiver ?: expression.dispatchReceiver ?: expression.extensionReceiver
        if (receiver?.resolvedType?.isClassOrSubclassOf(TYPED_KEY_CLASS_ID) == true)
            reporter.reportOn(receiver.source, NovaDiagnostics.TYPED_KEY_AS_KEY)
    }
}

internal object TypedKeyPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (declaration.returnTypeRef.coneType.isClassOrSubclassOf(TYPED_KEY_CLASS_ID)
            && declaration.symbol.directOverriddenPropertiesSafe(context).any { it.resolvedReturnType.isKeyWithoutTypedKey() }) {
            reporter.reportOn(declaration.returnTypeRef.source ?: declaration.source, NovaDiagnostics.TYPED_KEY_AS_KEY)
        }
    }
}

context(context: CheckerContext)
private fun ConeKotlinType.isKeyWithoutTypedKey(): Boolean =
    isClassOrSubclassOf(KEY_CLASS_ID) && !isClassOrSubclassOf(TYPED_KEY_CLASS_ID)
