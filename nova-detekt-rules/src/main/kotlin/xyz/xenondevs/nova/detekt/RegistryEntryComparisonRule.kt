package xyz.xenondevs.nova.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.allSupertypes
import org.jetbrains.kotlin.analysis.api.components.semanticallyEquals
import org.jetbrains.kotlin.analysis.api.components.withNullability
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

private val REGISTRY_ENTRY_CLASS_ID = ClassId(
    packageFqName = FqName("xyz.xenondevs.nova.registry"),
    topLevelName = Name.identifier("RegistryEntry")
)

@OptIn(KaContextParameterApi::class)
class RegistryEntryComparisonRule(
    config: Config
) : Rule(config, "Reports comparisons between RegistryEntry<T> and T"), RequiresAnalysisApi {
    
    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)
        
        val op = expression.operationToken
        if (op != KtTokens.EQEQ && op != KtTokens.EXCLEQ)
            return
        
        analyze(expression) {
            val leftType = expression.left?.expressionType
                ?: return
            val rightType = expression.right?.expressionType
                ?: return
            
            if (isRegistryEntryVsValue(leftType, rightType)
                || isRegistryEntryVsValue(rightType, leftType)) {
                report(
                    Finding(
                        Entity.from(expression),
                        "Do not compare RegistryEntry<T> with T. Compare entry-to-entry or value-to-value."
                    )
                )
            }
        }
    }
    
    override fun visitWhenExpression(expression: KtWhenExpression) {
        super.visitWhenExpression(expression)
        
        val subjectExpression = expression.subjectExpression
            ?: return
        
        analyze(expression) {
            val subjectType = subjectExpression.expressionType
                ?: return
            
            for (entry in expression.entries) {
                for (condition in entry.conditions) {
                    if (condition !is KtWhenConditionWithExpression)
                        continue
                    val conditionType = condition.expression?.expressionType
                        ?: continue
                    
                    if (isRegistryEntryVsValue(subjectType, conditionType)
                        || isRegistryEntryVsValue(conditionType, subjectType)) {
                        report(
                            Finding(
                                Entity.from(condition),
                                "Do not compare RegistryEntry<T> with T. Compare entry-to-entry or value-to-value."
                            )
                        )
                    }
                }
            }
        }
    }
    
    context(_: KaSession)
    private fun isRegistryEntryVsValue(
        maybeEntry: KaType,
        maybeValue: KaType
    ): Boolean {
        val entry = maybeEntry.withNullability(false)
        val value = maybeValue.withNullability(false)
        val registryEntryType = entry.findRegistryEntryType()
            ?: return false
        val typeArg = registryEntryType.typeArguments.firstOrNull()?.type
            ?: return false
        return value.semanticallyEquals(typeArg)
    }
    
    context(_: KaSession)
    private fun KaType.findRegistryEntryType(): KaClassType? {
        if (this is KaClassType && classId == REGISTRY_ENTRY_CLASS_ID)
            return this
        return allSupertypes.filterIsInstance<KaClassType>()
            .firstOrNull { it.classId == REGISTRY_ENTRY_CLASS_ID }
    }
    
}