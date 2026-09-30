package xyz.xenondevs.nova.annotations.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.setProperty
import javax.inject.Inject

/**
 * Configures the annotations recorded in nova-annotations.json.
 */
abstract class AnnotationIndexExtension @Inject constructor(
    objects: ObjectFactory
) {
    
    /**
     * Class directories to scan. Defaults to the main source set's compiled classes.
     */
    abstract val classes: ConfigurableFileCollection
    
    /**
     * The annotations to look for on classes.
     */
    val classAnnotations: SetProperty<String> = objects
        .setProperty<String>()
        .convention(emptySet())
    
    /**
     * The annotations to look for on methods
     */
    val methodAnnotations: SetProperty<String> = objects
        .setProperty<String>()
        .convention(emptySet())
    
}
