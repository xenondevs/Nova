package xyz.xenondevs.nova.compiler

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isNothingOrNullableNothing
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.AbstractTypeChecker

internal val KEY_CLASS_ID = ClassId.topLevel(FqName("net.kyori.adventure.key.Key"))

/**
 * Checks membership in an API class independently of nullability and type arguments.
 * Bottom types do not represent instances of any API class.
 */
context(context: CheckerContext)
internal fun ConeKotlinType.isClassOrSubclassOf(classId: ClassId): Boolean {
    if (isNothingOrNullableNothing)
        return false
    
    val target = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
        ?.defaultType()
        ?.replaceArgumentsWithStarProjections()
        ?.withNullability(true, context.session.typeContext)
        ?: return false
    return AbstractTypeChecker.isSubtypeOf(context.session.typeContext, this, target)
}

context(context: CheckerContext)
internal fun ConeKotlinType.findSupertype(classId: ClassId): ConeClassLikeType? {
    val target = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.defaultType() ?: return null
    val state = context.session.typeContext.newTypeCheckerState(
        errorTypesEqualToAnything = false,
        stubTypesEqualToAnything = false
    )
    return AbstractTypeChecker.findCorrespondingSupertypes(
        state,
        lowerBoundIfFlexible(),
        with(context.session.typeContext) { target.typeConstructor() }
    ).firstOrNull() as? ConeClassLikeType
}
