package xyz.xenondevs.nova.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression

private val KYORI_KEY_CLASS_ID = ClassId(
    packageFqName = FqName("net.kyori.adventure.key"),
    topLevelName = Name.identifier("Key")
)

@OptIn(KaContextParameterApi::class)
class KeyToStringRule(
    config: Config
) : Rule(config, "Reports calls to Key.toString()"), RequiresAnalysisApi {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text != "toString" || expression.valueArguments.isNotEmpty())
            return

        val qualifiedExpression = expression.parent as? KtQualifiedExpression
            ?: return
        if (qualifiedExpression.selectorExpression !== expression)
            return

        val isKyoriKey = analyze(expression) {
            qualifiedExpression.receiverExpression.expressionType?.isSubtypeOf(KYORI_KEY_CLASS_ID) == true
        }
        if (!isKyoriKey)
            return

        report(
            Finding(
                Entity.from(expression),
                "Some Key implementations (like TypedKey) do not return the expected format (`namespace:name`) on Key.toString(). Use Key.asString() instead."
            )
        )
    }

}
