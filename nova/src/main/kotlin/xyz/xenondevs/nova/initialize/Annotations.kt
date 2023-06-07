package xyz.xenondevs.nova.initialize

import kotlin.reflect.KClass

/**
 * Marks a class to be initialized by the [Initializer].
 * This annotation is specifically for internal use.
 * 
 * Classes marked with this annotation will be loaded during initialization.
 * If there are methods annotated with the [InitFun] annotation in the class, they will be called.
 * On disable, the methods annotated with the [DisableFun] annotation will be called.
 * 
 * @param stage The [InternalInitStage] at which the class should be initialized.
 * @param dependsOn The classes which should be initialized before this class. Those classes must also be annotated with [InternalInit].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
internal annotation class InternalInit(
    val stage: InternalInitStage,
    val dependsOn: Array<KClass<*>> = []
)

/**
 * Marks an addon class to be initialized at startup.
 * 
 * Classes marked with this annotation will be loaded during initialization.
 * If there are methods annotated with the [InitFun] annotation in the class, they will be called.
 * On disable, the methods annotated with the [DisableFun] annotation will be called.
 * 
 * @param stage The [InternalInitStage] at which the class should be initialized.
 * @param runAfter The classes which should be initialized before this class. (This class is initialized **after** them.)
 * Those classes must be annotated with [Init] or [InternalInit].
 * @param runBefore The classes which should be initialized after this class. (This class is initialized **before** them.)
 * Those classes must be annotated with [Init] or [InternalInit].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Init(
    val stage: InitStage,
    val runAfter: Array<KClass<*>> = [],
    val runBefore: Array<KClass<*>> = [],
)

/**
 * Marks a function to be called during initialization.
 * This requires the class to be annotated with [Init] or [InternalInit].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class InitFun

/**
 * Marks a function to be called during disable.
 * This requires the class to be annotated with [Init] or [InternalInit].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class DisableFun