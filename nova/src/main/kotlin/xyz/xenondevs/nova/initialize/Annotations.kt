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
 * @param dispatcher Defines how the initialization is dispatched.
 * @param dependsOn The classes (and init functions) which should be initialized before this class.
 * Those classes must also be annotated with [InternalInit].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
internal annotation class InternalInit(
    val stage: InternalInitStage,
    val dispatcher: Dispatcher = Dispatcher.SERVER,
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
 * @param dispatcher Defines how the initialization is dispatched.
 * @param runAfter The classes (and init functions) which should be initialized before this class.
 * (This class is initialized **after** them.)
 * Those classes must be annotated with [Init] or [InternalInit].
 * @param runBefore The classes (and init functions) which should be initialized after this class.
 * (This class is initialized **before** them.)
 * Those classes must be annotated with [Init] or [InternalInit].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Init(
    val stage: InitStage,
    val dispatcher: Dispatcher = Dispatcher.SERVER,
    val runAfter: Array<KClass<*>> = [],
    val runBefore: Array<KClass<*>> = [],
)

/**
 * Marks a function to be called during initialization.
 * This requires the class to be annotated with [Init] or [InternalInit],
 * and automatically has a dependency on the initialization of the class.
 *
 * @param dispatcher Defines how the initialization is dispatched.
 * Falls back to the dispatcher of the class if not specified.
 * @param runAfter The classes (and their init functions) which should be initialized before this function is called.
 * (This function is called **after** them.)
 * Those classes must be annotated with [Init] or [InternalInit].
 * @param runAfter The classes (and their init functions) which should be initialized after this function is called.
 * (This function is called **before** them.)
 * Those classes must be annotated with [Init] or [InternalInit].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class InitFun(
    val dispatcher: Dispatcher = Dispatcher.SERVER,
    val runAfter: Array<KClass<*>> = [],
    val runBefore: Array<KClass<*>> = []
)

/**
 * Marks a function to be called during disable.
 *
 * @param dispatcher Defines how the function is dispatched.
 * @param runAfter The classes whose disable functions should be called before this function.
 * (This function is called **after** them.)
 * @param runAfter The classes whose disable functions should be called after this function.
 * (This function is called **before** them.)
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class DisableFun(
    val dispatcher: Dispatcher = Dispatcher.SERVER,
    val runAfter: Array<KClass<*>> = [],
    val runBefore: Array<KClass<*>> = []
)