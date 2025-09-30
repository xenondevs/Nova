package xyz.xenondevs.novadokka

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

/**
 * A pre-merge transformer that hides all `@JvmStatic`, `@JvmName` and `@JvmField` annotations from the documentation.
 */
class HideJvmAnnotationsTransformer(ctx: DokkaContext) : PreMergeDocumentableTransformer {
    
    override fun invoke(modules: List<DModule>): List<DModule> =
        modules.map(::processModule)
    
    private fun processModule(module: DModule): DModule = module.copy(packages = module.packages.map(::processPackage))
    
    private fun processPackage(pkg: DPackage): DPackage = pkg.copy(
        functions = pkg.functions.map(::processFunction),
        properties = pkg.properties.map(::processProperty),
        classlikes = pkg.classlikes.map(::processClasslike),
    )
    
    private fun processClasslike(classlike: DClasslike): DClasslike = when (classlike) {
        is DAnnotation -> processAnnotation(classlike)
        is DClass -> processClass(classlike)
        is DEnum -> processEnum(classlike)
        is DInterface -> processInterface(classlike)
        is DObject -> processObject(classlike)
    }
    
    private fun processClass(clazz: DClass): DClass = clazz.copy(
        constructors = clazz.constructors.map(::processFunction),
        functions = clazz.functions.map(::processFunction),
        properties = clazz.properties.map(::processProperty),
        classlikes = clazz.classlikes.map(::processClasslike),
        companion = clazz.companion?.let(::processObject),
    )
    
    private fun processInterface(itf: DInterface): DInterface = itf.copy(
        functions = itf.functions.map(::processFunction),
        properties = itf.properties.map(::processProperty),
        classlikes = itf.classlikes.map(::processClasslike),
        companion = itf.companion?.let(::processObject),
    )
    
    private fun processEnum(enum: DEnum): DEnum = enum.copy(
        constructors = enum.constructors.map(::processFunction),
        functions = enum.functions.map(::processFunction),
        properties = enum.properties.map(::processProperty),
        classlikes = enum.classlikes.map(::processClasslike),
        companion = enum.companion?.let(::processObject),
        entries = enum.entries.map(::processEnumEntry),
    )
    
    private fun processEnumEntry(entry: DEnumEntry): DEnumEntry = entry.copy(
        functions = entry.functions.map(::processFunction),
        properties = entry.properties.map(::processProperty),
        classlikes = entry.classlikes.map(::processClasslike),
    )
    
    private fun processAnnotation(annotation: DAnnotation): DAnnotation = annotation.copy(
        constructors = annotation.constructors.map(::processFunction),
        functions = annotation.functions.map(::processFunction),
        properties = annotation.properties.map(::processProperty),
        classlikes = annotation.classlikes.map(::processClasslike),
        companion = annotation.companion?.let(::processObject),
    )
    
    private fun processObject(obj: DObject): DObject = obj.copy(
        functions = obj.functions.map(::processFunction),
        properties = obj.properties.map(::processProperty),
        classlikes = obj.classlikes.map(::processClasslike),
    )
    
    private fun processFunction(function: DFunction): DFunction {
        val annotations = function.extra[Annotations]
        if (annotations == null)
            return function
        
        return function.copy(extra = function.extra.addAll(listOf(filterAnnotations(annotations))))
    }
    
    private fun processProperty(property: DProperty): DProperty {
        val annotations = property.extra[Annotations]
        if (annotations == null)
            return property
        
        return property.copy(extra = property.extra.addAll(listOf(filterAnnotations(annotations))))
    }
    
    @Suppress("DEPRECATION")
    private fun filterAnnotations(annotations: Annotations): Annotations = annotations.copy(
        myContent = annotations.content.mapValues { (_, annotations) ->
            annotations.filter(::isAnnotationAllowed)
        }
    )
    
    private fun isAnnotationAllowed(annotation: Annotations.Annotation): Boolean {
        return annotation.dri.classNames != "JvmStatic"
            && annotation.dri.classNames != "JvmName"
            && annotation.dri.classNames != "JvmField"
    }
    
}