package xyz.xenondevs.novadokka

import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext

class HideInternalApiTransformer(
    context: DokkaContext
) : SuppressedByConditionDocumentableFilterTransformer(context) {
    
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val annotations = (d as? WithExtraProperties<*>)
            ?.extra
            ?.allOfType<Annotations>()
            ?.flatMap { it.directAnnotations.values.flatten() }
            ?: emptyList()
        
        return annotations.any { it.dri.packageName == "org.jetbrains.annotations" && it.dri.classNames == "ApiStatus.Internal" }
    }
    
}