package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.delegateFieldsMap
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.createConeSubstitutorFromTypeArguments
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.varargElementType
import org.jetbrains.kotlin.name.ClassId

/**
 * An expression's contextual type and the enclosing value expressions that share that expectation.
 * Checkers can use [parentExpressions] to avoid reporting both a composite expression and its results.
 */
internal class FirTypeExpectation(
    val type: ConeKotlinType,
    val parentExpressions: List<FirExpression>
)

/**
 * Finds the nearest consumer of this value using the compiler's current ancestor stack.
 * Only result positions forward an expectation: receivers, conditions, and discarded statements do not.
 * No subtree traversal or additional type inference is performed.
 */
context(context: CheckerContext)
internal fun FirExpression.findTypeExpectation(): FirTypeExpectation? {
    var value: FirElement = this
    var isVarargElement = false
    val parentExpressions = mutableListOf<FirExpression>()
    val ancestors = context.containingElements
    for (index in ancestors.lastIndex - 1 downTo 0) {
        val parent = ancestors[index]
        when (parent) {
            is FirArgumentList -> continue
            is FirBlock -> if (parent.statements.lastOrNull() !== value) return null
            is FirWhenBranch -> if (parent.result !== value) return null
            is FirCatch -> if (parent.block !== value) return null
            is FirWhenExpression -> {
                if (value !is FirWhenBranch || !parent.usedAsExpression) return null
                parentExpressions += parent
            }
            
            is FirTryExpression -> {
                if (parent.tryBlock !== value && value !is FirCatch) return null
                parentExpressions += parent
            }
            
            is FirElvisExpression -> {
                if (parent.lhs !== value && parent.rhs !== value) return null
                parentExpressions += parent
            }
            
            is FirSafeCallExpression -> {
                if (parent.selector !== value) return null
                parentExpressions += parent
            }
            
            is FirSmartCastExpression -> {
                parentExpressions += parent
            }
            
            is FirWrappedArgumentExpression -> {
                if (parent.isSpread) return null
            }
            
            is FirVarargArgumentsExpression -> isVarargElement = true
            else -> {
                val expectedType = when (parent) {
                    is FirValueParameter -> parent.returnTypeRef.coneType.takeIf { parent.defaultValue === value }
                    is FirVariable -> parent.initializerExpectedType().takeIf { parent.initializer === value }
                    is FirReturnExpression -> parent.target.labeledElement.returnTypeRef.coneType.takeIf { parent.result === value }
                    is FirVariableAssignment -> parent.lValue.resolvedType.takeIf { parent.rValue === value }
                    is FirTypeOperatorCall -> parent.conversionTypeRef.coneType.takeIf {
                        parent.operation == FirOperation.AS || parent.operation == FirOperation.SAFE_AS
                    }
                    
                    is FirEqualityOperatorCall -> parent.whenBranchExpectedType(value)
                    is FirCall -> parent.argumentExpectedType(value, isVarargElement)
                    else -> null
                }
                return expectedType?.let { FirTypeExpectation(it, parentExpressions) }
            }
        }
        value = parent
    }
    return null
}

context(context: CheckerContext)
private fun FirVariable.initializerExpectedType(): ConeKotlinType {
    if (this is FirField) {
        // Interface delegate fields may store the concrete delegate type instead of the implemented interface.
        val owner = context.containingElements.lastOrNull { it is FirClass } as? FirClass
        val supertypeIndex = owner?.delegateFieldsMap?.entries?.firstOrNull { it.value == symbol }?.key
        if (supertypeIndex != null)
            return owner.superTypeRefs[supertypeIndex].coneType
    }
    return returnTypeRef.coneType
}

private fun FirEqualityOperatorCall.whenBranchExpectedType(value: FirElement): ConeKotlinType? {
    val arguments = argumentList.arguments
    if (arguments.size != 2 || arguments[1] !== value)
        return null
    val subject = arguments[0]
    if (subject.unwrapSmartcastExpression() !is FirWhenSubjectExpression)
        return null
    return subject.resolvedType
}

context(context: CheckerContext)
private fun FirCall.argumentExpectedType(value: FirElement, isVarargElement: Boolean): ConeKotlinType? {
    val parameter = resolvedArgumentMapping?.get(value) ?: return null
    val symbol = (this as? FirResolvable)?.calleeReference?.toResolvedFunctionSymbol() ?: return null
    val parameterType = parameter.returnTypeRef.coneType
    val expectedType = when (this) {
        is FirQualifiedAccessExpression -> {
            val receiverSubstitutor = dispatchReceiver?.resolvedType.classSubstitutor(symbol.callableId.classId)
            createConeSubstitutorFromTypeArguments(symbol, context.session)
                .substituteOrSelf(receiverSubstitutor.substituteOrSelf(parameterType))
        }
        
        is FirDelegatedConstructorCall ->
            constructedTypeRef.coneType.classSubstitutor(symbol.callableId.classId).substituteOrSelf(parameterType)
        
        else -> parameterType
    }
    return if (isVarargElement) expectedType.varargElementType() else expectedType
}

context(context: CheckerContext)
private fun ConeKotlinType?.classSubstitutor(classId: ClassId?): ConeSubstitutor {
    if (this == null || classId == null)
        return ConeSubstitutor.Empty
    val classSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
        ?: return ConeSubstitutor.Empty
    if (classSymbol.typeParameterSymbols.isEmpty())
        return ConeSubstitutor.Empty
    val supertype = findSupertype(classId) ?: return ConeSubstitutor.Empty
    val substitution = classSymbol.typeParameterSymbols.zip(supertype.typeArguments).mapNotNull { [parameter, argument] ->
        argument.type?.let { parameter to it }
    }.toMap()
    return substitutorByMap(substitution, context.session)
}
