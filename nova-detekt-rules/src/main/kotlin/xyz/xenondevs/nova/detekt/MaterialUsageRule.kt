package xyz.xenondevs.nova.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeReference

private val MATERIAL_CLASS_ID = ClassId(
    packageFqName = FqName("org.bukkit"),
    topLevelName = Name.identifier("Material")
)

class MaterialUsageRule(
    config: Config
) : Rule(config, "Reports usages of org.bukkit.Material"), RequiresAnalysisApi {
    
    override fun visit(root: KtFile) {
        analyze(root) {
            root.accept(object : KtTreeVisitorVoid() {
                override fun visitTypeReference(typeReference: KtTypeReference) {
                    super.visitTypeReference(typeReference)
                    if (typeReference.type.containsMaterial())
                        report(typeReference)
                }
                
                override fun visitExpression(expression: KtExpression) {
                    super.visitExpression(expression)
                    if (expression.expressionType?.containsMaterial() == true)
                        report(expression)
                }
            })
        }
    }
    
    context(_: KaSession)
    private fun KaType.containsMaterial(): Boolean {
        return this is KaClassType && (classId == MATERIAL_CLASS_ID || typeArguments.any { it.type?.containsMaterial() == true })
    }
    
    private fun report(element: KtElement) {
        report(
            Finding(
                Entity.from(element),
                "Do not use org.bukkit.Material. Use ItemType or BlockType instead."
            )
        )
    }
}
