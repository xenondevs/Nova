package xyz.xenondevs.nova.initialize

import kotlin.reflect.KClass

/**
 * Marks a class or function to be initialized by the [Initializer].
 * This annotation is specifically for internal use.
 *
 * Can be used on classes to run their static initializers or on (suspending) functions of singleton objects to run them during initialization.
 * If both the containing class and a function are annotated, function initialization additionally awaits class initialization.
 *
 * @param stage The [InternalInitStage] at which the class is initialized or the function is called.
 * @param dispatcher Defines how this task is dispatched.
 * @param runAfter The classes and their initialization functions that must finish before this task.
 * @param runBefore The classes and their initialization functions that must run after this task.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
internal annotation class InternalInit(
    val stage: InternalInitStage,
    val dispatcher: Dispatcher = Dispatcher.SYNC,
    val runAfter: Array<KClass<*>> = [],
    val runBefore: Array<KClass<*>> = []
)

/**
 * Marks a class or function to be initialized at startup.
 *
 * Can be used on classes to run their static initializers or on (suspending) functions of singleton objects to run them during initialization.
 * If both the containing class and a function are annotated, function initialization additionally awaits class initialization.
 *
 * @param stage The [InitStage] at which the class is initialized or the function is called. Defaults to [InitStage.BOOTSTRAP].
 * @param dispatcher Defines how this task is dispatched. Defaults to [Dispatcher.SYNC].
 * @param runAfter The classes and their initialization functions that must finish before this task.
 * @param runBefore The classes and their initialization functions that must run after this task.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Init(
    val stage: InitStage = InitStage.BOOTSTRAP,
    val dispatcher: Dispatcher = Dispatcher.SYNC,
    val runAfter: Array<KClass<*>> = [],
    val runBefore: Array<KClass<*>> = [],
)

/**
 * Marks a function to be called during initialization using its containing class's stage.
 * The containing class must be annotated with [Init] or [InternalInit].
 * An omitted dispatcher inherits the class's dispatcher.
 */
@Deprecated("Use @Init instead.")
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class InitFun(
    val dispatcher: Dispatcher = Dispatcher.SYNC,
    val runAfter: Array<KClass<*>> = [],
    val runBefore: Array<KClass<*>> = []
)

/**
 * Marks a function to be called during disable.
 *
 * @param dispatcher Defines how the function is dispatched.
 * @param runAfter The classes whose disable functions should be called before this function.
 * (This function is called **after** them.)
 * @param runBefore The classes whose disable functions should be called after this function.
 * (This function is called **before** them.)
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class DisableFun(
    val dispatcher: Dispatcher = Dispatcher.SYNC,
    val runAfter: Array<KClass<*>> = [],
    val runBefore: Array<KClass<*>> = []
)