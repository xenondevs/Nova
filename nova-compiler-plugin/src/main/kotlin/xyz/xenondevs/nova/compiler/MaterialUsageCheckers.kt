package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirGetClassCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private val MATERIAL_CLASS_ID = ClassId.topLevel(FqName("org.bukkit.Material"))

internal object MaterialExpressionChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (expression.resolvedType.containsMaterial())
            reporter.reportOn(expression.source, NovaDiagnostics.MATERIAL_USAGE)
    }
}

internal object MaterialClassLiteralChecker : FirGetClassCallChecker(MppCheckerKind.Common) {
    
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirGetClassCall) {
        if (expression.resolvedType.containsMaterial())
            reporter.reportOn(expression.source, NovaDiagnostics.MATERIAL_USAGE)
    }
}

internal object MaterialTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        // Inferred types are covered by expressions. Also inspect expanded type arguments for type aliases.
        if (typeRef.source?.kind !is KtFakeSourceElementKind && typeRef.coneType.containsMaterial())
            reporter.reportOn(typeRef.source, NovaDiagnostics.MATERIAL_USAGE)
    }
}

private fun ConeKotlinType.containsMaterial(): Boolean {
    val type = lowerBoundIfFlexible()
    return type.classId == MATERIAL_CLASS_ID || type.typeArguments.any { it.type?.containsMaterial() == true }
}
