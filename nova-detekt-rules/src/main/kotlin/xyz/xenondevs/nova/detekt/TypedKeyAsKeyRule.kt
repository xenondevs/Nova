package xyz.xenondevs.nova.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.allOverriddenSymbols
import org.jetbrains.kotlin.analysis.api.components.isSubtypeOf
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

@OptIn(KaContextParameterApi::class)
class TypedKeyAsKeyRule(
    config: Config
) : Rule(config, "Reports usages of TypedKey that rely on it implementing Key"), RequiresAnalysisApi {
    
    private val keyClassId = ClassId(
        packageFqName = FqName("net.kyori.adventure.key"),
        topLevelName = Name.identifier("Key")
    )
    
    private val typedKeyClassId = ClassId(
        packageFqName = FqName("io.papermc.paper.registry"),
        topLevelName = Name.identifier("TypedKey")
    )
    
    override fun visit(root: KtFile) {
        analyze(root) {
            val reported = HashSet<KtElement>()
            
            fun report(element: KtElement) {
                if (!reported.add(element))
                    return
                this@TypedKeyAsKeyRule.report(
                    Finding(
                        Entity.from(element),
                        "Do not use TypedKey as Key. Access its key() instead."
                    )
                )
            }
            
            fun checkOverride(declaration: KtCallableDeclaration) {
                if (!declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD))
                    return
                
                val symbol = declaration.symbol as? KaCallableSymbol
                    ?: return
                if (!symbol.returnType.isTypedKey())
                    return
                if (symbol.allOverriddenSymbols.any { it.returnType.isKeyWithoutTypedKey() })
                    report(declaration.typeReference ?: declaration)
            }
            
            root.accept(object : KtTreeVisitorVoid() {
                
                override fun visitExpression(expression: KtExpression) {
                    super.visitExpression(expression)
                    
                    val parent = expression.parent as? KtQualifiedExpression
                    if (parent?.selectorExpression === expression)
                        return
                    
                    val actualType = expression.expressionType
                        ?: return
                    val expectedType = expression.expectedType
                        ?: return
                    val parentExpression = expression.parent as? KtExpression
                    if (parentExpression?.expressionType?.isTypedKey() == true &&
                        parentExpression.expectedType?.isKeyWithoutTypedKey() == true) {
                        return
                    }
                    if (actualType.isTypedKey() && expectedType.isKeyWithoutTypedKey())
                        report(expression)
                }
                
                override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
                    super.visitQualifiedExpression(expression)
                    
                    if (expression.receiverExpression.expressionType?.isTypedKey() != true)
                        return
                    val call = expression.selectorExpression
                        ?.resolveToCall()
                        ?.successfulCallOrNull<KaCallableMemberCall<*, *>>()
                        ?: return
                    if (call.symbol.isKeyMember())
                        report(expression.receiverExpression)
                }
                
                override fun visitProperty(property: KtProperty) {
                    super.visitProperty(property)
                    checkOverride(property)
                }
                
                override fun visitParameter(parameter: KtParameter) {
                    super.visitParameter(parameter)
                    if (parameter.hasValOrVar())
                        checkOverride(parameter)
                }
                
            })
        }
    }
    
    context(_: KaSession)
    private fun KaType.isTypedKey(): Boolean =
        isSubtypeOf(typedKeyClassId)
    
    context(_: KaSession)
    private fun KaType.isKeyWithoutTypedKey(): Boolean =
        isSubtypeOf(keyClassId) && !isTypedKey()
    
    context(_: KaSession)
    private fun KaCallableSymbol.isKeyMember(): Boolean =
        callableId?.classId != typedKeyClassId &&
            (callableId?.classId == keyClassId ||
                allOverriddenSymbols.any { it.callableId?.classId == keyClassId })
    
}
