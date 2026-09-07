package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

internal class NovaCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    
    override val declarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers = setOf(TypedKeyPropertyChecker)
    }
    
    override val expressionCheckers = object : ExpressionCheckers() {
        override val basicExpressionCheckers = setOf(TypedKeyConversionChecker)
        override val equalityOperatorCallCheckers = setOf(RegistryEntryEqualityChecker)
        override val functionCallCheckers = setOf(KeyToStringCallChecker, RegistryEntryEqualsCallChecker)
        override val getClassCallCheckers = setOf(MaterialClassLiteralChecker)
        override val qualifiedAccessExpressionCheckers = setOf(MaterialExpressionChecker, TypedKeyMemberAccessChecker)
        override val stringConcatenationCallCheckers = setOf(KeyStringInterpolationChecker)
    }
    
    override val typeCheckers = object : TypeCheckers() {
        override val resolvedTypeRefCheckers = setOf(MaterialTypeChecker)
    }
}
