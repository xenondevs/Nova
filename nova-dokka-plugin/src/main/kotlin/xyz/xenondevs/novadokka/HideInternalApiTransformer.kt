package xyz.xenondevs.novadokka

import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext

/**
 * A pre-merge transformer that hides all declarations annotated with `@ApiStatus.Internal`.
 */
class HideInternalApiTransformer(
    context: DokkaContext
) : SuppressedByConditionDocumentableFilterTransformer(context) {
    
    override fun invoke(modules: List<DModule>): List<DModule> {
        val modules = super.invoke(modules)
        return modules.map { module ->
            module.copy(
                packages = module.packages.filter { pkg ->
                    pkg.properties.isNotEmpty()
                        || pkg.functions.isNotEmpty()
                        || pkg.classlikes.isNotEmpty()
                }
            )
        }
    }
    
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val annotations = (d as? WithExtraProperties<*>)
            ?.extra
            ?.allOfType<Annotations>()
            ?.flatMap { it.directAnnotations.values.flatten() }
            ?: emptyList()
        
        return annotations.any { it.dri.packageName == "org.jetbrains.annotations" && it.dri.classNames == "ApiStatus.Internal" }
    }
    
}