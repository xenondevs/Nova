package xyz.xenondevs.nova.ksp.annotation

/**
 * Instructs the nova-ksp-processor to generate flat-mapping extension functions and properties
 * for properties and functions of the annotated class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateFlatMapExtensions